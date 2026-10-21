package com.shohankhan.hisaabos.backup

import androidx.room.withTransaction
import com.shohankhan.hisaabos.BuildConfig
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.BudgetEntity
import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.data.db.DebtEntity
import com.shohankhan.hisaabos.data.db.EmiEntity
import com.shohankhan.hisaabos.data.db.HisaabDatabase
import com.shohankhan.hisaabos.data.db.LedgerPostingEntity
import com.shohankhan.hisaabos.data.db.LoanEntity
import com.shohankhan.hisaabos.data.db.PersonEntity
import com.shohankhan.hisaabos.data.db.PurchaseEntity
import com.shohankhan.hisaabos.data.db.PurchaseItemEntity
import com.shohankhan.hisaabos.data.db.ReminderEntity
import com.shohankhan.hisaabos.data.db.SavingsPlanEntity
import com.shohankhan.hisaabos.data.db.ScheduleItemEntity
import com.shohankhan.hisaabos.data.db.ShopEntity
import com.shohankhan.hisaabos.data.db.TransactionEntity
import com.shohankhan.hisaabos.data.prefs.AppPreferences
import com.shohankhan.hisaabos.domain.model.LedgerMath
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.ScheduleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.time.ZoneId

enum class RestoreMode { REPLACE_ALL, MERGE }

/**
 * Builds backups from the database and restores them inside a single Room
 * transaction. A failed restore rolls back completely; the existing books are
 * never left half-imported.
 */
