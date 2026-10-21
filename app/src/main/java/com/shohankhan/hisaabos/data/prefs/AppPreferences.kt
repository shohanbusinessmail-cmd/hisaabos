package com.shohankhan.hisaabos.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Only non-financial UI/security preferences live here. Money always lives in Room. */
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val pinSet: Boolean = false,
    val pinSalt: String = "",
    val pinHash: String = "",
    val pinIterations: Int = PinDefaults.ITERATIONS,
    val biometricEnabled: Boolean = false,
    val autoLockSeconds: Int = 60,
    val hideAmounts: Boolean = false,
    val remindersEnabled: Boolean = false,
    val screenProtection: Boolean = true,
    val failedAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0,
    val lastBackupAtMillis: Long = 0,
    val lastExportAtMillis: Long = 0,
    val showBanglaDigits: Boolean = true,
    val startupTab: Int = 0,
)

object PinDefaults {
    /** PBKDF2-HMAC-SHA256 iterations. Stored per-install so it can be raised later. */
    const val ITERATIONS = 210_000
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8
    const val MAX_ATTEMPTS = 5
    const val BASE_LOCKOUT_SECONDS = 30
}

private val Context.hisaabDataStore: DataStore<Preferences> by preferencesDataStore(name = "hisaabos_prefs")

class AppPreferences(private val context: Context) {

    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val pinSalt = stringPreferencesKey("pin_salt")
        val pinHash = stringPreferencesKey("pin_hash")
        val pinIterations = intPreferencesKey("pin_iterations")
        val biometricEnabled = booleanPreferencesKey("biometric_enabled")
        val autoLockSeconds = intPreferencesKey("auto_lock_seconds")
        val hideAmounts = booleanPreferencesKey("hide_amounts")
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val screenProtection = booleanPreferencesKey("screen_protection")
        val failedAttempts = intPreferencesKey("failed_attempts")
        val lockoutUntil = longPreferencesKey("lockout_until")
        val lastBackupAt = longPreferencesKey("last_backup_at")
        val lastExportAt = longPreferencesKey("last_export_at")
        val showBanglaDigits = booleanPreferencesKey("show_bangla_digits")
        val startupTab = intPreferencesKey("startup_tab")
    }

    val settings: Flow<AppSettings> = context.hisaabDataStore.data.map { prefs ->
        AppSettings(
            onboardingCompleted = prefs[Keys.onboardingCompleted] ?: false,
            pinSet = !prefs[Keys.pinHash].isNullOrBlank(),
            pinSalt = prefs[Keys.pinSalt] ?: "",
            pinHash = prefs[Keys.pinHash] ?: "",
            pinIterations = prefs[Keys.pinIterations] ?: PinDefaults.ITERATIONS,
            biometricEnabled = prefs[Keys.biometricEnabled] ?: false,
            autoLockSeconds = prefs[Keys.autoLockSeconds] ?: 60,
            hideAmounts = prefs[Keys.hideAmounts] ?: false,
            remindersEnabled = prefs[Keys.remindersEnabled] ?: false,
            screenProtection = prefs[Keys.screenProtection] ?: true,
            failedAttempts = prefs[Keys.failedAttempts] ?: 0,
            lockoutUntilMillis = prefs[Keys.lockoutUntil] ?: 0,
            lastBackupAtMillis = prefs[Keys.lastBackupAt] ?: 0,
            lastExportAtMillis = prefs[Keys.lastExportAt] ?: 0,
            showBanglaDigits = prefs[Keys.showBanglaDigits] ?: true,
            startupTab = prefs[Keys.startupTab] ?: 0,
        )
    }

    suspend fun completeOnboarding() = edit { it[Keys.onboardingCompleted] = true }

    suspend fun savePin(saltBase64: String, hashBase64: String, iterations: Int) = edit {
        it[Keys.pinSalt] = saltBase64
        it[Keys.pinHash] = hashBase64
        it[Keys.pinIterations] = iterations
        it[Keys.failedAttempts] = 0
        it[Keys.lockoutUntil] = 0
    }

    suspend fun setBiometricEnabled(enabled: Boolean) = edit { it[Keys.biometricEnabled] = enabled }

    suspend fun setAutoLockSeconds(seconds: Int) = edit { it[Keys.autoLockSeconds] = seconds }

    suspend fun setHideAmounts(hidden: Boolean) = edit { it[Keys.hideAmounts] = hidden }

    suspend fun setRemindersEnabled(enabled: Boolean) = edit { it[Keys.remindersEnabled] = enabled }

    suspend fun setScreenProtection(enabled: Boolean) = edit { it[Keys.screenProtection] = enabled }

    suspend fun setShowBanglaDigits(enabled: Boolean) = edit { it[Keys.showBanglaDigits] = enabled }

    suspend fun setStartupTab(index: Int) = edit { it[Keys.startupTab] = index }

    suspend fun recordFailedAttempt(lockoutUntilMillis: Long) = edit {
        it[Keys.failedAttempts] = (it[Keys.failedAttempts] ?: 0) + 1
        it[Keys.lockoutUntil] = lockoutUntilMillis
    }

    suspend fun clearFailedAttempts() = edit {
        it[Keys.failedAttempts] = 0
        it[Keys.lockoutUntil] = 0
    }

    suspend fun markBackupDone(atMillis: Long) = edit { it[Keys.lastBackupAt] = atMillis }

    suspend fun markExportDone(atMillis: Long) = edit { it[Keys.lastExportAt] = atMillis }

    /** Removes the PIN only as part of a full local reset that wipes financial data. */
    suspend fun clearSecurityState() = edit {
        it.remove(Keys.pinHash)
        it.remove(Keys.pinSalt)
        it.remove(Keys.pinIterations)
        it.remove(Keys.biometricEnabled)
        it.remove(Keys.failedAttempts)
        it.remove(Keys.lockoutUntil)
        it.remove(Keys.onboardingCompleted)
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.hisaabDataStore.edit { block(it) }
    }
}
