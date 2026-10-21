package com.shohankhan.hisaabos.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether the sensitive screens are currently unlocked.
 *
 * A cold start is always locked. Backgrounding beyond the configured timeout
 * locks again. State is deliberately in-memory only: process death always
 * requires the PIN.
 */
class SessionManager {

    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundedAtMillis: Long = 0L
    private var biometricPromptInFlight = false

    fun unlock() {
        _locked.value = false
        backgroundedAtMillis = 0L
    }

    fun lock() {
        _locked.value = true
    }

    fun onBackground(nowMillis: Long = System.currentTimeMillis()) {
        if (!_locked.value) backgroundedAtMillis = nowMillis
    }

    /** Returns true when the session should be locked again after returning to the foreground. */
    fun onForeground(autoLockSeconds: Int, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (_locked.value) return true
        if (autoLockSeconds <= 0) {
            lock()
            return true
        }
        if (backgroundedAtMillis > 0L && nowMillis - backgroundedAtMillis >= autoLockSeconds * 1000L) {
            lock()
            return true
        }
        return false
    }

    fun setBiometricPromptInFlight(inFlight: Boolean) {
        biometricPromptInFlight = inFlight
    }

    fun isBiometricPromptInFlight(): Boolean = biometricPromptInFlight
}
