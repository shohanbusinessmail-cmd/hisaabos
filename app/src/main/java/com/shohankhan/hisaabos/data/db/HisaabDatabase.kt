package com.shohankhan.hisaabos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        PersonEntity::class,
        ShopEntity::class,
        TransactionEntity::class,
        LedgerPostingEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        DebtEntity::class,
        LoanEntity::class,
        EmiEntity::class,
        ScheduleItemEntity::class,
        SavingsPlanEntity::class,
        BudgetEntity::class,
        BudgetCategoryEntity::class,
        RecurringRuleEntity::class,
        ReminderEntity::class,
        AuditLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class HisaabDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun personDao(): PersonDao
    abstract fun shopDao(): ShopDao
    abstract fun transactionDao(): TransactionDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun debtDao(): DebtDao
    abstract fun loanDao(): LoanDao
    abstract fun emiDao(): EmiDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun savingsDao(): SavingsDao
    abstract fun budgetDao(): BudgetDao
    abstract fun reminderDao(): ReminderDao
    abstract fun recurringDao(): RecurringDao
    abstract fun auditDao(): AuditDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val NAME = "hisaabos.db"

        /**
         * No destructive fallback anywhere: financial history must survive.
         * Future schema changes must ship explicit migrations.
         */
        fun build(context: Context): HisaabDatabase =
            Room.databaseBuilder(context, HisaabDatabase::class.java, NAME)
                .build()
    }
}
