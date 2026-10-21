package com.shohankhan.hisaabos.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.domain.model.BudgetPeriod
import com.shohankhan.hisaabos.domain.model.CategoryKind
import com.shohankhan.hisaabos.domain.model.DebtDirection
import com.shohankhan.hisaabos.domain.model.InterestModel
import com.shohankhan.hisaabos.domain.model.LedgerEntry
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.LoanKind
import com.shohankhan.hisaabos.domain.model.PartyKind
import com.shohankhan.hisaabos.domain.model.RecurrenceFrequency
import com.shohankhan.hisaabos.domain.model.ReminderKind
import com.shohankhan.hisaabos.domain.model.RepaymentFrequency
import com.shohankhan.hisaabos.domain.model.SavingsKind
import com.shohankhan.hisaabos.domain.model.ScheduleOwner
import com.shohankhan.hisaabos.domain.model.TxType
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Master data
// ---------------------------------------------------------------------------

@Entity(
    tableName = "accounts",
    indices = [Index("isArchived"), Index("sortOrder")],
)
@Serializable
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.CASH,
    val institution: String? = null,
    val notes: String? = null,
    val isLiability: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val iconKey: String = "wallet",
    @ColumnInfo(defaultValue = "4278220") val colorArgb: Long = 0xFF087443,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "kind"], unique = true), Index("isArchived")],
)
@Serializable
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: CategoryKind = CategoryKind.EXPENSE,
    val iconKey: String = "tag",
    val colorArgb: Long = 0xFF087443,
    val description: String? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "people", indices = [Index("name")])
@Serializable
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "shops", indices = [Index("name"), Index("isArchived")])
@Serializable
data class ShopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val shopkeeperName: String? = null,
    val personId: Long? = null,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    /** Payable carried forward from before HisaabOS was installed. Never income. */
    val openingBalanceMinor: Long = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

