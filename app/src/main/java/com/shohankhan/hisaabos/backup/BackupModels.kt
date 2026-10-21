package com.shohankhan.hisaabos.backup

import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.AuditLogEntity
import com.shohankhan.hisaabos.data.db.BudgetCategoryEntity
import com.shohankhan.hisaabos.data.db.BudgetEntity
import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.data.db.DebtEntity
import com.shohankhan.hisaabos.data.db.EmiEntity
import com.shohankhan.hisaabos.data.db.LedgerPostingEntity
import com.shohankhan.hisaabos.data.db.LoanEntity
import com.shohankhan.hisaabos.data.db.PersonEntity
import com.shohankhan.hisaabos.data.db.PurchaseEntity
import com.shohankhan.hisaabos.data.db.PurchaseItemEntity
import com.shohankhan.hisaabos.data.db.RecurringRuleEntity
import com.shohankhan.hisaabos.data.db.ReminderEntity
import com.shohankhan.hisaabos.data.db.SavingsPlanEntity
import com.shohankhan.hisaabos.data.db.ScheduleItemEntity
import com.shohankhan.hisaabos.data.db.ShopEntity
import com.shohankhan.hisaabos.data.db.TransactionEntity
import kotlinx.serialization.Serializable

/**
 * Versioned, self-describing backup. Contains every record needed to rebuild the
 * books, but deliberately **not** the PIN verifier, biometrics or any signing
 * material. Restoring therefore always asks the user to set a PIN again.
 */
@Serializable
data class BackupEnvelope(
    val format: String = FORMAT,
    val version: Int = CURRENT_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val appVersionName: String = "",
    val appVersionCode: Int = 0,
    val timeZone: String = "",
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
    val shops: List<ShopEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val ledgerPostings: List<LedgerPostingEntity> = emptyList(),
    val purchases: List<PurchaseEntity> = emptyList(),
    val purchaseItems: List<PurchaseItemEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val emis: List<EmiEntity> = emptyList(),
    val scheduleItems: List<ScheduleItemEntity> = emptyList(),
    val savingsPlans: List<SavingsPlanEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val budgetCategories: List<BudgetCategoryEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val recurringRules: List<RecurringRuleEntity> = emptyList(),
    val auditLog: List<AuditLogEntity> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
) {
    val counts: Map<String, Int>
        get() = mapOf(
            "accounts" to accounts.size,
            "categories" to categories.size,
            "people" to people.size,
            "shops" to shops.size,
            "transactions" to transactions.size,
            "purchases" to purchases.size,
            "debts" to debts.size,
            "loans" to loans.size,
            "emis" to emis.size,
            "savings" to savingsPlans.size,
            "budgets" to budgets.size,
            "reminders" to reminders.size,
        )

    companion object {
        const val FORMAT = "hisaabos-backup"
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class BackupSettings(
    val autoLockSeconds: Int = 60,
    val hideAmounts: Boolean = false,
    val remindersEnabled: Boolean = false,
    val showBanglaDigits: Boolean = true,
    val screenProtection: Boolean = true,
)

@Serializable
data class EncryptedBackupEnvelope(
    val format: String = FORMAT_ENCRYPTED,
    val version: Int = 1,
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iterations: Int = 120_000,
    val salt: String,
    val iv: String,
    val ciphertext: String,
) {
    companion object {
        const val FORMAT_ENCRYPTED = "hisaabos-backup-encrypted"
    }
}

sealed interface BackupDecodeResult {
    data class Success(val envelope: BackupEnvelope) : BackupDecodeResult
    data class Error(val message: String) : BackupDecodeResult
    data object NeedsPassphrase : BackupDecodeResult
}

sealed interface RestoreResult {
    data class Success(val imported: Map<String, Int>) : RestoreResult
    data class Invalid(val message: String) : RestoreResult
    data class Failed(val message: String) : RestoreResult
}
