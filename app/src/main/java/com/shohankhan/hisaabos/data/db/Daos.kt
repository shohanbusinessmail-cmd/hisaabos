package com.shohankhan.hisaabos.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shohankhan.hisaabos.domain.model.DebtDirection
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.LoanKind
import com.shohankhan.hisaabos.domain.model.ReminderKind
import com.shohankhan.hisaabos.domain.model.ScheduleOwner
import com.shohankhan.hisaabos.domain.model.TxType
import kotlinx.coroutines.flow.Flow

/** A transaction row joined with the display names of everything it references. */
data class TxListItem(
    @androidx.room.Embedded val tx: TransactionEntity,
    val accountName: String? = null,
    val counterAccountName: String? = null,
    val categoryName: String? = null,
    val categoryColor: Long? = null,
    val shopName: String? = null,
    val personName: String? = null,
)

data class AccountBalanceRow(val accountId: Long, val balanceMinor: Long)
data class TypeTotalRow(val type: TxType, val totalMinor: Long, val itemCount: Int)
data class CategoryTotalRow(val categoryId: Long?, val type: TxType, val totalMinor: Long)
data class ShopTotalRow(val shopId: Long, val type: TxType, val totalMinor: Long)
data class LinkPaidRow(
    val linkId: Long,
    val principalMinor: Long,
    val interestMinor: Long,
    val feeMinor: Long,
    val amountMinor: Long,
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    suspend fun active(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun byId(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeActiveList(): Flow<List<AccountEntity>>

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET isArchived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long = System.currentTimeMillis())

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM accounts")
    suspend fun maxSortOrder(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE isDeleted = 0 AND (accountId = :id OR counterAccountId = :id)")
    suspend fun transactionCount(id: Long): Int

    @Query("SELECT * FROM ledger_postings WHERE accountId = :accountId ORDER BY occurredAt DESC LIMIT :limit")
    fun observePostings(accountId: Long, limit: Int = 200): Flow<List<LedgerPostingEntity>>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isArchived ASC, sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    suspend fun active(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun byId(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories WHERE LOWER(name) = LOWER(:name) AND kind = :kind AND id != :excludeId")
    suspend fun countWithName(name: String, kind: String, excludeId: Long = -1): Int

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("UPDATE categories SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transactions SET categoryId = :toCategoryId WHERE categoryId = :fromCategoryId")
    suspend fun reassignTransactions(fromCategoryId: Long, toCategoryId: Long?)

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :id AND isDeleted = 0")
    suspend fun usageCount(id: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM categories")
    suspend fun maxSortOrder(): Int
}

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY isArchived ASC, name ASC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun byId(id: Long): PersonEntity?

    @Query("SELECT * FROM people WHERE id = :id")
    fun observeById(id: Long): Flow<PersonEntity?>

    @Insert
    suspend fun insert(person: PersonEntity): Long

    @Update
    suspend fun update(person: PersonEntity)

    @Query("UPDATE people SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)
}

@Dao
interface ShopDao {
    @Query("SELECT * FROM shops ORDER BY isArchived ASC, name ASC")
    fun observeAll(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE isArchived = 0 ORDER BY name ASC")
    suspend fun active(): List<ShopEntity>

    @Query("SELECT * FROM shops WHERE id = :id")
    suspend fun byId(id: Long): ShopEntity?

    @Query("SELECT * FROM shops WHERE id = :id")
    fun observeById(id: Long): Flow<ShopEntity?>

    @Insert
    suspend fun insert(shop: ShopEntity): Long

    @Update
    suspend fun update(shop: ShopEntity)

    @Query("UPDATE shops SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)
}

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Insert
    suspend fun insertPostings(postings: List<LedgerPostingEntity>)

    @Query("DELETE FROM ledger_postings WHERE transactionId = :transactionId")
    suspend fun deletePostings(transactionId: Long)

    @Query("SELECT * FROM ledger_postings WHERE transactionId = :transactionId")
    suspend fun postingsFor(transactionId: Long): List<LedgerPostingEntity>

    @Query("SELECT * FROM ledger_postings")
    suspend fun allPostings(): List<LedgerPostingEntity>

    @Query("SELECT * FROM ledger_postings WHERE accountId = :accountId ORDER BY occurredAt DESC LIMIT :limit")
    fun observeAccountPostings(accountId: Long, limit: Int): Flow<List<LedgerPostingEntity>>

    @Query("SELECT accountId, SUM(deltaMinor) AS balanceMinor FROM ledger_postings GROUP BY accountId")
    fun observeBalances(): Flow<List<AccountBalanceRow>>

    @Query("SELECT accountId, SUM(deltaMinor) AS balanceMinor FROM ledger_postings GROUP BY accountId")
    suspend fun balances(): List<AccountBalanceRow>

    @Query(
        """
        SELECT t.*, a.name AS accountName, ca.name AS counterAccountName, c.name AS categoryName,
               c.colorArgb AS categoryColor, s.name AS shopName, p.name AS personName
        FROM transactions t
        LEFT JOIN accounts a ON a.id = t.accountId
        LEFT JOIN accounts ca ON ca.id = t.counterAccountId
        LEFT JOIN categories c ON c.id = t.categoryId
        LEFT JOIN shops s ON s.id = t.shopId
        LEFT JOIN people p ON p.id = t.personId
        WHERE t.isDeleted = 0
        ORDER BY t.occurredAt DESC, t.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observeRecent(limit: Int, offset: Int = 0): Flow<List<TxListItem>>

    @Query(
        """
        SELECT t.*, a.name AS accountName, ca.name AS counterAccountName, c.name AS categoryName,
               c.colorArgb AS categoryColor, s.name AS shopName, p.name AS personName
        FROM transactions t
        LEFT JOIN accounts a ON a.id = t.accountId
        LEFT JOIN accounts ca ON ca.id = t.counterAccountId
        LEFT JOIN categories c ON c.id = t.categoryId
        LEFT JOIN shops s ON s.id = t.shopId
        LEFT JOIN people p ON p.id = t.personId
        WHERE t.isDeleted = 0
          AND (:from IS NULL OR t.businessDate >= :from)
          AND (:to IS NULL OR t.businessDate <= :to)
          AND (:noTypeFilter = 1 OR t.type IN (:types))
          AND (:categoryId IS NULL OR t.categoryId = :categoryId)
          AND (:accountId IS NULL OR t.accountId = :accountId OR t.counterAccountId = :accountId)
          AND (:shopId IS NULL OR t.shopId = :shopId)
          AND (:personId IS NULL OR t.personId = :personId)
          AND (:minAmount IS NULL OR t.amountMinor >= :minAmount)
          AND (:maxAmount IS NULL OR t.amountMinor <= :maxAmount)
          AND (:search IS NULL OR t.title LIKE :search OR t.note LIKE :search OR t.partyName LIKE :search
               OR s.name LIKE :search OR p.name LIKE :search OR c.name LIKE :search)
        ORDER BY
          CASE WHEN :sort = 'OLDEST' THEN t.occurredAt END ASC,
          CASE WHEN :sort = 'HIGHEST' THEN t.amountMinor END DESC,
          CASE WHEN :sort = 'LOWEST' THEN t.amountMinor END ASC,
          CASE WHEN :sort = 'NEWEST' THEN t.occurredAt END DESC,
          t.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observeFiltered(
        from: String?,
        to: String?,
        noTypeFilter: Int,
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
    ): Flow<List<TxListItem>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND businessDate >= :from AND businessDate <= :to ORDER BY occurredAt ASC")
    suspend fun between(from: String, to: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND businessDate >= :from AND businessDate <= :to ORDER BY occurredAt ASC")
    fun observeBetween(from: String, to: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY occurredAt ASC")
    suspend fun allActive(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND type IN (:types) AND businessDate >= :from AND businessDate <= :to")
    suspend fun byTypesBetween(types: List<TxType>, from: String, to: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND (accountId = :accountId OR counterAccountId = :accountId) ORDER BY occurredAt DESC LIMIT :limit")
    suspend fun forAccount(accountId: Long, limit: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND shopId = :shopId ORDER BY occurredAt DESC")
    suspend fun forShop(shopId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND personId = :personId ORDER BY occurredAt DESC")
    suspend fun forPerson(personId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND linkKind = :linkKind AND linkId = :linkId ORDER BY occurredAt ASC, id ASC")
    suspend fun forLink(linkKind: LinkKind, linkId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND linkKind = :linkKind AND linkId = :linkId ORDER BY occurredAt ASC, id ASC")
    fun observeForLink(linkKind: LinkKind, linkId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND linkKind = :linkKind ORDER BY occurredAt ASC")
    suspend fun allForLinkKind(linkKind: LinkKind): List<TransactionEntity>

    @Query("SELECT linkId, SUM(principalMinor) AS principalMinor, SUM(interestMinor) AS interestMinor, SUM(feeMinor) AS feeMinor, SUM(amountMinor) AS amountMinor FROM transactions WHERE isDeleted = 0 AND linkKind = :linkKind AND type = :type AND linkId IS NOT NULL GROUP BY linkId")
    fun observePaidByLink(linkKind: LinkKind, type: TxType): Flow<List<LinkPaidRow>>

    @Query("SELECT shopId, type, SUM(amountMinor) AS totalMinor FROM transactions WHERE isDeleted = 0 AND shopId IS NOT NULL GROUP BY shopId, type")
    fun observeShopTotals(): Flow<List<ShopTotalRow>>

    @Query("SELECT type, SUM(amountMinor) AS totalMinor, COUNT(*) AS itemCount FROM transactions WHERE isDeleted = 0 AND businessDate >= :from AND businessDate <= :to GROUP BY type")
    suspend fun totalsByType(from: String, to: String): List<TypeTotalRow>

    @Query("SELECT categoryId, type, SUM(amountMinor) AS totalMinor FROM transactions WHERE isDeleted = 0 AND businessDate >= :from AND businessDate <= :to AND categoryId IS NOT NULL GROUP BY categoryId, type")
    suspend fun categoryTotals(from: String, to: String): List<CategoryTotalRow>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE isDeleted = 0 AND businessDate = :date AND type = :type")
    suspend fun sumOnDate(date: String, type: TxType): Long

    @Query(
        """
        SELECT t.*, a.name AS accountName, ca.name AS counterAccountName, c.name AS categoryName,
               c.colorArgb AS categoryColor, s.name AS shopName, p.name AS personName
        FROM transactions t
        LEFT JOIN accounts a ON a.id = t.accountId
        LEFT JOIN accounts ca ON ca.id = t.counterAccountId
        LEFT JOIN categories c ON c.id = t.categoryId
        LEFT JOIN shops s ON s.id = t.shopId
        LEFT JOIN people p ON p.id = t.personId
        WHERE t.isDeleted = 0
        ORDER BY t.occurredAt DESC, t.id DESC
        LIMIT :limit
        """
    )
    suspend fun recent(limit: Int): List<TxListItem>

    @Query("SELECT COUNT(*) FROM transactions WHERE isDeleted = 0")
    suspend fun countActive(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE isDeleted = 0 AND type = :type AND amountMinor = :amountMinor AND businessDate = :businessDate AND accountId IS :accountId")
    suspend fun countSimilar(type: TxType, amountMinor: Long, businessDate: String, accountId: Long?): Int
}

@Dao
interface PurchaseDao {
    @Insert
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Insert
    suspend fun insertItems(items: List<PurchaseItemEntity>)

    @Query("SELECT * FROM purchases WHERE isDeleted = 0 ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun byId(id: Long): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE id = :id")
    fun observeById(id: Long): Flow<PurchaseEntity?>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId ORDER BY id ASC")
    fun observeItems(purchaseId: Long): Flow<List<PurchaseItemEntity>>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId ORDER BY id ASC")
    suspend fun items(purchaseId: Long): List<PurchaseItemEntity>

    @Query("SELECT * FROM purchases WHERE isDeleted = 0 AND shopId = :shopId ORDER BY occurredAt DESC")
    fun observeForShop(shopId: Long): Flow<List<PurchaseEntity>>

    @Query("UPDATE purchases SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT name FROM purchase_items GROUP BY name ORDER BY MAX(id) DESC LIMIT 30")
    suspend fun recentItemNames(): List<String>
}

@Dao
interface DebtDao {
    @Insert
    suspend fun insert(debt: DebtEntity): Long

    @Update
    suspend fun update(debt: DebtEntity)

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun byId(id: Long): DebtEntity?

    @Query("SELECT * FROM debts WHERE id = :id")
    fun observeById(id: Long): Flow<DebtEntity?>

    @Query("SELECT * FROM debts ORDER BY isClosed ASC, occurredAt DESC")
    fun observeAll(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE direction = :direction AND isClosed = 0")
    fun observeOpen(direction: DebtDirection): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE personId = :personId ORDER BY occurredAt DESC")
    fun observeForPerson(personId: Long): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE isClosed = 0")
    suspend fun open(): List<DebtEntity>

    @Query("UPDATE debts SET isClosed = :closed, updatedAt = :now WHERE id = :id")
    suspend fun setClosed(id: Long, closed: Boolean, now: Long = System.currentTimeMillis())
}

@Dao
interface LoanDao {
    @Insert
    suspend fun insert(loan: LoanEntity): Long

    @Update
    suspend fun update(loan: LoanEntity)

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun byId(id: Long): LoanEntity?

    @Query("SELECT * FROM loans WHERE id = :id")
    fun observeById(id: Long): Flow<LoanEntity?>

    @Query("SELECT * FROM loans ORDER BY isClosed ASC, disbursementDate DESC")
    fun observeAll(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE kind = :kind ORDER BY isClosed ASC, disbursementDate DESC")
    fun observeByKind(kind: LoanKind): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE isClosed = 0")
    suspend fun open(): List<LoanEntity>

    @Query("UPDATE loans SET isClosed = :closed, closedAt = :closedAt, updatedAt = :now WHERE id = :id")
    suspend fun setClosed(id: Long, closed: Boolean, closedAt: Long?, now: Long = System.currentTimeMillis())
}

@Dao
interface EmiDao {
    @Insert
    suspend fun insert(emi: EmiEntity): Long

    @Update
    suspend fun update(emi: EmiEntity)

    @Query("SELECT * FROM emi_purchases WHERE id = :id")
    suspend fun byId(id: Long): EmiEntity?

    @Query("SELECT * FROM emi_purchases WHERE id = :id")
    fun observeById(id: Long): Flow<EmiEntity?>

    @Query("SELECT * FROM emi_purchases ORDER BY isSettled ASC, purchaseDate DESC")
    fun observeAll(): Flow<List<EmiEntity>>

    @Query("SELECT * FROM emi_purchases WHERE isSettled = 0")
    suspend fun open(): List<EmiEntity>

    @Query("UPDATE emi_purchases SET isSettled = :settled, updatedAt = :now WHERE id = :id")
    suspend fun setSettled(id: Long, settled: Boolean, now: Long = System.currentTimeMillis())
}

@Dao
interface ScheduleDao {
    @Insert
    suspend fun insertAll(items: List<ScheduleItemEntity>)

    @Query("SELECT * FROM schedule_items WHERE ownerKind = :ownerKind AND ownerId = :ownerId ORDER BY sequence ASC")
    fun observe(ownerKind: ScheduleOwner, ownerId: Long): Flow<List<ScheduleItemEntity>>

    @Query("SELECT * FROM schedule_items WHERE ownerKind = :ownerKind AND ownerId = :ownerId ORDER BY sequence ASC")
    suspend fun forOwner(ownerKind: ScheduleOwner, ownerId: Long): List<ScheduleItemEntity>

    @Query("SELECT * FROM schedule_items ORDER BY dueDate ASC")
    fun observeAll(): Flow<List<ScheduleItemEntity>>

    @Query("SELECT * FROM schedule_items ORDER BY dueDate ASC")
    suspend fun all(): List<ScheduleItemEntity>

    @Query("SELECT * FROM schedule_items WHERE ownerKind = :ownerKind AND ownerId = :ownerId ORDER BY dueDate ASC LIMIT 1")
    suspend fun first(ownerKind: ScheduleOwner, ownerId: Long): ScheduleItemEntity?

    @Query("DELETE FROM schedule_items WHERE ownerKind = :ownerKind AND ownerId = :ownerId")
    suspend fun deleteForOwner(ownerKind: ScheduleOwner, ownerId: Long)

    @Query("SELECT COUNT(*) FROM schedule_items")
    suspend fun count(): Int
}

@Dao
interface SavingsDao {
    @Insert
    suspend fun insert(plan: SavingsPlanEntity): Long

    @Update
    suspend fun update(plan: SavingsPlanEntity)

    @Query("SELECT * FROM savings_plans WHERE id = :id")
    suspend fun byId(id: Long): SavingsPlanEntity?

    @Query("SELECT * FROM savings_plans WHERE id = :id")
    fun observeById(id: Long): Flow<SavingsPlanEntity?>

    @Query("SELECT * FROM savings_plans ORDER BY isArchived ASC, name ASC")
    fun observeAll(): Flow<List<SavingsPlanEntity>>

    @Query("UPDATE savings_plans SET isArchived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long = System.currentTimeMillis())
}

@Dao
interface BudgetDao {
    @Insert
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(refs: List<BudgetCategoryEntity>)

    @Query("DELETE FROM budget_categories WHERE budgetId = :budgetId")
    suspend fun deleteCategories(budgetId: Long)

    @Query("SELECT * FROM budget_categories WHERE budgetId = :budgetId")
    suspend fun categoriesFor(budgetId: Long): List<BudgetCategoryEntity>

    @Query("SELECT * FROM budgets ORDER BY isArchived ASC, startDate DESC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isArchived = 0 ORDER BY startDate DESC")
    fun observeActive(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun byId(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE id = :id")
    fun observeById(id: Long): Flow<BudgetEntity?>

    @Query("UPDATE budgets SET isArchived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long = System.currentTimeMillis())
}

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun byId(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders ORDER BY isDone ASC, dueDate ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isDone = 0 ORDER BY dueDate ASC")
    fun observeOpen(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isDone = 0 ORDER BY dueDate ASC")
    suspend fun open(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isDone = 0 AND dueDate <= :date ORDER BY dueDate ASC")
    suspend fun dueBy(date: String): List<ReminderEntity>

    @Query("UPDATE reminders SET isDone = :done, updatedAt = :now WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM reminders WHERE linkKind = :linkKind AND linkId = :linkId AND kind = :kind")
    suspend fun deleteLinked(linkKind: LinkKind, linkId: Long, kind: ReminderKind)

    @Query("SELECT * FROM reminders WHERE kind = :kind AND dueDate = :dueDate AND title = :title LIMIT 1")
    suspend fun findGenerated(kind: ReminderKind, dueDate: String, title: String): ReminderEntity?
}

@Dao
interface RecurringDao {
    @Insert
    suspend fun insert(rule: RecurringRuleEntity): Long

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun byId(id: Long): RecurringRuleEntity?

    @Query("SELECT * FROM recurring_rules ORDER BY isActive DESC, nextRunDate ASC")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE isActive = 1")
    suspend fun active(): List<RecurringRuleEntity>

    @Query("UPDATE recurring_rules SET isActive = :active, updatedAt = :now WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, now: Long = System.currentTimeMillis())
}

@Dao
interface AuditDao {
    @Insert
    suspend fun insert(entry: AuditLogEntity): Long

    @Query("SELECT * FROM audit_log ORDER BY at DESC LIMIT :limit")
    suspend fun recent(limit: Int = 200): List<AuditLogEntity>
}

@Dao
interface MaintenanceDao {
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM ledger_postings") suspend fun clearPostings()
    @Query("DELETE FROM purchases") suspend fun clearPurchases()
    @Query("DELETE FROM purchase_items") suspend fun clearPurchaseItems()
    @Query("DELETE FROM debts") suspend fun clearDebts()
    @Query("DELETE FROM loans") suspend fun clearLoans()
    @Query("DELETE FROM emi_purchases") suspend fun clearEmis()
    @Query("DELETE FROM schedule_items") suspend fun clearSchedule()
    @Query("DELETE FROM savings_plans") suspend fun clearSavings()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM budget_categories") suspend fun clearBudgetCategories()
    @Query("DELETE FROM reminders") suspend fun clearReminders()
    @Query("DELETE FROM recurring_rules") suspend fun clearRecurring()
    @Query("DELETE FROM audit_log") suspend fun clearAudit()
    @Query("DELETE FROM accounts") suspend fun clearAccounts()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM people") suspend fun clearPeople()
    @Query("DELETE FROM shops") suspend fun clearShops()
}

/** Raw access used only by backup export/restore, never by feature screens. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM accounts") suspend fun accounts(): List<AccountEntity>
    @Query("SELECT * FROM categories") suspend fun categories(): List<CategoryEntity>
    @Query("SELECT * FROM people") suspend fun people(): List<PersonEntity>
    @Query("SELECT * FROM shops") suspend fun shops(): List<ShopEntity>
    @Query("SELECT * FROM transactions") suspend fun transactions(): List<TransactionEntity>
    @Query("SELECT * FROM ledger_postings") suspend fun ledgerPostings(): List<LedgerPostingEntity>
    @Query("SELECT * FROM purchases") suspend fun purchases(): List<PurchaseEntity>
    @Query("SELECT * FROM purchase_items") suspend fun purchaseItems(): List<PurchaseItemEntity>
    @Query("SELECT * FROM debts") suspend fun debts(): List<DebtEntity>
    @Query("SELECT * FROM loans") suspend fun loans(): List<LoanEntity>
    @Query("SELECT * FROM emi_purchases") suspend fun emis(): List<EmiEntity>
    @Query("SELECT * FROM schedule_items") suspend fun scheduleItems(): List<ScheduleItemEntity>
    @Query("SELECT * FROM savings_plans") suspend fun savingsPlans(): List<SavingsPlanEntity>
    @Query("SELECT * FROM budgets") suspend fun budgets(): List<BudgetEntity>
    @Query("SELECT * FROM budget_categories") suspend fun budgetCategories(): List<BudgetCategoryEntity>
    @Query("SELECT * FROM reminders") suspend fun reminders(): List<ReminderEntity>
    @Query("SELECT * FROM recurring_rules") suspend fun recurringRules(): List<RecurringRuleEntity>
    @Query("SELECT * FROM audit_log") suspend fun auditLog(): List<AuditLogEntity>
}
