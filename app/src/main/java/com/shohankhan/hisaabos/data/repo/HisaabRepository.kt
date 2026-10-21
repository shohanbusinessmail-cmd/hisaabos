package com.shohankhan.hisaabos.data.repo

import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.AccountBalanceRow
import com.shohankhan.hisaabos.data.db.BudgetCategoryEntity
import com.shohankhan.hisaabos.data.db.BudgetEntity
import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.data.db.DebtEntity
import com.shohankhan.hisaabos.data.db.EmiEntity
import com.shohankhan.hisaabos.data.db.HisaabDatabase
import com.shohankhan.hisaabos.data.db.LinkPaidRow
import com.shohankhan.hisaabos.data.db.LoanEntity
import com.shohankhan.hisaabos.data.db.PersonEntity
import com.shohankhan.hisaabos.data.db.PurchaseEntity
import com.shohankhan.hisaabos.data.db.PurchaseItemEntity
import com.shohankhan.hisaabos.data.db.RecurringRuleEntity
import com.shohankhan.hisaabos.data.db.ReminderEntity
import com.shohankhan.hisaabos.data.db.SavingsPlanEntity
import com.shohankhan.hisaabos.data.db.ScheduleItemEntity
import com.shohankhan.hisaabos.data.db.ShopEntity
import com.shohankhan.hisaabos.data.db.ShopTotalRow
import com.shohankhan.hisaabos.data.db.TxListItem
import com.shohankhan.hisaabos.domain.ledger.TransactionService
import com.shohankhan.hisaabos.domain.model.DebtDirection
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.LoanKind
import com.shohankhan.hisaabos.domain.model.ScheduleOwner
import com.shohankhan.hisaabos.domain.model.TxType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Read side of the app. Screens observe Flows; all writes go through
 * [TransactionService] so balances can never be edited directly.
 */
