package com.shohankhan.hisaabos.domain.report

import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.DebtEntity
import com.shohankhan.hisaabos.data.db.EmiEntity
import com.shohankhan.hisaabos.data.db.LoanEntity
import com.shohankhan.hisaabos.data.db.PersonEntity
import com.shohankhan.hisaabos.data.db.SavingsPlanEntity
import com.shohankhan.hisaabos.data.db.ShopEntity
import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.domain.model.DebtDirection
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.ReminderKind
import com.shohankhan.hisaabos.domain.model.SavingsKind
import com.shohankhan.hisaabos.domain.model.TxType

data class AccountPosition(val account: AccountEntity, val balanceMinor: Long)

data class ShopPosition(
    val shop: ShopEntity,
    val outstandingMinor: Long,
    val creditTotalMinor: Long,
    val paidTotalMinor: Long,
)

data class DebtPosition(
    val debt: DebtEntity,
    val personName: String,
    val paidPrincipalMinor: Long,
    val paidInterestMinor: Long,
    val outstandingPrincipalMinor: Long,
    val outstandingTotalMinor: Long,
)

data class LoanPosition(
    val loan: LoanEntity,
    val paidPrincipalMinor: Long,
    val paidTotalMinor: Long,
    val outstandingPrincipalMinor: Long,
    val outstandingTotalMinor: Long,
)

data class EmiPosition(
    val emi: EmiEntity,
    val paidPrincipalMinor: Long,
    val paidTotalMinor: Long,
    val outstandingPrincipalMinor: Long,
    val outstandingTotalMinor: Long,
)

data class SavingsPosition(
    val plan: SavingsPlanEntity,
    val balanceMinor: Long,
    val progressPercent: Int,
)

data class PositionSummary(
    val totalAssetsMinor: Long = 0,
    val totalLiabilitiesMinor: Long = 0,
    val netWorthMinor: Long = 0,
    val cashAndBankMinor: Long = 0,
    val savingsMinor: Long = 0,
    val shopPayableMinor: Long = 0,
    val borrowedOutstandingMinor: Long = 0,
    val receivableMinor: Long = 0,
    val loanOutstandingMinor: Long = 0,
    val emiOutstandingMinor: Long = 0,
    val liabilityAccountMinor: Long = 0,
)

data class PaymentTotals(
    val principalMinor: Long = 0,
    val interestMinor: Long = 0,
    val feeMinor: Long = 0,
    val amountMinor: Long = 0,
)

data class FinanceSnapshot(
    val accounts: List<AccountPosition> = emptyList(),
    val shops: List<ShopPosition> = emptyList(),
    val debts: List<DebtPosition> = emptyList(),
    val loans: List<LoanPosition> = emptyList(),
    val emis: List<EmiPosition> = emptyList(),
    val savings: List<SavingsPosition> = emptyList(),
    val position: PositionSummary = PositionSummary(),
)

/**
 * Builds every financial position from persisted rows only.
 * Balances come from ledger postings; outstanding amounts from linked payments.
 */
object FinanceOverview {

