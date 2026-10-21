package com.shohankhan.hisaabos.domain.ledger

import androidx.room.withTransaction
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.AuditLogEntity
import com.shohankhan.hisaabos.data.db.HisaabDatabase
import com.shohankhan.hisaabos.data.db.LedgerPostingEntity
import com.shohankhan.hisaabos.data.db.LinkPaidRow
import com.shohankhan.hisaabos.data.db.TransactionEntity
import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.domain.model.CategoryKind
import com.shohankhan.hisaabos.domain.model.LedgerMath
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.PartyKind
import com.shohankhan.hisaabos.domain.model.TxType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Aggregated view of what has been paid against one linked finance record. */
data class Settlement(
    val principalMinor: Long = 0,
    val interestMinor: Long = 0,
    val feeMinor: Long = 0,
    val amountMinor: Long = 0,
)

data class IntegrityIssue(val transactionId: Long, val message: String)

/**
 * The only component allowed to write ledger rows.
 *
 * Every write happens inside one Room transaction: the transaction row, its
 * ledger postings, linked finance records and the audit trail are committed
 * atomically or not at all.
 */
class TransactionService(
    private val db: HisaabDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val txDao = db.transactionDao()
    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val auditDao = db.auditDao()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ------------------------------------------------------------------ post

    suspend fun post(draft: TxDraft): WriteResult = io {
        val categoryKind = draft.categoryId?.let { categoryDao.byId(it)?.kind }
        val error = TxValidator.validate(draft, categoryKind)
        if (error != null) return@io WriteResult.Invalid(error)
        if (draft.accountId != null && accountDao.byId(draft.accountId) == null) {
            return@io WriteResult.Invalid("নির্বাচিত হিসাবটি খুঁজে পাওয়া যায়নি")
        }
        val id = db.withTransaction { postLocked(draft) }
        refreshLinked(draft.linkKind, draft.linkId)
        WriteResult.Success(id)
    }

    /** Posts inside an existing DB transaction; callers must already hold one. */
    private suspend fun postLocked(draft: TxDraft): Long {
        val duplicateWindow = txDao.countSimilar(
            type = draft.type,
            amountMinor = draft.amountMinor,
            businessDate = draft.businessDate,
            accountId = draft.accountId,
        )
        val entity = TransactionEntity(
            type = draft.type,
            amountMinor = draft.amountMinor,
            flowSign = draft.flowSign,
            accountId = draft.accountId,
            counterAccountId = draft.counterAccountId,
            interestMinor = draft.interestMinor,
            feeMinor = draft.feeMinor,
            principalMinor = if (draft.type.isLiabilitySettlement) {
                (draft.amountMinor - draft.interestMinor - draft.feeMinor).coerceAtLeast(0)
            } else {
                0L
            },
            occurredAt = draft.occurredAt,
            businessDate = draft.businessDate,
            categoryId = draft.categoryId,
            shopId = draft.shopId,
            personId = draft.personId,
            partyKind = draft.partyKind,
            partyName = draft.partyName,
            linkKind = draft.linkKind,
            linkId = draft.linkId,
            installmentIndex = draft.installmentIndex,
            title = draft.title,
            note = draft.note,
            tags = draft.tags,
            attachmentUri = draft.attachmentUri,
            reversesTransactionId = draft.reversesTransactionId,
            groupId = draft.groupId,
            createdAt = now(),
            updatedAt = now(),
        )
        val id = txDao.insert(entity)
        writePostings(id, entity)
        audit("POST", entity.copy(id = id), id)
        if (duplicateWindow > 0) {
            audit("DUPLICATE_SUSPECTED", entity.copy(id = id), id)
        }
        return id
    }

    private suspend fun writePostings(transactionId: Long, entry: TransactionEntity) {
        val postings = LedgerMath.deltas(entry).map { delta ->
            LedgerPostingEntity(
                transactionId = transactionId,
                accountId = delta.accountId,
                deltaMinor = delta.amountMinor,
                businessDate = entry.businessDate,
                occurredAt = entry.occurredAt,
            )
        }
        if (postings.isNotEmpty()) txDao.insertPostings(postings)
    }

    suspend fun update(id: Long, draft: TxDraft): WriteResult = io {
        val existing = txDao.byId(id) ?: return@io WriteResult.Invalid("লেনদেনটি খুঁজে পাওয়া যায়নি")
        if (existing.isDeleted) return@io WriteResult.Invalid("মুছে ফেলা লেনদেন সম্পাদনা করা যাবে না")
        val categoryKind = draft.categoryId?.let { categoryDao.byId(it)?.kind }
        val error = TxValidator.validate(draft, categoryKind)
        if (error != null) return@io WriteResult.Invalid(error)

        db.withTransaction {
            audit("UPDATE", existing, id)
            val updated = existing.copy(
                amountMinor = draft.amountMinor,
                flowSign = draft.flowSign,
                accountId = draft.accountId,
                counterAccountId = draft.counterAccountId,
                interestMinor = draft.interestMinor,
                feeMinor = draft.feeMinor,
                principalMinor = if (draft.type.isLiabilitySettlement) {
                    (draft.amountMinor - draft.interestMinor - draft.feeMinor).coerceAtLeast(0)
                } else {
                    existing.principalMinor
                },
                occurredAt = draft.occurredAt,
                businessDate = draft.businessDate,
                categoryId = draft.categoryId,
                shopId = draft.shopId,
                personId = draft.personId,
                partyKind = draft.partyKind,
                partyName = draft.partyName,
                linkKind = draft.linkKind,
                linkId = draft.linkId,
                installmentIndex = draft.installmentIndex,
                title = draft.title,
                note = draft.note,
                tags = draft.tags,
                attachmentUri = draft.attachmentUri,
                updatedAt = now(),
            )
            txDao.update(updated)
            txDao.deletePostings(id)
            writePostings(id, updated)
        }
        refreshLinked(existing.linkKind, existing.linkId)
        if (existing.linkKind != draft.linkKind || existing.linkId != draft.linkId) {
            refreshLinked(draft.linkKind, draft.linkId)
        }
        WriteResult.Success(id)
    }

    /** Soft-deletes so the audit trail and any linked item dates stay explainable. */
    suspend fun delete(id: Long): WriteResult = io {
        val existing = txDao.byId(id) ?: return@io WriteResult.Invalid("লেনদেনটি খুঁজে পাওয়া যায়নি")
        if (existing.isDeleted) return@io WriteResult.Success(id)
        db.withTransaction {
            audit("DELETE", existing, id)
            txDao.update(existing.copy(isDeleted = true, deletedAt = now(), updatedAt = now()))
            txDao.deletePostings(id)
        }
        refreshLinked(existing.linkKind, existing.linkId)
        WriteResult.Success(id)
    }

    suspend fun restore(id: Long): WriteResult = io {
        val existing = txDao.byId(id) ?: return@io WriteResult.Invalid("লেনদেনটি খুঁজে পাওয়া যায়নি")
        db.withTransaction {
            audit("RESTORE", existing, id)
            val restored = existing.copy(isDeleted = false, deletedAt = null, updatedAt = now())
            txDao.update(restored)
            txDao.deletePostings(id)
            writePostings(id, restored)
        }
        refreshLinked(existing.linkKind, existing.linkId)
        WriteResult.Success(id)
    }

    // -------------------------------------------------------------- finances

    suspend fun createPurchase(input: PurchaseInput): WriteResult = io {
        val lineTotals = input.items.map { PurchaseMath.lineTotal(it.quantityMilli, it.unitPriceMinor) }
        val subtotal = lineTotals.sum()
        val total = subtotal - input.discountMinor + input.extraChargeMinor
        if (input.items.isEmpty()) return@io WriteResult.Invalid("অন্তত একটি পণ্য যোগ করুন")
        if (input.discountMinor < 0 || input.extraChargeMinor < 0) return@io WriteResult.Invalid("ছাড় বা অতিরিক্ত চার্জ ঋণাত্মক হতে পারবে না")
        if (input.discountMinor > subtotal) return@io WriteResult.Invalid("ছাড় মোট মূল্যের চেয়ে বেশি হতে পারবে না")
        if (total <= 0) return@io WriteResult.Invalid("মোট মূল্য শূন্যের বেশি হতে হবে")
        if (input.paidMinor < 0) return@io WriteResult.Invalid("পরিশোধিত টাকা ঋণাত্মক হতে পারবে না")
        if (input.paidMinor > total) return@io WriteResult.Invalid("পরিশোধিত টাকা মোট মূল্যের চেয়ে বেশি হতে পারবে না")
        val credit = input.creditMinor ?: (total - input.paidMinor)
        if (credit < 0) return@io WriteResult.Invalid("বাকির পরিমাণ ঋণাত্মক হতে পারবে না")
        if (input.paidMinor + credit != total) {
            return@io WriteResult.Invalid("পরিশোধ ও বাকির যোগফল মোট মূল্যের সমান হতে হবে")
        }
        if (input.paidMinor > 0 && input.accountId == null) {
            return@io WriteResult.Invalid("নগদ পরিশোধের জন্য একটি হিসাব নির্বাচন করুন")
        }
        if (credit > 0 && input.shopId == null && input.personId == null) {
            return@io WriteResult.Invalid("বাকির জন্য দোকান বা ব্যক্তি নির্বাচন করুন")
        }

        var id = 0L
        db.withTransaction {
            val purchase = com.shohankhan.hisaabos.data.db.PurchaseEntity(
                shopId = input.shopId,
                personId = input.personId,
                occurredAt = input.occurredAt,
                businessDate = input.businessDate,
                subtotalMinor = subtotal,
                discountMinor = input.discountMinor,
                extraChargeMinor = input.extraChargeMinor,
                totalMinor = total,
                paidMinor = input.paidMinor,
                creditMinor = credit,
                accountId = input.accountId,
                categoryId = input.categoryId,
                note = input.note,
            )
            id = db.purchaseDao().insertPurchase(purchase)
            db.purchaseDao().insertItems(
                input.items.mapIndexed { index, item ->
                    com.shohankhan.hisaabos.data.db.PurchaseItemEntity(
                        purchaseId = id,
                        name = item.name.trim(),
                        quantityMilli = item.quantityMilli,
                        unit = item.unit,
                        unitPriceMinor = item.unitPriceMinor,
                        lineTotalMinor = lineTotals[index],
                    )
                }
            )
            val group = "purchase-$id"
            if (input.paidMinor > 0) {
                postLocked(
                    TxDraft(
                        type = TxType.EXPENSE,
                        amountMinor = input.paidMinor,
                        occurredAt = input.occurredAt,
                        businessDate = input.businessDate,
                        accountId = input.accountId,
                        categoryId = input.categoryId,
                        shopId = input.shopId,
                        personId = input.personId,
                        partyKind = if (input.shopId != null) PartyKind.SHOP else PartyKind.PERSON,
                        linkKind = LinkKind.PURCHASE,
                        linkId = id,
                        title = "বাজার / ক্রয়",
                        note = input.note,
                        groupId = group,
                    )
                )
            }
            if (credit > 0) {
                postLocked(
                    TxDraft(
                        type = TxType.CREDIT_PURCHASE,
                        amountMinor = credit,
                        occurredAt = input.occurredAt,
                        businessDate = input.businessDate,
                        categoryId = input.categoryId,
                        shopId = input.shopId,
                        personId = input.personId,
                        partyKind = if (input.shopId != null) PartyKind.SHOP else PartyKind.PERSON,
                        linkKind = LinkKind.PURCHASE,
                        linkId = id,
                        title = "বাকিতে কেনা",
                        note = input.note,
                        groupId = group,
                    )
                )
            }
        }
        WriteResult.Success(id)
    }

    suspend fun createDebt(input: DebtInput): WriteResult = io {
        if (input.principalMinor <= 0) return@io WriteResult.Invalid("টাকার পরিমাণ শূন্যের বেশি হতে হবে")
        if (input.interestMinor < 0) return@io WriteResult.Invalid("সুদ ঋণাত্মক হতে পারবে না")
        if (input.accountId == null) return@io WriteResult.Invalid("কোন হিসাবে টাকা এসেছে বা গেছে তা নির্বাচন করুন")
        var debtId = 0L
        db.withTransaction {
            debtId = db.debtDao().insert(
                com.shohankhan.hisaabos.data.db.DebtEntity(
                    direction = input.direction,
                    personId = input.personId,
                    principalMinor = input.principalMinor,
                    interestMinor = input.interestMinor,
                    occurredAt = input.occurredAt,
                    businessDate = input.businessDate,
                    dueDate = input.dueDate,
                    accountId = input.accountId,
                    note = input.note,
                )
            )
            val borrowed = input.direction == com.shohankhan.hisaabos.domain.model.DebtDirection.BORROWED
            postLocked(
                TxDraft(
                    type = if (borrowed) TxType.DEBT_BORROWED else TxType.LENDING,
                    amountMinor = input.principalMinor,
                    occurredAt = input.occurredAt,
                    businessDate = input.businessDate,
                    accountId = input.accountId,
                    personId = input.personId,
                    partyKind = PartyKind.PERSON,
                    linkKind = LinkKind.DEBT,
                    linkId = debtId,
                    title = if (borrowed) "ধার নেওয়া" else "ধার দেওয়া",
                    note = input.note,
                )
            )
        }
        WriteResult.Success(debtId)
    }

    suspend fun settleDebt(
        debtId: Long,
        amountMinor: Long,
        principalMinor: Long,
        interestMinor: Long,
        feeMinor: Long,
        occurredAt: Long,
        businessDate: String,
        accountId: Long?,
        note: String? = null,
        installmentIndex: Int? = null,
    ): WriteResult = io {
        val debt = db.debtDao().byId(debtId) ?: return@io WriteResult.Invalid("ধারের তথ্য পাওয়া যায়নি")
        if (accountId == null) return@io WriteResult.Invalid("কোন হিসাব থেকে পরিশোধ হচ্ছে তা নির্বাচন করুন")
        val paid = settlementFor(LinkKind.DEBT, debtId)
        val outstandingPrincipal = (debt.principalMinor - paid.principalMinor).coerceAtLeast(0)
        val splitError = com.shohankhan.hisaabos.domain.loan.LoanMath.validateSettlement(
            amountMinor, principalMinor, interestMinor, feeMinor, outstandingPrincipal,
        )
        if (splitError != null) return@io WriteResult.Invalid(splitError)
        if (debt.isClosed) return@io WriteResult.Invalid("এই ধারটি ইতিমধ্যে সম্পূর্ণ পরিশোধিত")
        val borrowed = debt.direction == com.shohankhan.hisaabos.domain.model.DebtDirection.BORROWED
        db.withTransaction {
            postLocked(
                TxDraft(
                    type = if (borrowed) TxType.DEBT_REPAID else TxType.LOAN_COLLECTION,
                    amountMinor = amountMinor,
                    occurredAt = occurredAt,
                    businessDate = businessDate,
                    accountId = accountId,
                    personId = debt.personId,
                    partyKind = PartyKind.PERSON,
                    linkKind = LinkKind.DEBT,
                    linkId = debtId,
                    installmentIndex = installmentIndex,
                    interestMinor = interestMinor,
                    feeMinor = feeMinor,
                    title = if (borrowed) "ধার পরিশোধ" else "পাওনা আদায়",
                    note = note,
                )
            )
        }
        refreshLinked(LinkKind.DEBT, debtId)
        WriteResult.Success(debtId)
    }

    suspend fun createLoan(input: LoanInput): WriteResult = io {
        if (input.principalMinor <= 0) return@io WriteResult.Invalid("ঋণের মূল টাকা শূন্যের বেশি হতে হবে")
        if (input.interestMinor < 0 || input.feeMinor < 0) return@io WriteResult.Invalid("সুদ বা ফি ঋণাত্মক হতে পারবে না")
        if (input.accountId == null) return@io WriteResult.Invalid("ঋণের টাকা কোন হিসাবে এসেছে তা নির্বাচন করুন")

        val schedule = when {
            input.model == com.shohankhan.hisaabos.domain.model.InterestModel.MANUAL -> input.manualSchedule
            input.firstDueDate == null || input.installmentCount <= 0 -> emptyList()
            else -> com.shohankhan.hisaabos.domain.loan.LoanMath.buildSchedule(
                principalMinor = input.principalMinor,
                model = input.model,
                annualRateBp = input.annualRateBp,
                totalRepayableMinor = input.totalRepayableMinor,
                interestMinor = input.interestMinor,
                feeMinor = input.feeMinor,
                installmentCount = input.installmentCount,
                frequency = input.frequency,
                firstDueDate = input.firstDueDate,
            )
        }
        if (input.model != com.shohankhan.hisaabos.domain.model.InterestModel.MANUAL && schedule.isEmpty()) {
            return@io WriteResult.Invalid("কিস্তির সংখ্যা ও প্রথম কিস্তির তারিখ দিন")
        }
        if (schedule.isNotEmpty()) {
            val schedulePrincipal = schedule.sumOf { it.principalMinor }
            if (schedulePrincipal > input.principalMinor) {
                return@io WriteResult.Invalid("কিস্তির মূল টাকার যোগফল ঋণের চেয়ে বেশি হতে পারবে না")
            }
        }
        val totalRepayable = schedule.sumOf { it.amountMinor }.takeIf { it > 0 }
            ?: input.totalRepayableMinor
            ?: (input.principalMinor + input.interestMinor + input.feeMinor)

        var loanId = 0L
        db.withTransaction {
            loanId = db.loanDao().insert(
                com.shohankhan.hisaabos.data.db.LoanEntity(
                    kind = input.kind,
                    institution = input.institution.trim(),
                    branch = input.branch?.trim()?.takeIf { it.isNotEmpty() },
                    reference = input.reference?.trim()?.takeIf { it.isNotEmpty() },
                    principalMinor = input.principalMinor,
                    interestMinor = input.interestMinor,
                    feeMinor = input.feeMinor,
                    totalRepayableMinor = totalRepayable,
                    model = input.model,
                    annualRateBp = input.annualRateBp,
                    frequency = input.frequency,
                    installmentCount = input.installmentCount,
                    installmentAmountMinor = input.installmentAmountMinor.takeIf { it > 0 }
                        ?: schedule.firstOrNull()?.amountMinor ?: 0,
                    firstDueDate = input.firstDueDate,
                    disbursementDate = input.disbursementDate,
                    accountId = input.accountId,
                    notes = input.notes,
                )
            )
            if (schedule.isNotEmpty()) {
                db.scheduleDao().insertAll(
                    schedule.map {
                        com.shohankhan.hisaabos.data.db.ScheduleItemEntity(
                            ownerKind = com.shohankhan.hisaabos.domain.model.ScheduleOwner.LOAN,
                            ownerId = loanId,
                            sequence = it.sequence,
                            dueDate = it.dueDate,
                            principalMinor = it.principalMinor,
                            interestMinor = it.interestMinor,
                            feeMinor = it.feeMinor,
                            amountMinor = it.amountMinor,
                        )
                    }
                )
            }
            postLocked(
                TxDraft(
                    type = TxType.LOAN_DISBURSEMENT,
                    amountMinor = input.principalMinor,
                    occurredAt = BusinessDateMillis.of(input.disbursementDate),
                    businessDate = input.disbursementDate,
                    accountId = input.accountId,
                    partyKind = PartyKind.INSTITUTION,
                    partyName = input.institution,
                    linkKind = LinkKind.LOAN,
                    linkId = loanId,
                    title = if (input.kind == com.shohankhan.hisaabos.domain.model.LoanKind.NGO) "এনজিও ঋণ গ্রহণ" else "ব্যাংক ঋণ গ্রহণ",
                    note = input.notes,
                )
            )
        }
        WriteResult.Success(loanId)
    }

    suspend fun repayLoan(
        loanId: Long,
        amountMinor: Long,
        principalMinor: Long,
        interestMinor: Long,
        feeMinor: Long,
        occurredAt: Long,
        businessDate: String,
        accountId: Long?,
        installmentIndex: Int? = null,
        note: String? = null,
    ): WriteResult = io {
        val loan = db.loanDao().byId(loanId) ?: return@io WriteResult.Invalid("ঋণের তথ্য পাওয়া যায়নি")
        if (loan.isClosed) return@io WriteResult.Invalid("এই ঋণটি ইতিমধ্যে বন্ধ করা হয়েছে")
        if (accountId == null) return@io WriteResult.Invalid("কোন হিসাব থেকে পরিশোধ হচ্ছে তা নির্বাচন করুন")
        val paid = settlementFor(LinkKind.LOAN, loanId)
        val outstandingPrincipal = (loan.principalMinor - paid.principalMinor).coerceAtLeast(0)
        val splitError = com.shohankhan.hisaabos.domain.loan.LoanMath.validateSettlement(
            amountMinor, principalMinor, interestMinor, feeMinor, outstandingPrincipal,
        )
        if (splitError != null) return@io WriteResult.Invalid(splitError)
        db.withTransaction {
            postLocked(
                TxDraft(
                    type = TxType.LOAN_REPAYMENT,
                    amountMinor = amountMinor,
                    occurredAt = occurredAt,
                    businessDate = businessDate,
                    accountId = accountId,
                    partyKind = PartyKind.INSTITUTION,
                    partyName = loan.institution,
                    linkKind = LinkKind.LOAN,
                    linkId = loanId,
                    installmentIndex = installmentIndex,
                    interestMinor = interestMinor,
                    feeMinor = feeMinor,
                    title = "ঋণ পরিশোধ",
                    note = note,
                )
            )
        }
        refreshLinked(LinkKind.LOAN, loanId)
        WriteResult.Success(loanId)
    }

    suspend fun createEmi(input: EmiInput): WriteResult = io {
        if (input.productName.isBlank()) return@io WriteResult.Invalid("পণ্যের নাম লিখুন")
        if (input.totalFinancedPriceMinor <= 0) return@io WriteResult.Invalid("মোট মূল্য শূন্যের বেশি হতে হবে")
        if (input.downPaymentMinor < 0) return@io WriteResult.Invalid("ডাউন পেমেন্ট ঋণাত্মক হতে পারবে না")
        if (input.downPaymentMinor > input.totalFinancedPriceMinor) return@io WriteResult.Invalid("ডাউন পেমেন্ট মোট মূল্যের চেয়ে বেশি হতে পারবে না")
        if (input.interestMinor < 0 || input.feeMinor < 0) return@io WriteResult.Invalid("সুদ বা ফি ঋণাত্মক হতে পারবে না")
        if (input.installmentCount <= 0) return@io WriteResult.Invalid("কিস্তির সংখ্যা দিন")
        if (input.firstDueDate.isBlank()) return@io WriteResult.Invalid("প্রথম কিস্তির তারিখ দিন")
        if (input.downPaymentMinor > 0 && input.accountId == null) {
            return@io WriteResult.Invalid("ডাউন পেমেন্টের জন্য হিসাব নির্বাচন করুন")
        }
        val financed = input.totalFinancedPriceMinor - input.downPaymentMinor
        if (financed <= 0) return@io WriteResult.Invalid("অর্থায়নের পরিমাণ শূন্যের বেশি হতে হবে")

        val cashPrice = input.cashPriceMinor ?: (input.totalFinancedPriceMinor - input.interestMinor - input.feeMinor)
        if (cashPrice <= 0) return@io WriteResult.Invalid("পণ্যের নগদ মূল্য সঠিক নয়")
        if (cashPrice + input.interestMinor + input.feeMinor != input.totalFinancedPriceMinor) {
            return@io WriteResult.Invalid("নগদ মূল্য, সুদ ও ফি-র যোগফল মোট অর্থায়িত মূল্যের সমান হতে হবে")
        }
        val principalOutstanding = cashPrice - input.downPaymentMinor
        if (principalOutstanding < 0) return@io WriteResult.Invalid("ডাউন পেমেন্ট নগদ মূল্যের চেয়ে বেশি হতে পারবে না")

        val schedule = com.shohankhan.hisaabos.domain.loan.LoanMath.buildSchedule(
            principalMinor = principalOutstanding,
            model = com.shohankhan.hisaabos.domain.model.InterestModel.FLAT_TOTAL,
            annualRateBp = 0,
            totalRepayableMinor = financed,
            interestMinor = input.interestMinor,
            feeMinor = input.feeMinor,
            installmentCount = input.installmentCount,
            frequency = input.frequency,
            firstDueDate = input.firstDueDate,
        )
        var emiId = 0L
        db.withTransaction {
            emiId = db.emiDao().insert(
                com.shohankhan.hisaabos.data.db.EmiEntity(
                    productName = input.productName.trim(),
                    merchant = input.merchant?.trim()?.takeIf { it.isNotEmpty() },
                    purchaseDate = input.purchaseDate,
                    cashPriceMinor = cashPrice,
                    totalFinancedPriceMinor = input.totalFinancedPriceMinor,
                    downPaymentMinor = input.downPaymentMinor,
                    financedMinor = financed,
                    interestMinor = input.interestMinor,
                    feeMinor = input.feeMinor,
                    frequency = input.frequency,
                    installmentAmountMinor = schedule.firstOrNull()?.amountMinor ?: 0,
                    installmentCount = input.installmentCount,
                    firstDueDate = input.firstDueDate,
                    accountId = input.accountId,
                    categoryId = input.categoryId,
                    notes = input.notes,
                )
            )
            db.scheduleDao().insertAll(
                schedule.map {
                    com.shohankhan.hisaabos.data.db.ScheduleItemEntity(
                        ownerKind = com.shohankhan.hisaabos.domain.model.ScheduleOwner.EMI,
                        ownerId = emiId,
                        sequence = it.sequence,
                        dueDate = it.dueDate,
                        principalMinor = it.principalMinor,
                        interestMinor = it.interestMinor,
                        feeMinor = it.feeMinor,
                        amountMinor = it.amountMinor,
                    )
                }
            )
            // Recognise the product cost at purchase time (accrual, no cash movement).
            postLocked(
                TxDraft(
                    type = TxType.EMI_PURCHASE,
                    amountMinor = cashPrice,
                    occurredAt = input.occurredAt,
                    businessDate = input.purchaseDate,
                    categoryId = input.categoryId,
                    partyKind = PartyKind.INSTITUTION,
                    partyName = input.merchant,
                    linkKind = LinkKind.EMI,
                    linkId = emiId,
                    title = "কিস্তিতে ক্রয়: ${input.productName.trim()}",
                    note = input.notes,
                )
            )
            if (input.downPaymentMinor > 0) {
                postLocked(
                    TxDraft(
                        type = TxType.EMI_PAYMENT,
                        amountMinor = input.downPaymentMinor,
                        occurredAt = input.occurredAt,
                        businessDate = input.purchaseDate,
                        accountId = input.accountId,
                        partyKind = PartyKind.INSTITUTION,
                        partyName = input.merchant,
                        linkKind = LinkKind.EMI,
                        linkId = emiId,
                        installmentIndex = 0,
                        title = "ডাউন পেমেন্ট",
                    )
                )
            }
        }
        WriteResult.Success(emiId)
    }

    suspend fun payEmi(
        emiId: Long,
        amountMinor: Long,
        principalMinor: Long,
        interestMinor: Long,
        feeMinor: Long,
        occurredAt: Long,
        businessDate: String,
        accountId: Long?,
        installmentIndex: Int? = null,
        note: String? = null,
    ): WriteResult = io {
        val emi = db.emiDao().byId(emiId) ?: return@io WriteResult.Invalid("কিস্তি চুক্তি পাওয়া যায়নি")
        if (emi.isSettled) return@io WriteResult.Invalid("এই কিস্তি চুক্তি ইতিমধ্যে সম্পূর্ণ পরিশোধিত")
        if (accountId == null) return@io WriteResult.Invalid("কোন হিসাব থেকে পরিশোধ হচ্ছে তা নির্বাচন করুন")
        val paid = settlementFor(LinkKind.EMI, emiId)
        val principalTotal = (emi.cashPriceMinor ?: emi.totalFinancedPriceMinor)
        val outstandingPrincipal = (principalTotal - paid.principalMinor).coerceAtLeast(0)
        val splitError = com.shohankhan.hisaabos.domain.loan.LoanMath.validateSettlement(
            amountMinor, principalMinor, interestMinor, feeMinor, outstandingPrincipal,
        )
        if (splitError != null) return@io WriteResult.Invalid(splitError)
        db.withTransaction {
            postLocked(
                TxDraft(
                    type = TxType.EMI_PAYMENT,
                    amountMinor = amountMinor,
                    occurredAt = occurredAt,
                    businessDate = businessDate,
                    accountId = accountId,
                    partyKind = PartyKind.INSTITUTION,
                    partyName = emi.merchant,
                    linkKind = LinkKind.EMI,
                    linkId = emiId,
                    installmentIndex = installmentIndex,
                    interestMinor = interestMinor,
                    feeMinor = feeMinor,
                    title = "কিস্তি পরিশোধ: ${emi.productName}",
                    note = note,
                )
            )
        }
        refreshLinked(LinkKind.EMI, emiId)
        WriteResult.Success(emiId)
    }

    suspend fun createSavingsPlan(input: SavingsPlanInput): WriteResult = io {
        if (input.name.isBlank()) return@io WriteResult.Invalid("সঞ্চয়ের নাম লিখুন")
        if (input.targetAmountMinor != null && input.targetAmountMinor <= 0) {
            return@io WriteResult.Invalid("লক্ষ্যমাত্রা শূন্যের বেশি হতে হবে")
        }
        var planId = 0L
        db.withTransaction {
            val accountId = db.accountDao().insert(
                AccountEntity(
                    name = input.accountName?.takeIf { it.isNotBlank() } ?: input.name,
                    type = AccountType.SAVINGS,
                    institution = input.institution,
                    notes = "সঞ্চয় পরিকল্পনা",
                    sortOrder = db.accountDao().maxSortOrder() + 1,
                    iconKey = "savings",
                )
            )
            planId = db.savingsDao().insert(
                com.shohankhan.hisaabos.data.db.SavingsPlanEntity(
                    name = input.name.trim(),
                    institution = input.institution?.trim()?.takeIf { it.isNotEmpty() },
                    kind = input.kind,
                    accountId = accountId,
                    targetAmountMinor = input.targetAmountMinor,
                    targetDate = input.targetDate,
                    contributionAmountMinor = input.contributionAmountMinor,
                    note = input.note,
                )
            )
        }
        WriteResult.Success(planId)
    }

    suspend fun savingsMove(
        planId: Long,
        deposit: Boolean,
        amountMinor: Long,
        occurredAt: Long,
        businessDate: String,
        otherAccountId: Long?,
        note: String? = null,
    ): WriteResult = io {
        val plan = db.savingsDao().byId(planId) ?: return@io WriteResult.Invalid("সঞ্চয় পরিকল্পনা পাওয়া যায়নি")
        if (amountMinor <= 0) return@io WriteResult.Invalid("টাকার পরিমাণ শূন্যের বেশি হতে হবে")
        if (otherAccountId == null) return@io WriteResult.Invalid("অন্য হিসাবটি নির্বাচন করুন")
        if (otherAccountId == plan.accountId) return@io WriteResult.Invalid("একই হিসাবে টাকা সরানো যাবে না")
        db.withTransaction {
            postLocked(
                TxDraft(
                    type = if (deposit) TxType.SAVINGS_DEPOSIT else TxType.SAVINGS_WITHDRAWAL,
                    amountMinor = amountMinor,
                    occurredAt = occurredAt,
                    businessDate = businessDate,
                    accountId = if (deposit) otherAccountId else plan.accountId,
                    counterAccountId = if (deposit) plan.accountId else otherAccountId,
                    linkKind = LinkKind.SAVINGS,
                    linkId = planId,
                    title = if (deposit) "সঞ্চয় জমা" else "সঞ্চয় উত্তোলন",
                    note = note,
                )
            )
        }
        WriteResult.Success(planId)
    }

    suspend fun transfer(
        fromAccountId: Long,
        toAccountId: Long,
        amountMinor: Long,
        occurredAt: Long,
        businessDate: String,
        note: String? = null,
    ): WriteResult = io {
        if (amountMinor <= 0) return@io WriteResult.Invalid("টাকার পরিমাণ শূন্যের বেশি হতে হবে")
        if (fromAccountId == toAccountId) return@io WriteResult.Invalid("একই হিসাবে টাকা স্থানান্তর করা যাবে না")
        db.withTransaction {
            postLocked(
                TxDraft(
                    type = TxType.TRANSFER,
                    amountMinor = amountMinor,
                    occurredAt = occurredAt,
                    businessDate = businessDate,
                    accountId = fromAccountId,
                    counterAccountId = toAccountId,
                    title = "হিসাব স্থানান্তর",
                    note = note,
                )
            )
        }
        WriteResult.Success(toAccountId)
    }

    suspend fun adjustAccount(
        accountId: Long,
        amountMinor: Long,
        increase: Boolean,
        occurredAt: Long,
        businessDate: String,
        reason: String,
    ): WriteResult = io {
        if (amountMinor <= 0) return@io WriteResult.Invalid("টাকার পরিমাণ শূন্যের বেশি হতে হবে")
        if (reason.isBlank()) return@io WriteResult.Invalid("সমন্বয়ের কারণ লিখুন")
        db.withTransaction {
            postLocked(
                TxDraft(
                    type = TxType.ADJUSTMENT,
                    amountMinor = amountMinor,
                    flowSign = if (increase) 1 else -1,
                    occurredAt = occurredAt,
                    businessDate = businessDate,
                    accountId = accountId,
                    title = "হিসাব সমন্বয়",
                    note = reason,
                )
            )
        }
        WriteResult.Success(accountId)
    }

    // --------------------------------------------------------------- helpers

    /** Non-blocking duplicate warning used by the entry forms. */
    suspend fun isPotentialDuplicate(draft: TxDraft): Boolean =
        txDao.countSimilar(draft.type, draft.amountMinor, draft.businessDate, draft.accountId) > 0

    suspend fun settlementFor(linkKind: LinkKind, linkId: Long): Settlement {
        val type = when (linkKind) {
            LinkKind.DEBT -> null
            LinkKind.LOAN -> TxType.LOAN_REPAYMENT
            LinkKind.EMI -> TxType.EMI_PAYMENT
            else -> null
        }
        val rows: List<LinkPaidRow> = if (linkKind == LinkKind.DEBT) {
            val repayments = txDao.paidByLink(LinkKind.DEBT, TxType.DEBT_REPAID)
            val collections = txDao.paidByLink(LinkKind.DEBT, TxType.LOAN_COLLECTION)
            (repayments + collections).filter { it.linkId == linkId }
        } else {
            txDao.paidByLink(linkKind, type ?: TxType.ADJUSTMENT).filter { it.linkId == linkId }
        }
        return Settlement(
            principalMinor = rows.sumOf { it.principalMinor },
            interestMinor = rows.sumOf { it.interestMinor },
            feeMinor = rows.sumOf { it.feeMinor },
            amountMinor = rows.sumOf { it.amountMinor },
        )
    }

    private suspend fun refreshLinked(linkKind: LinkKind, linkId: Long?) {
        if (linkId == null) return
        when (linkKind) {
            LinkKind.DEBT -> {
                val debt = db.debtDao().byId(linkId) ?: return
                val paid = settlementFor(LinkKind.DEBT, linkId)
                val outstandingPrincipal = (debt.principalMinor - paid.principalMinor).coerceAtLeast(0)
                val outstandingInterest = (debt.interestMinor - paid.interestMinor).coerceAtLeast(0)
                db.debtDao().setClosed(linkId, outstandingPrincipal <= 0 && outstandingInterest <= 0)
            }
            LinkKind.LOAN -> {
                val loan = db.loanDao().byId(linkId) ?: return
                val paid = settlementFor(LinkKind.LOAN, linkId)
                val outstandingPrincipal = loan.principalMinor - paid.principalMinor
                val total = loan.totalRepayableMinor ?: loan.principalMinor
                val outstandingTotal = total - paid.amountMinor
                val closed = outstandingPrincipal <= 0 && outstandingTotal <= 0
                db.loanDao().setClosed(linkId, closed, if (closed) now() else null)
            }
            LinkKind.EMI -> {
                val emi = db.emiDao().byId(linkId) ?: return
                val paid = settlementFor(LinkKind.EMI, linkId)
                val total = emi.totalFinancedPriceMinor
                val closed = paid.amountMinor >= total
                db.emiDao().setSettled(linkId, closed)
            }
            else -> Unit
        }
    }

    private suspend fun audit(action: String, entity: TransactionEntity, id: Long) {
        runCatching {
            auditDao.insert(
                AuditLogEntity(
                    at = now(),
                    action = action,
                    entityTable = "transactions",
                    entityId = id,
                    snapshotJson = json.encodeToString(TransactionEntity.serializer(), entity),
                )
            )
        }
    }

    /**
     * Recomputes every posting from its transaction and reports mismatches.
     * Used by the "হিসাব যাচাই" tool and by database tests.
     */
    suspend fun verifyIntegrity(): List<IntegrityIssue> = withContext(Dispatchers.IO) {
        val issues = ArrayList<IntegrityIssue>()
        val active = txDao.allActive()
        val stored = txDao.allPostings().groupBy { it.transactionId }
        for (tx in active) {
            val expected = LedgerMath.deltas(tx).map { it.accountId to it.amountMinor }.sortedBy { it.first }
            val actual = stored[tx.id].orEmpty().map { it.accountId to it.deltaMinor }.sortedBy { it.first }
            if (expected != actual) {
                issues.add(
                    IntegrityIssue(
                        tx.id,
                        "হিসাবের পোস্টিং মিলছে না (${Money.formatPlain(tx.amountMinor)})",
                    )
                )
            }
        }
        val orphanPostings = stored.keys.filter { id -> active.none { it.id == id } }
        if (orphanPostings.isNotEmpty()) {
            issues.add(IntegrityIssue(0, "${orphanPostings.size}টি লেনদেনের পোস্টিং অনাথ অবস্থায় আছে"))
        }
        issues
    }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
}

/** Converts an ISO business date into an epoch timestamp at local noon (stable for history). */
object BusinessDateMillis {
    fun of(iso: String): Long {
        val date = com.shohankhan.hisaabos.domain.model.BizDate.parse(iso)
            ?: java.time.LocalDate.now()
        return date.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

/** Deterministic itemised-purchase arithmetic. */
object PurchaseMath {
    /** line total = round(quantityMilli × unitPriceMinor / 1000), exact integer maths. */
    fun lineTotal(quantityMilli: Long, unitPriceMinor: Long): Long {
        if (quantityMilli <= 0 || unitPriceMinor <= 0) return 0
        return com.shohankhan.hisaabos.domain.loan.LoanMath.roundDiv(
            Math.multiplyExact(quantityMilli, unitPriceMinor),
            1000,
        )
    }
}
