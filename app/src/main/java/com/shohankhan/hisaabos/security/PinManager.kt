package com.shohankhan.hisaabos.security

import com.shohankhan.hisaabos.data.prefs.PinDefaults
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN verification with a salted PBKDF2-HMAC-SHA256 verifier.
 *
 * The PIN itself is never stored, logged or sent anywhere. Verification uses
 * constant-time comparison, so an attacker cannot learn a prefix of the PIN from
 * response timing. The hash is bound to this installation by its random salt.
 */
object PinManager {

    const val SALT_BYTES = 16
    const val KEY_BITS = 256

    fun normalize(pin: String): String =
        com.shohankhan.hisaabos.domain.model.Money.toLatinDigits(pin).filter { it.isDigit() }

    fun isValidPin(pin: String): Boolean {
        val normalized = normalize(pin)
        return normalized.length in PinDefaults.MIN_LENGTH..PinDefaults.MAX_LENGTH
    }

    fun newSalt(): ByteArray {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun saltToBase64(salt: ByteArray): String = Base64.getEncoder().encodeToString(salt)

    fun saltFromBase64(encoded: String): ByteArray? =
        runCatching { Base64.getDecoder().decode(encoded) }.getOrNull()

    fun hash(pin: String, salt: ByteArray, iterations: Int = PinDefaults.ITERATIONS): ByteArray {
        val normalized = normalize(pin)
        val spec = PBEKeySpec(normalized.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun hashToBase64(pin: String, salt: ByteArray, iterations: Int = PinDefaults.ITERATIONS): String =
        Base64.getEncoder().encodeToString(hash(pin, salt, iterations))

    fun verify(pin: String, saltBase64: String, expectedHashBase64: String, iterations: Int): Boolean {
        val salt = saltFromBase64(saltBase64) ?: return false
        val expected = runCatching { Base64.getDecoder().decode(expectedHashBase64) }.getOrNull() ?: return false
        val actual = hash(pin, salt, iterations)
        return MessageDigest.isEqual(expected, actual)
    }

    /** Escalating cooldown after repeated wrong PINs, persisted across restarts. */
    fun lockoutMillis(failedAttempts: Int): Long {
        val extra = (failedAttempts / PinDefaults.MAX_ATTEMPTS) - 1
        val seconds = PinDefaults.BASE_LOCKOUT_SECONDS * (1L shl extra.coerceIn(0, 4))
        return seconds * 1000L
    }
}