    fun build(
        accounts: List<AccountEntity>,
        balances: Map<Long, Long>,
        shops: List<ShopEntity>,
        shopTotals: Map<Long, Pair<Long, Long>>,
        debts: List<DebtEntity>,
        people: List<PersonEntity>,
        debtPayments: Map<Long, PaymentTotals>,
        loans: List<LoanEntity>,
        loanPayments: Map<Long, PaymentTotals>,
        emis: List<EmiEntity>,
        emiPayments: Map<Long, PaymentTotals>,
        savingsPlans: List<SavingsPlanEntity>,
    ): FinanceSnapshot {
        val accountPositions = accounts.map { AccountPosition(it, balances[it.id] ?: 0L) }

        var assets = 0L
        var liabilityAccounts = 0L
        var cashAndBank = 0L
        var savingsTotal = 0L
        for (position in accountPositions) {
            val balance = position.balanceMinor
            val account = position.account
            if (account.isArchived) continue
            if (account.isLiability) {
                if (balance < 0) liabilityAccounts += -balance else assets += balance
            } else {
                assets += balance
                if (account.type in setOf(AccountType.CASH, AccountType.BANK, AccountType.MFS, AccountType.WALLET)) {
                    cashAndBank += balance
                }
                if (account.type == AccountType.SAVINGS) savingsTotal += balance
            }
        }

        val shopPositions = shops.map { shop ->
            val totals = shopTotals[shop.id] ?: (0L to 0L)
            ShopPosition(
                shop = shop,
                outstandingMinor = shop.openingBalanceMinor + totals.first - totals.second,
                creditTotalMinor = totals.first,
                paidTotalMinor = totals.second,
            )
        }
        val shopPayable = shopPositions.sumOf { it.outstandingMinor.coerceAtLeast(0) }

        val peopleById = people.associateBy { it.id }
        val debtPositions = debts.map { debt ->
            val paid = debtPayments[debt.id] ?: PaymentTotals()
            val outstandingPrincipal = (debt.principalMinor - paid.principalMinor).coerceAtLeast(0)
            val outstandingInterest = (debt.interestMinor - paid.interestMinor).coerceAtLeast(0)
            DebtPosition(
                debt = debt,
                personName = peopleById[debt.personId]?.name ?: "অজানা ব্যক্তি",
                paidPrincipalMinor = paid.principalMinor,
                paidInterestMinor = paid.interestMinor,
                outstandingPrincipalMinor = outstandingPrincipal,
                outstandingTotalMinor = outstandingPrincipal + outstandingInterest,
            )
        }
        val borrowed = debtPositions.filter { it.debt.direction == DebtDirection.BORROWED && !it.debt.isClosed }
            .sumOf { it.outstandingPrincipalMinor }
        val receivable = debtPositions.filter { it.debt.direction == DebtDirection.LENT && !it.debt.isClosed }
            .sumOf { it.outstandingPrincipalMinor }

        val loanPositions = loans.map { loan ->
            val paid = loanPayments[loan.id] ?: PaymentTotals()
            val outstandingPrincipal = (loan.principalMinor - paid.principalMinor).coerceAtLeast(0)
            val total = loan.totalRepayableMinor ?: loan.principalMinor
            LoanPosition(
                loan = loan,
                paidPrincipalMinor = paid.principalMinor,
                paidTotalMinor = paid.amountMinor,
                outstandingPrincipalMinor = outstandingPrincipal,
                outstandingTotalMinor = (total - paid.amountMinor).coerceAtLeast(0),
            )
        }
        val loanOutstanding = loanPositions.filter { !it.loan.isClosed }.sumOf { it.outstandingPrincipalMinor }

        val emiPositions = emis.map { emi ->
            val paid = emiPayments[emi.id] ?: PaymentTotals()
            val principalTotal = emi.cashPriceMinor ?: emi.totalFinancedPriceMinor
            val outstandingPrincipal = (principalTotal - paid.principalMinor).coerceAtLeast(0)
            EmiPosition(
                emi = emi,
                paidPrincipalMinor = paid.principalMinor,
                paidTotalMinor = paid.amountMinor,
                outstandingPrincipalMinor = outstandingPrincipal,
                outstandingTotalMinor = (emi.totalFinancedPriceMinor - paid.amountMinor).coerceAtLeast(0),
            )
        }
        val emiOutstanding = emiPositions.filter { !it.emi.isSettled }.sumOf { it.outstandingPrincipalMinor }

        val savingsPositions = savingsPlans.map { plan ->
            val balance = balances[plan.accountId] ?: 0L
            val progress = plan.targetAmountMinor?.takeIf { it > 0 }?.let { target ->
                ((balance * 100) / target).toInt().coerceIn(0, 100)
            } ?: 0
            SavingsPosition(plan, balance, progress)
        }

        val totalAssets = assets + receivable
        val totalLiabilities = liabilityAccounts + shopPayable + borrowed + loanOutstanding + emiOutstanding

        return FinanceSnapshot(
            accounts = accountPositions,
            shops = shopPositions,
            debts = debtPositions,
            loans = loanPositions,
            emis = emiPositions,
            savings = savingsPositions,
            position = PositionSummary(
                totalAssetsMinor = totalAssets,
                totalLiabilitiesMinor = totalLiabilities,
                netWorthMinor = totalAssets - totalLiabilities,
                cashAndBankMinor = cashAndBank,
                savingsMinor = savingsTotal,
                shopPayableMinor = shopPayable,
                borrowedOutstandingMinor = borrowed,
                receivableMinor = receivable,
                loanOutstandingMinor = loanOutstanding,
                emiOutstandingMinor = emiOutstanding,
                liabilityAccountMinor = liabilityAccounts,
            ),
        )
    }