// ---------------------------------------------------------------------------
// The ledger
// ---------------------------------------------------------------------------

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(AccountEntity::class, ["id"], ["counterAccountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(ShopEntity::class, ["id"], ["shopId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(PersonEntity::class, ["id"], ["personId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index("occurredAt"),
        Index("businessDate"),
        Index("type"),
        Index("accountId"),
        Index("counterAccountId"),
        Index("categoryId"),
        Index("shopId"),
        Index("personId"),
        Index("linkKind", "linkId"),
        Index("isDeleted"),
    ],
)
@Serializable
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    override val type: TxType,
    override val amountMinor: Long,
    override val flowSign: Int = 1,
    override val accountId: Long? = null,
    override val counterAccountId: Long? = null,
    override val interestMinor: Long = 0,
    override val feeMinor: Long = 0,
    /** Principal component of a settlement (amount − interest − fee). Never a separate total. */
    val principalMinor: Long = 0,
    val occurredAt: Long,
    val businessDate: String,
    val categoryId: Long? = null,
    val shopId: Long? = null,
    val personId: Long? = null,
    val partyKind: PartyKind = PartyKind.NONE,
    /** Free-typed counterparty label (institution, merchant) when no master record is used. */
    val partyName: String? = null,
    val linkKind: LinkKind = LinkKind.NONE,
    val linkId: Long? = null,
    val installmentIndex: Int? = null,
    val title: String? = null,
    val note: String? = null,
    val tags: String? = null,
    val attachmentUri: String? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val reversesTransactionId: Long? = null,
    val groupId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) : LedgerEntry

/**
 * Double-entry posting derived from a transaction by [com.shohankhan.hisaabos.domain.model.LedgerMath].
 * Account balances are always `SUM(delta_minor)` over this table, so they can be
 * reconstructed and independently verified at any time.
 */
@Entity(
    tableName = "ledger_postings",
    foreignKeys = [
        ForeignKey(TransactionEntity::class, ["id"], ["transactionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("transactionId"), Index("accountId"), Index("businessDate")],
)
@Serializable
data class LedgerPostingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val accountId: Long,
    val deltaMinor: Long,
    val businessDate: String,
    val occurredAt: Long,
)

// ---------------------------------------------------------------------------
// Market purchases
// ---------------------------------------------------------------------------

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(ShopEntity::class, ["id"], ["shopId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(PersonEntity::class, ["id"], ["personId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("occurredAt"), Index("businessDate"), Index("shopId")],
)
@Serializable
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long? = null,
    val personId: Long? = null,
    val occurredAt: Long,
    val businessDate: String,
    val subtotalMinor: Long,
    val discountMinor: Long = 0,
    val extraChargeMinor: Long = 0,
    val totalMinor: Long,
    val paidMinor: Long = 0,
    val creditMinor: Long = 0,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val note: String? = null,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "purchase_items",
    foreignKeys = [ForeignKey(PurchaseEntity::class, ["id"], ["purchaseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("purchaseId"), Index("name")],
)
@Serializable
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val name: String,
    /** Quantity in thousandths so 1.5 kg = 1500 without floating point. */
    val quantityMilli: Long = 1000,
    val unit: String = "পিস",
    val unitPriceMinor: Long,
    val lineTotalMinor: Long,
)

// ---------------------------------------------------------------------------
// Personal borrowing / lending
// ---------------------------------------------------------------------------

@Entity(
    tableName = "debts",
    foreignKeys = [
        ForeignKey(PersonEntity::class, ["id"], ["personId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("personId"), Index("direction")],
)
@Serializable
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val direction: DebtDirection,
    val personId: Long,
    val principalMinor: Long,
    val interestMinor: Long = 0,
    val occurredAt: Long,
    val businessDate: String,
    val dueDate: String? = null,
    val accountId: Long? = null,
    val note: String? = null,
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

// ---------------------------------------------------------------------------
// NGO / bank loans and EMI purchases
// ---------------------------------------------------------------------------

@Entity(
    tableName = "loans",
    foreignKeys = [ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("kind"), Index("isClosed")],
)
@Serializable
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: LoanKind,
    val institution: String,
    val branch: String? = null,
    val reference: String? = null,
    val principalMinor: Long,
    val interestMinor: Long = 0,
    val feeMinor: Long = 0,
    val totalRepayableMinor: Long? = null,
    val model: InterestModel = InterestModel.MANUAL,
    val annualRateBp: Int = 0,
    val frequency: RepaymentFrequency = RepaymentFrequency.MONTHLY,
    val installmentCount: Int = 0,
    val installmentAmountMinor: Long = 0,
    val firstDueDate: String? = null,
    val disbursementDate: String,
    val accountId: Long? = null,
    val notes: String? = null,
    val isClosed: Boolean = false,
    val closedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "emi_purchases",
    foreignKeys = [
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("isSettled")],
)
@Serializable
data class EmiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productName: String,
    val merchant: String? = null,
    val purchaseDate: String,
    val cashPriceMinor: Long? = null,
    val totalFinancedPriceMinor: Long,
    val downPaymentMinor: Long = 0,
    val financedMinor: Long,
    val interestMinor: Long = 0,
    val feeMinor: Long = 0,
    val frequency: RepaymentFrequency = RepaymentFrequency.MONTHLY,
    val installmentAmountMinor: Long = 0,
    val installmentCount: Int,
    val firstDueDate: String,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val notes: String? = null,
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "schedule_items",
    indices = [Index("ownerKind", "ownerId"), Index("dueDate")],
)
@Serializable
data class ScheduleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerKind: ScheduleOwner,
    val ownerId: Long,
    val sequence: Int,
    val dueDate: String,
    val principalMinor: Long = 0,
    val interestMinor: Long = 0,
    val feeMinor: Long = 0,
    val amountMinor: Long,
)

// ---------------------------------------------------------------------------
// Savings
// ---------------------------------------------------------------------------

@Entity(
    tableName = "savings_plans",
    foreignKeys = [
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("isArchived")],
)
@Serializable
data class SavingsPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val institution: String? = null,
    val kind: SavingsKind = SavingsKind.CASH,
    /** Owned asset account that holds this savings balance (no separate balance is stored). */
    val accountId: Long,
    val targetAmountMinor: Long? = null,
    val targetDate: String? = null,
    val contributionAmountMinor: Long? = null,
    val contributionFrequency: RecurrenceFrequency? = null,
    val note: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

// ---------------------------------------------------------------------------
// Budgets, recurring rules, reminders, audit
// ---------------------------------------------------------------------------

@Entity(tableName = "budgets", indices = [Index("isArchived")])
@Serializable
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: String,
    val endDate: String? = null,
    val limitMinor: Long,
    val includeAllCategories: Boolean = false,
    val note: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "budget_categories",
    primaryKeys = ["budgetId", "categoryId"],
    foreignKeys = [
        ForeignKey(BudgetEntity::class, ["id"], ["budgetId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("categoryId")],
)
@Serializable
data class BudgetCategoryEntity(
    val budgetId: Long,
    val categoryId: Long,
)

@Entity(tableName = "recurring_rules", indices = [Index("isActive")])
@Serializable
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: TxType,
    val amountMinor: Long,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val personId: Long? = null,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val nextRunDate: String,
    val note: String? = null,
    val isActive: Boolean = true,
    val lastRunAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "reminders", indices = [Index("dueDate"), Index("isDone")])
@Serializable
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDate: String,
    val amountMinor: Long? = null,
    val kind: ReminderKind = ReminderKind.CUSTOM,
    val linkKind: LinkKind = LinkKind.NONE,
    val linkId: Long? = null,
    val note: String? = null,
    val isDone: Boolean = false,
    val notifyDaysBefore: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "audit_log", indices = [Index("entityTable", "entityId")])
@Serializable
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long = System.currentTimeMillis(),
    val action: String,
    val entityTable: String,
    val entityId: Long,
    /** JSON snapshot of the row before the change (never shown raw in the UI). */
    val snapshotJson: String? = null,
)