class HisaabRepository(
    private val db: HisaabDatabase,
    val service: TransactionService,
) {
    private val accounts = db.accountDao()
    private val categories = db.categoryDao()
    private val people = db.personDao()
    private val shops = db.shopDao()
    private val transactions = db.transactionDao()
    private val purchases = db.purchaseDao()
    private val debts = db.debtDao()
    private val loans = db.loanDao()
    private val emis = db.emiDao()
    private val schedule = db.scheduleDao()
    private val savings = db.savingsDao()
    private val budgets = db.budgetDao()
    private val reminders = db.reminderDao()
    private val recurring = db.recurringDao()

    // ---------------------------------------------------------------- masters
    suspend fun createAccount(
        name: String,
        type: com.shohankhan.hisaabos.domain.model.AccountType,
        institution: String? = null,
        isLiability: Boolean = false,
    ): Long = accounts.insert(
        AccountEntity(
            name = name.trim(),
            type = type,
            institution = institution?.trim()?.takeIf { it.isNotEmpty() },
            isLiability = isLiability,
            sortOrder = accounts.maxSortOrder() + 1,
        )
    )

    suspend fun createCategory(name: String, kind: com.shohankhan.hisaabos.domain.model.CategoryKind): Long {
        val order = categories.maxSortOrder() + 1
        return categories.insert(
            CategoryEntity(name = name.trim(), kind = kind, sortOrder = order)
        )
    }

    suspend fun createCategoryDetailed(name: String, kind: com.shohankhan.hisaabos.domain.model.CategoryKind, iconKey: String, colorArgb: Long): Long =
        categories.insert(
            CategoryEntity(
                name = name.trim(),
                kind = kind,
                iconKey = iconKey,
                colorArgb = colorArgb,
                sortOrder = categories.maxSortOrder() + 1,
            )
        )

    suspend fun updateCategory(category: CategoryEntity) = categories.update(category)

    suspend fun archiveCategory(id: Long, archived: Boolean) = categories.setArchived(id, archived)

    suspend fun deleteCategory(id: Long) = categories.deleteById(id)

    suspend fun categoryUsage(id: Long): Int = categories.usageCount(id)

    suspend fun countCategoryName(name: String, kind: com.shohankhan.hisaabos.domain.model.CategoryKind, excludeId: Long): Int =
        categories.countWithName(name, kind.name, excludeId)

    suspend fun reassignCategory(fromId: Long, toId: Long?) = categories.reassignTransactions(fromId, toId)

    suspend fun createShop(name: String): Long = shops.insert(ShopEntity(name = name.trim()))

    suspend fun createPerson(name: String): Long = people.insert(PersonEntity(name = name.trim()))

    suspend fun updateAccount(account: AccountEntity) = accounts.update(account)

    suspend fun archiveAccount(id: Long, archived: Boolean) = accounts.setArchived(id, archived)

    fun observeAccounts(): Flow<List<AccountEntity>> = accounts.observeAll()
    fun observeActiveAccounts(): Flow<List<AccountEntity>> = accounts.observeActive()
    fun observeAccount(id: Long): Flow<AccountEntity?> = accounts.observeById(id)
    suspend fun account(id: Long): AccountEntity? = accounts.byId(id)
    suspend fun activeAccounts(): List<AccountEntity> = accounts.active()

    fun observeCategories(): Flow<List<CategoryEntity>> = categories.observeAll()
    fun observeActiveCategories(): Flow<List<CategoryEntity>> = categories.observeActive()
    suspend fun activeCategories(): List<CategoryEntity> = categories.active()
    suspend fun category(id: Long): CategoryEntity? = categories.byId(id)

    fun observePeople(): Flow<List<PersonEntity>> = people.observeAll()
    fun observePerson(id: Long): Flow<PersonEntity?> = people.observeById(id)
    suspend fun person(id: Long): PersonEntity? = people.byId(id)

    fun observeShops(): Flow<List<ShopEntity>> = shops.observeAll()
    fun observeShop(id: Long): Flow<ShopEntity?> = shops.observeById(id)
    suspend fun shop(id: Long): ShopEntity? = shops.byId(id)

    // --------------------------------------------------------------- ledger
    fun observeBalances(): Flow<List<AccountBalanceRow>> = transactions.observeBalances()
    suspend fun balances(): Map<Long, Long> = transactions.balances().associate { it.accountId to it.balanceMinor }
    fun observeRecentTransactions(limit: Int = 50): Flow<List<TxListItem>> = transactions.observeRecent(limit)
    fun observeTransaction(id: Long): Flow<com.shohankhan.hisaabos.data.db.TransactionEntity?> = transactions.observeById(id)
    suspend fun transaction(id: Long): com.shohankhan.hisaabos.data.db.TransactionEntity? = transactions.byId(id)
    fun observeTransactionsBetween(from: String, to: String) = transactions.observeBetween(from, to)
    suspend fun transactionsBetween(from: String, to: String) = transactions.between(from, to)
    fun observeTransactionList(
        from: String?,
        to: String?,
        types: List<TxType>,
        categoryId: Long?,
        accountId: Long?,
        shopId: Long?,
        personId: Long?,
        minAmount: Long?,
        maxAmount: Long?,
        search: String?,
        sort: String,
        limit: Int,
        offset: Int,
    ): Flow<List<TxListItem>> = transactions.observeFiltered(
        from = from,
        to = to,
        noTypeFilter = if (types.isEmpty()) 1 else 0,
        types = types.ifEmpty { listOf(TxType.INCOME) },
        categoryId = categoryId,
        accountId = accountId,
        shopId = shopId,
        personId = personId,
        minAmount = minAmount,
        maxAmount = maxAmount,
        search = search?.takeIf { it.isNotBlank() }?.let { "%${it.trim()}%" },
        sort = sort,
        limit = limit,
        offset = offset,
    )

    suspend fun accountTransactions(accountId: Long, limit: Int = 100) = transactions.forAccount(accountId, limit)
    suspend fun shopTransactions(shopId: Long) = transactions.forShop(shopId)
    suspend fun personTransactions(personId: Long) = transactions.forPerson(personId)
    fun observeForLink(linkKind: LinkKind, linkId: Long) = transactions.observeForLink(linkKind, linkId)
    suspend fun forLink(linkKind: LinkKind, linkId: Long) = transactions.forLink(linkKind, linkId)
    fun observePaidByLink(linkKind: LinkKind, type: TxType): Flow<List<LinkPaidRow>> =
        transactions.observePaidByLink(linkKind, type)
    fun observeShopTotals(): Flow<List<ShopTotalRow>> = transactions.observeShopTotals()
    suspend fun paidByLink(linkKind: LinkKind, type: TxType): List<LinkPaidRow> =
        transactions.paidByLink(linkKind, type)
    suspend fun allActiveTransactions() = transactions.allActive()

    // ------------------------------------------------------------- purchases
    fun observeRecentPurchases(limit: Int = 50): Flow<List<PurchaseEntity>> = purchases.observeRecent(limit)
    fun observePurchase(id: Long): Flow<PurchaseEntity?> = purchases.observeById(id)
    suspend fun purchase(id: Long): PurchaseEntity? = purchases.byId(id)
    fun observePurchaseItems(purchaseId: Long): Flow<List<PurchaseItemEntity>> = purchases.observeItems(purchaseId)
    suspend fun purchaseItems(purchaseId: Long): List<PurchaseItemEntity> = purchases.items(purchaseId)
    fun observeShopPurchases(shopId: Long): Flow<List<PurchaseEntity>> = purchases.observeForShop(shopId)
    suspend fun recentItemNames(): List<String> = purchases.recentItemNames()

    // ----------------------------------------------------------------- debts
    fun observeDebts(): Flow<List<DebtEntity>> = debts.observeAll()
    fun observeOpenDebts(direction: DebtDirection): Flow<List<DebtEntity>> = debts.observeOpen(direction)
    fun observeDebt(id: Long): Flow<DebtEntity?> = debts.observeById(id)
    suspend fun debt(id: Long): DebtEntity? = debts.byId(id)
    fun observePersonDebts(personId: Long): Flow<List<DebtEntity>> = debts.observeForPerson(personId)

    // ----------------------------------------------------------------- loans
    fun observeLoans(): Flow<List<LoanEntity>> = loans.observeAll()
    fun observeLoans(kind: LoanKind): Flow<List<LoanEntity>> = loans.observeByKind(kind)
    fun observeLoan(id: Long): Flow<LoanEntity?> = loans.observeById(id)
    suspend fun loan(id: Long): LoanEntity? = loans.byId(id)

    fun observeSchedule(ownerKind: ScheduleOwner, ownerId: Long): Flow<List<ScheduleItemEntity>> =
        schedule.observe(ownerKind, ownerId)
    suspend fun schedule(ownerKind: ScheduleOwner, ownerId: Long): List<ScheduleItemEntity> =
        schedule.forOwner(ownerKind, ownerId)
    fun observeAllSchedule(): Flow<List<ScheduleItemEntity>> = schedule.observeAll()

    // ------------------------------------------------------------------- EMI
    fun observeEmis(): Flow<List<EmiEntity>> = emis.observeAll()
    fun observeEmi(id: Long): Flow<EmiEntity?> = emis.observeById(id)
    suspend fun emi(id: Long): EmiEntity? = emis.byId(id)

    // --------------------------------------------------------------- savings
    fun observeSavingsPlans(): Flow<List<SavingsPlanEntity>> = savings.observeAll()
    fun observeSavingsPlan(id: Long): Flow<SavingsPlanEntity?> = savings.observeById(id)
    suspend fun savingsPlan(id: Long): SavingsPlanEntity? = savings.byId(id)

    // --------------------------------------------------------------- budgets
    fun observeBudgets(): Flow<List<BudgetEntity>> = budgets.observeAll()
    fun observeBudget(id: Long): Flow<BudgetEntity?> = budgets.observeById(id)
    suspend fun budget(id: Long): BudgetEntity? = budgets.byId(id)
    suspend fun budgetCategories(budgetId: Long): List<BudgetCategoryEntity> = budgets.categoriesFor(budgetId)

    // ------------------------------------------------------------- reminders
    fun observeReminders(): Flow<List<ReminderEntity>> = reminders.observeAll()
    fun observeOpenReminders(): Flow<List<ReminderEntity>> = reminders.observeOpen()
    fun observeRecurring(): Flow<List<RecurringRuleEntity>> = recurring.observeAll()
    suspend fun recurringRules(): List<RecurringRuleEntity> = recurring.active()

    /**
     * Emits whenever any of the given tables change; used by snapshot screens
     * that must recompute in-memory aggregates. Room flows already observe the
     * underlying tables, so this is a merge of the matching primary keys.
     */
    fun invalidation(vararg tables: String): Flow<Unit> {
        val flows = tables.mapNotNull { table ->
            when (table) {
                "transactions" -> transactions.observeRecent(1)
                "accounts" -> accounts.observeAll()
                "categories" -> categories.observeAll()
                "shops" -> shops.observeAll()
                "people" -> people.observeAll()
                "debts" -> debts.observeAll()
                "loans" -> loans.observeAll()
                "emi_purchases" -> emis.observeAll()
                "savings_plans" -> savings.observeAll()
                "budgets" -> budgets.observeAll()
                "reminders" -> reminders.observeAll()
                "recurring_rules" -> recurring.observeAll()
                "purchases" -> purchases.observeRecent(1)
                "schedule_items" -> schedule.observeAll()
                else -> null
            } as Flow<Any?>?
        }
        if (flows.isEmpty()) return flowOf(Unit)
        return combine(flows) { }.map { }
    }

    suspend fun recentTransactions(limit: Int): List<TxListItem> = transactions.recent(limit)
    suspend fun allForLinkKind(linkKind: LinkKind) = transactions.allForLinkKind(linkKind)
    suspend fun allSchedule(): List<ScheduleItemEntity> = schedule.all()
    suspend fun openReminders(): List<ReminderEntity> = reminders.open()
    suspend fun openLoans(): List<LoanEntity> = loans.open()
    suspend fun openEmis(): List<EmiEntity> = emis.open()
    suspend fun openDebts(): List<DebtEntity> = debts.open()
    suspend fun allCategories(): List<CategoryEntity> = categories.active()
    suspend fun allShops(): List<ShopEntity> = shops.active()
    suspend fun allPeople(): List<PersonEntity> = people.observeAll().first()
    suspend fun allSavings(): List<SavingsPlanEntity> = savings.observeAll().first()
    fun observeShopTotalsList(): Flow<List<ShopTotalRow>> = transactions.observeShopTotals()
    suspend fun categoryTotals(from: String, to: String) = transactions.categoryTotals(from, to)

    // ------------------------------------------------------------- integrity
    suspend fun verifyIntegrity() = service.verifyIntegrity()

    suspend fun dataCounts(): Map<String, Int> {
        val dao = db.backupDao()
        return mapOf(
            "accounts" to dao.accounts().size,
            "categories" to dao.categories().size,
            "transactions" to dao.transactions().size,
            "people" to dao.people().size,
            "shops" to dao.shops().size,
            "loans" to dao.loans().size,
            "emis" to dao.emis().size,
            "savings" to dao.savingsPlans().size,
            "budgets" to dao.budgets().size,
        )
    }

    suspend fun wipeEverything() {
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
}