class BackupService(
    private val db: HisaabDatabase,
    private val prefs: AppPreferences,
) {

    suspend fun export(passphrase: CharArray?): String = withContext(Dispatchers.IO) {
        val dao = db.backupDao()
        val settings = prefs.settings.first()
        val envelope = BackupEnvelope(
            createdAt = System.currentTimeMillis(),
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            timeZone = ZoneId.systemDefault().id,
            accounts = dao.accounts(),
            categories = dao.categories(),
            people = dao.people(),
            shops = dao.shops(),
            transactions = dao.transactions(),
            ledgerPostings = dao.ledgerPostings(),
            purchases = dao.purchases(),
            purchaseItems = dao.purchaseItems(),
            debts = dao.debts(),
            loans = dao.loans(),
            emis = dao.emis(),
            scheduleItems = dao.scheduleItems(),
            savingsPlans = dao.savingsPlans(),
            budgets = dao.budgets(),
            budgetCategories = dao.budgetCategories(),
            reminders = dao.reminders(),
            recurringRules = dao.recurringRules(),
            auditLog = dao.auditLog(),
            settings = BackupSettings(
                autoLockSeconds = settings.autoLockSeconds,
                hideAmounts = settings.hideAmounts,
                remindersEnabled = settings.remindersEnabled,
                showBanglaDigits = settings.showBanglaDigits,
                screenProtection = settings.screenProtection,
            ),
        )
        BackupCodec.encode(envelope, passphrase)
    }

    suspend fun restore(envelope: BackupEnvelope, mode: RestoreMode): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val counts = db.withTransaction {
                if (mode == RestoreMode.REPLACE_ALL) replaceAll(envelope) else merge(envelope)
            }
            prefs.markBackupDone(System.currentTimeMillis())
            RestoreResult.Success(counts)
        } catch (t: Throwable) {
            RestoreResult.Failed("পুনরুদ্ধার ব্যর্থ হয়েছে; আগের তথ্য অপরিবর্তিত আছে। (${t.javaClass.simpleName})")
        }
    }

    private suspend fun clearAll() {
        val m = db.maintenanceDao()
        m.clearPostings()
        m.clearPurchaseItems()
        m.clearPurchases()
        m.clearTransactions()
        m.clearSchedule()
        m.clearDebts()
        m.clearLoans()
        m.clearEmis()
        m.clearBudgetCategories()
        m.clearBudgets()
        m.clearSavings()
        m.clearReminders()
        m.clearRecurring()
        m.clearAudit()
        m.clearShops()
        m.clearPeople()
        m.clearCategories()
        m.clearAccounts()
    }

    private suspend fun replaceAll(envelope: BackupEnvelope): Map<String, Int> {
        clearAll()
        insertMaster(envelope)
        envelope.transactions.forEach { tx ->
            db.transactionDao().insert(tx)
        }
        rebuildPostings(envelope.transactions)
        // Purchases and items
        envelope.purchases.forEach { db.purchaseDao().insertPurchase(it) }
        if (envelope.purchaseItems.isNotEmpty()) db.purchaseDao().insertItems(envelope.purchaseItems)
        envelope.debts.forEach { db.debtDao().insert(it) }
        envelope.loans.forEach { db.loanDao().insert(it) }
        envelope.emis.forEach { db.emiDao().insert(it) }
        if (envelope.scheduleItems.isNotEmpty()) db.scheduleDao().insertAll(envelope.scheduleItems)
        envelope.savingsPlans.forEach { db.savingsDao().insert(it) }
        envelope.budgets.forEach { db.budgetDao().insert(it) }
        if (envelope.budgetCategories.isNotEmpty()) db.budgetDao().insertCategories(envelope.budgetCategories)
        envelope.reminders.forEach { db.reminderDao().insert(it) }
        envelope.recurringRules.forEach { db.recurringDao().insert(it) }
        envelope.auditLog.forEach { db.auditDao().insert(it) }
        return envelope.counts
    }

    private suspend fun insertMaster(envelope: BackupEnvelope) {
        envelope.accounts.forEach { db.accountDao().insert(it) }
        envelope.categories.forEach { db.categoryDao().insert(it) }
        envelope.people.forEach { db.personDao().insert(it) }
        envelope.shops.forEach { db.shopDao().insert(it) }
    }

    /**
     * Merge keeps the current books and adds the backup on top:
     * master records are matched by natural key, finance records get new IDs and
     * ledger postings are recomputed from the imported transactions.
     */
    private suspend fun merge(envelope: BackupEnvelope): Map<String, Int> {
        val existingAccounts = db.backupDao().accounts()
        val existingCategories = db.backupDao().categories()
        val existingPeople = db.backupDao().people()
        val existingShops = db.backupDao().shops()

        val accountMap = HashMap<Long, Long>()
        for (account in envelope.accounts) {
            val match = existingAccounts.firstOrNull { it.name == account.name && it.type == account.type }
            accountMap[account.id] = match?.id ?: db.accountDao().insert(account.copy(id = 0))
        }
        val categoryMap = HashMap<Long, Long>()
        for (category in envelope.categories) {
            val match = existingCategories.firstOrNull { it.name.equals(category.name, ignoreCase = true) && it.kind == category.kind }
            categoryMap[category.id] = match?.id ?: db.categoryDao().insert(category.copy(id = 0))
        }
        val personMap = HashMap<Long, Long>()
        for (person in envelope.people) {
            val match = existingPeople.firstOrNull { it.name == person.name }
            personMap[person.id] = match?.id ?: db.personDao().insert(person.copy(id = 0))
        }
        val shopMap = HashMap<Long, Long>()
        for (shop in envelope.shops) {
            val match = existingShops.firstOrNull { it.name == shop.name }
            shopMap[shop.id] = match?.id ?: db.shopDao().insert(
                shop.copy(id = 0, personId = shop.personId?.let { personMap[it] })
            )
        }

        val existingTransactions = db.backupDao().transactions().filter { !it.isDeleted }
        val purchaseMap = HashMap<Long, Long>()
        for (purchase in envelope.purchases) {
            purchaseMap[purchase.id] = db.purchaseDao().insertPurchase(
                purchase.copy(
                    id = 0,
                    shopId = purchase.shopId?.let { shopMap[it] },
                    personId = purchase.personId?.let { personMap[it] },
                    accountId = purchase.accountId?.let { accountMap[it] },
                    categoryId = purchase.categoryId?.let { categoryMap[it] },
                )
            )
        }
        for (item in envelope.purchaseItems) {
            val newPurchaseId = purchaseMap[item.purchaseId] ?: continue
            db.purchaseDao().insertItems(listOf(item.copy(id = 0, purchaseId = newPurchaseId)))
        }
        val debtMap = HashMap<Long, Long>()
        for (debt in envelope.debts) {
            debtMap[debt.id] = db.debtDao().insert(
                debt.copy(
                    id = 0,
                    personId = personMap[debt.personId] ?: continue,
                    accountId = debt.accountId?.let { accountMap[it] },
                )
            )
        }
        val loanMap = HashMap<Long, Long>()
        for (loan in envelope.loans) {
            loanMap[loan.id] = db.loanDao().insert(
                loan.copy(id = 0, accountId = loan.accountId?.let { accountMap[it] })
            )
        }
        val emiMap = HashMap<Long, Long>()
        for (emi in envelope.emis) {
            emiMap[emi.id] = db.emiDao().insert(
                emi.copy(
                    id = 0,
                    accountId = emi.accountId?.let { accountMap[it] },
                    categoryId = emi.categoryId?.let { categoryMap[it] },
                )
            )
        }
        val savingsMap = HashMap<Long, Long>()
        for (plan in envelope.savingsPlans) {
            val newAccount = accountMap[plan.accountId] ?: continue
            savingsMap[plan.id] = db.savingsDao().insert(plan.copy(id = 0, accountId = newAccount))
        }
        val budgetMap = HashMap<Long, Long>()
        for (budget in envelope.budgets) {
            budgetMap[budget.id] = db.budgetDao().insert(budget.copy(id = 0))
        }
        val newBudgetCategories = envelope.budgetCategories.mapNotNull { ref ->
            val budgetId = budgetMap[ref.budgetId] ?: return@mapNotNull null
            val categoryId = categoryMap[ref.categoryId] ?: return@mapNotNull null
            com.shohankhan.hisaabos.data.db.BudgetCategoryEntity(budgetId, categoryId)
        }
        if (newBudgetCategories.isNotEmpty()) db.budgetDao().insertCategories(newBudgetCategories)

        val importedTransactions = ArrayList<TransactionEntity>()
        for (tx in envelope.transactions) {
            val accountId = tx.accountId?.let { accountMap[it] } ?: tx.accountId
            if (!tx.isDeleted && existingTransactions.any {
                    it.type == tx.type && it.amountMinor == tx.amountMinor &&
                        it.businessDate == tx.businessDate && it.accountId == accountId &&
                        it.title == tx.title
                }
            ) {
                continue
            }
            val newLinkId = when (tx.linkKind) {
                LinkKind.DEBT -> tx.linkId?.let { debtMap[it] }
                LinkKind.LOAN -> tx.linkId?.let { loanMap[it] }
                LinkKind.EMI -> tx.linkId?.let { emiMap[it] }
                LinkKind.SAVINGS -> tx.linkId?.let { savingsMap[it] }
                LinkKind.PURCHASE -> tx.linkId?.let { purchaseMap[it] }
                LinkKind.BUDGET -> tx.linkId?.let { budgetMap[it] }
                else -> null
            }
            val copy = tx.copy(
                id = 0,
                accountId = tx.accountId?.let { accountMap[it] },
                counterAccountId = tx.counterAccountId?.let { accountMap[it] },
                categoryId = tx.categoryId?.let { categoryMap[it] },
                shopId = tx.shopId?.let { shopMap[it] },
                personId = tx.personId?.let { personMap[it] },
                linkId = newLinkId,
            )
            val newId = db.transactionDao().insert(copy)
            importedTransactions.add(copy.copy(id = newId))
        }
        rebuildPostings(importedTransactions)
        for (item in envelope.scheduleItems) {
            val newOwnerId = when (item.ownerKind) {
                ScheduleOwner.LOAN -> loanMap[item.ownerId]
                ScheduleOwner.EMI -> emiMap[item.ownerId]
            } ?: continue
            db.scheduleDao().insertAll(listOf(item.copy(id = 0, ownerId = newOwnerId)))
        }
        for (reminder in envelope.reminders) {
            db.reminderDao().insert(
                reminder.copy(
                    id = 0,
                    linkId = when (reminder.linkKind) {
                        LinkKind.LOAN -> reminder.linkId?.let { loanMap[it] }
                        LinkKind.EMI -> reminder.linkId?.let { emiMap[it] }
                        LinkKind.DEBT -> reminder.linkId?.let { debtMap[it] }
                        LinkKind.SAVINGS -> reminder.linkId?.let { savingsMap[it] }
                        else -> null
                    },
                )
            )
        }
        for (rule in envelope.recurringRules) {
            db.recurringDao().insert(
                rule.copy(
                    id = 0,
                    accountId = rule.accountId?.let { accountMap[it] },
                    categoryId = rule.categoryId?.let { categoryMap[it] },
                    personId = rule.personId?.let { personMap[it] },
                )
            )
        }
        return mapOf(
            "accounts" to accountMap.size,
            "categories" to categoryMap.size,
            "transactions" to importedTransactions.size,
            "people" to personMap.size,
            "shops" to shopMap.size,
            "purchases" to purchaseMap.size,
            "loans" to loanMap.size,
            "debts" to debtMap.size,
        )
    }

    private suspend fun rebuildPostings(transactions: List<TransactionEntity>) {
        for (tx in transactions) {
            if (tx.isDeleted) continue
            val postings = LedgerMath.deltas(tx).map { delta ->
                LedgerPostingEntity(
                    transactionId = tx.id,
                    accountId = delta.accountId,
                    deltaMinor = delta.amountMinor,
                    businessDate = tx.businessDate,
                    occurredAt = tx.occurredAt,
                )
            }
            if (postings.isNotEmpty()) db.transactionDao().insertPostings(postings)
        }
    }

    /** True when at least one transaction or master row exists. */
    suspend fun hasData(): Boolean =
        db.backupDao().accounts().isNotEmpty() || db.backupDao().transactions().isNotEmpty()
}
