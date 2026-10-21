package com.shohankhan.hisaabos.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

/**
 * Optional biometric unlock.
 *
 * The prompt is gated by a hardware-backed Android Keystore key that requires
 * user authentication for every use. If the key is unavailable, invalidated or
 * the prompt is cancelled, the user simply falls back to the PIN. Biometrics are
 * never required and are disabled by default.
 */
object BiometricGate {

    private const val KEY_NAME = "hisaabos_biometric_gate"

    fun isAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val cipher = runCatching { createCipher() }.getOrNull()
        if (cipher == null) {
            onFailure("বায়োমেট্রিক এই মুহূর্তে ব্যবহার করা যাচ্ছে না")
            return
        }
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    runCatching { result.cryptoObject?.cipher?.doFinal() }
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure(null)
                }

                override fun onAuthenticationFailed() {
                    onFailure(null)
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("হিসাবওএস খুলুন")
            .setSubtitle("আপনার পরিচয় নিশ্চিত করুন")
            .setNegativeButtonText("পিন ব্যবহার করুন")
            .setConfirmationRequired(false)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    private fun createCipher(): Cipher {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key: SecretKey = (keyStore.getEntry(KEY_NAME, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: generateKey()
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }
}