    /** Converts raw linked-payment rows into per-link totals. */
    fun paymentTotals(
        rows: List<com.shohankhan.hisaabos.data.db.LinkPaidRow>,
    ): Map<Long, PaymentTotals> = rows.groupBy { it.linkId }.mapValues { (_, list) ->
        PaymentTotals(
            principalMinor = list.sumOf { it.principalMinor },
            interestMinor = list.sumOf { it.interestMinor },
            feeMinor = list.sumOf { it.feeMinor },
            amountMinor = list.sumOf { it.amountMinor },
        )
    }
}

enum class ObligationKind { DEBT, LOAN, EMI, SAVINGS, CUSTOM }

data class Obligation(
    val title: String,
    val dueDate: String,
    val amountMinor: Long,
    val paidMinor: Long = 0,
    val kind: ObligationKind,
    val linkKind: LinkKind? = null,
    val linkId: Long? = null,
    val installmentIndex: Int? = null,
    val subtitle: String? = null,
) {
    val remainingMinor: Long get() = (amountMinor - paidMinor).coerceAtLeast(0)
    fun isOverdue(todayIso: String): Boolean = dueDate < todayIso && remainingMinor > 0
    fun isDueToday(todayIso: String): Boolean = dueDate == todayIso && remainingMinor > 0
}

object ObligationEngine {

