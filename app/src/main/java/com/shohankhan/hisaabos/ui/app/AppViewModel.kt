package com.shohankhan.hisaabos.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.prefs.AppSettings
import com.shohankhan.hisaabos.data.prefs.AppPreferences
import com.shohankhan.hisaabos.data.prefs.PinDefaults
import com.shohankhan.hisaabos.security.PinManager
import com.shohankhan.hisaabos.security.SessionManager
import com.shohankhan.hisaabos.ui.text.Bn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

data class PinUiState(
    val error: String? = null,
    val busy: Boolean = false,
    val lockoutSeconds: Int = 0,
)

class AppViewModel(
    private val prefs: AppPreferences,
    private val session: SessionManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val locked: StateFlow<Boolean> = session.locked

    private val _pinState = MutableStateFlow(PinUiState())
    val pinState: StateFlow<PinUiState> = _pinState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.settings.collect { current ->
                if (current.lockoutUntilMillis > System.currentTimeMillis()) {
                    startLockoutCountdown(current.lockoutUntilMillis)
                }
            }
        }
    }

    private var countdownJob: kotlinx.coroutines.Job? = null

    private fun startLockoutCountdown(untilMillis: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val remaining = ((untilMillis - System.currentTimeMillis()) / 1000).toInt()
                if (remaining <= 0) {
                    _pinState.value = _pinState.value.copy(lockoutSeconds = 0)
                    break
                }
                _pinState.value = _pinState.value.copy(lockoutSeconds = remaining)
                delay(500)
            }
        }
    }

    fun clearPinError() {
        _pinState.value = _pinState.value.copy(error = null)
    }

    fun unlockWithPin(pin: String) {
        if (_pinState.value.busy) return
        viewModelScope.launch {
            val current = prefs.read()
            _pinState.value = _pinState.value.copy(busy = true, error = null)
            val cooldown = current.lockoutUntilMillis - System.currentTimeMillis()
            if (cooldown > 0) {
                startLockoutCountdown(current.lockoutUntilMillis)
                _pinState.value = PinUiState(error = "অনেকবার ভুল পিন দেওয়া হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন।")
                return@launch
            }
            val valid = withContext(Dispatchers.Default) {
                PinManager.verify(pin, current.pinSalt, current.pinHash, current.pinIterations)
            }
            if (valid) {
                prefs.clearFailedAttempts()
                session.unlock()
                _pinState.value = PinUiState()
            } else {
                val failures = current.failedAttempts + 1
                val lockUntil = if (failures % PinDefaults.MAX_ATTEMPTS == 0) {
                    System.currentTimeMillis() + PinManager.lockoutMillis(failures)
                } else {
                    0L
                }
                prefs.recordFailedAttempt(lockUntil)
                if (lockUntil > 0) startLockoutCountdown(lockUntil)
                _pinState.value = PinUiState(
                    error = Bn.pinIncorrect,
                    lockoutSeconds = if (lockUntil > 0) (lockUntil - System.currentTimeMillis()).toInt() / 1000 else 0,
                )
            }
        }
    }

    fun unlockWithBiometricSuccess() {
        viewModelScope.launch { prefs.clearFailedAttempts() }
        session.unlock()
        _pinState.value = PinUiState()
    }

    /** Creates or replaces the PIN. Used by onboarding and the security screen. */
    fun setPin(pin: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _pinState.value = _pinState.value.copy(busy = true, error = null)
            if (!PinManager.isValidPin(pin)) {
                _pinState.value = PinUiState(error = "৪ থেকে ৮ সংখ্যার পিন দিন")
                return@launch
            }
            val iterations = PinDefaults.ITERATIONS
            val salt = PinManager.newSalt()
            val hash = withContext(Dispatchers.Default) { PinManager.hashToBase64(pin, salt, iterations) }
            prefs.savePin(PinManager.saltToBase64(salt), hash, iterations)
            _pinState.value = PinUiState()
            onDone()
        }
    }

    fun changePin(currentPin: String, newPin: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val current = prefs.read()
            _pinState.value = _pinState.value.copy(busy = true, error = null)
            val valid = withContext(Dispatchers.Default) {
                PinManager.verify(currentPin, current.pinSalt, current.pinHash, current.pinIterations)
            }
            if (!valid) {
                _pinState.value = PinUiState(error = Bn.wrongCurrentPin)
                return@launch
            }
            if (!PinManager.isValidPin(newPin)) {
                _pinState.value = PinUiState(error = "৪ থেকে ৮ সংখ্যার পিন দিন")
                return@launch
            }
            val salt = PinManager.newSalt()
            val hash = withContext(Dispatchers.Default) { PinManager.hashToBase64(newPin, salt, PinDefaults.ITERATIONS) }
            prefs.savePin(PinManager.saltToBase64(salt), hash, PinDefaults.ITERATIONS)
            _pinState.value = PinUiState()
            onDone()
        }
    }

    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.completeOnboarding()
            onDone()
        }
    }

    fun lockNow() = session.lock()

    fun onBackground() = session.onBackground()

    fun onForeground(autoLockSeconds: Int) {
        session.onForeground(autoLockSeconds)
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { prefs.setBiometricEnabled(enabled) }
    }

    fun setAutoLockSeconds(seconds: Int) {
        viewModelScope.launch { prefs.setAutoLockSeconds(seconds) }
    }

    fun setHideAmounts(hidden: Boolean) {
        viewModelScope.launch { prefs.setHideAmounts(hidden) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setRemindersEnabled(enabled) }
    }

    fun setScreenProtection(enabled: Boolean) {
        viewModelScope.launch { prefs.setScreenProtection(enabled) }
    }

    fun setShowBanglaDigits(enabled: Boolean) {
        viewModelScope.launch { prefs.setShowBanglaDigits(enabled) }
    }

    /** Only called after an explicit, double-confirmed destructive action. */
    fun resetSecurityAfterWipe(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.clearSecurityState()
            session.lock()
            onDone()
        }
    }
}