    fun build(
        loans: List<LoanEntity>,
        loanSchedule: List<com.shohankhan.hisaabos.data.db.ScheduleItemEntity>,
        loanPayments: List<com.shohankhan.hisaabos.data.db.TransactionEntity>,
        emis: List<EmiEntity>,
        emiSchedule: List<com.shohankhan.hisaabos.data.db.ScheduleItemEntity>,
        emiPayments: List<com.shohankhan.hisaabos.data.db.TransactionEntity>,
        debts: List<DebtPosition>,
        savings: List<SavingsPosition>,
        reminders: List<com.shohankhan.hisaabos.data.db.ReminderEntity>,
        savingsPayments: Map<Long, Long> = emptyMap(),
    ): List<Obligation> {
        val out = ArrayList<Obligation>()
        val loanById = loans.associateBy { it.id }
        for (item in loanSchedule) {
            val loan = loanById[item.ownerId] ?: continue
            if (loan.isClosed) continue
            val paid = loanPayments
                .filter { it.linkKind == LinkKind.LOAN && it.linkId == loan.id && it.installmentIndex == item.sequence }
                .sumOf { it.amountMinor }
            if (paid >= item.amountMinor) continue
            out.add(
                Obligation(
                    title = loan.institution,
                    dueDate = item.dueDate,
                    amountMinor = item.amountMinor,
                    paidMinor = paid,
                    kind = ObligationKind.LOAN,
                    linkKind = LinkKind.LOAN,
                    linkId = loan.id,
                    installmentIndex = item.sequence,
                    subtitle = "কিস্তি ${item.sequence}",
                )
            )
        }
        val emiById = emis.associateBy { it.id }
        for (item in emiSchedule) {
            val emi = emiById[item.ownerId] ?: continue
            if (emi.isSettled) continue
            val paid = emiPayments
                .filter { it.linkKind == LinkKind.EMI && it.linkId == emi.id && it.installmentIndex == item.sequence }
                .sumOf { it.amountMinor }
            if (paid >= item.amountMinor) continue
            out.add(
                Obligation(
                    title = emi.productName,
                    dueDate = item.dueDate,
                    amountMinor = item.amountMinor,
                    paidMinor = paid,
                    kind = ObligationKind.EMI,
                    linkKind = LinkKind.EMI,
                    linkId = emi.id,
                    installmentIndex = item.sequence,
                    subtitle = "কিস্তি ${item.sequence}/${emi.installmentCount}",
                )
            )
        }
        for (position in debts) {
            val due = position.debt.dueDate ?: continue
            if (position.debt.isClosed) continue
            if (position.outstandingTotalMinor <= 0) continue
            out.add(
                Obligation(
                    title = position.personName,
                    dueDate = due,
                    amountMinor = position.outstandingTotalMinor,
                    kind = ObligationKind.DEBT,
                    linkKind = LinkKind.DEBT,
                    linkId = position.debt.id,
                    subtitle = if (position.debt.direction == DebtDirection.BORROWED) "ধার ফেরত" else "ধার আদায়",
                )
            )
        }
        for (position in savings) {
            val plan = position.plan
            val amount = plan.contributionAmountMinor ?: continue
            if (amount <= 0 || plan.isArchived) continue
            val last = savingsPayments[plan.id]
            val due = nextContributionDate(plan, last)
            out.add(
                Obligation(
                    title = plan.name,
                    dueDate = due,
                    amountMinor = amount,
                    kind = ObligationKind.SAVINGS,
                    linkKind = LinkKind.SAVINGS,
                    linkId = plan.id,
                    subtitle = "নির্ধারিত সঞ্চয়",
                )
            )
        }
        for (reminder in reminders) {
            if (reminder.isDone) continue
            out.add(
                Obligation(
                    title = reminder.title,
                    dueDate = reminder.dueDate,
                    amountMinor = reminder.amountMinor ?: 0L,
                    kind = when (reminder.kind) {
                        ReminderKind.DEBT -> ObligationKind.DEBT
                        ReminderKind.LOAN -> ObligationKind.LOAN
                        ReminderKind.EMI -> ObligationKind.EMI
                        ReminderKind.SAVINGS -> ObligationKind.SAVINGS
                        ReminderKind.CUSTOM -> ObligationKind.CUSTOM
                    },
                    linkKind = reminder.linkKind.takeIf { it != LinkKind.NONE },
                    linkId = reminder.linkId,
                    subtitle = reminder.note,
                )
            )
        }
        return out.sortedWith(compareBy({ it.dueDate }, { it.title }))
    }

    private fun nextContributionDate(plan: com.shohankhan.hisaabos.data.db.SavingsPlanEntity, lastPaymentMillis: Long?): String {
        val frequency = plan.contributionFrequency ?: com.shohankhan.hisaabos.domain.model.RecurrenceFrequency.MONTHLY
        val base = lastPaymentMillis?.let { com.shohankhan.hisaabos.domain.model.BizDate.of(it) }
            ?: com.shohankhan.hisaabos.domain.model.BizDate.of(plan.createdAt)
        val date = com.shohankhan.hisaabos.domain.model.BizDate.parse(base) ?: return base
        val next = when (frequency) {
            com.shohankhan.hisaabos.domain.model.RecurrenceFrequency.DAILY -> date.plusDays(1)
            com.shohankhan.hisaabos.domain.model.RecurrenceFrequency.WEEKLY -> date.plusWeeks(1)
            com.shohankhan.hisaabos.domain.model.RecurrenceFrequency.MONTHLY -> date.plusMonths(1)
        }
        val today = java.time.LocalDate.now()
        var candidate = next
        var guard = 0
        while (candidate.isBefore(today) && guard < 240) {
            candidate = when (frequency) {
                com.shohankhan.hisaabos.domain.model.RecurrenceFrequency.DAILY -> candidate.plusDays(1)
                com.shohankhan.hisaabos.domain.model.RecurrenceFrequency.WEEKLY -> candidate.plusWeeks(1)
                com.shohankhan.hisaabos.domain.model.RecurrenceFrequency.MONTHLY -> candidate.plusMonths(1)
            }
            guard++
        }
        return candidate.toString()
    }
}
