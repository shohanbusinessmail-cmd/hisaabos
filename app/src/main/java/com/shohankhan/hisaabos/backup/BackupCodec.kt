package com.shohankhan.hisaabos.backup

import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encodes and decodes the backup file.
 *
 * Plain backups are readable JSON. Encrypted backups use AES-256-GCM with a key
 * derived from a user passphrase (PBKDF2-HMAC-SHA256, 120k iterations). The
 * passphrase never leaves the device and cannot be recovered by anyone.
 */
object BackupCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun encode(envelope: BackupEnvelope, passphrase: CharArray?): String {
        val plain = json.encodeToString(BackupEnvelope.serializer(), envelope)
        if (passphrase == null) return plain
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt, ITERATIONS), GCMParameterSpec(TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val wrapper = EncryptedBackupEnvelope(
            salt = Base64.getEncoder().encodeToString(salt),
            iv = Base64.getEncoder().encodeToString(iv),
            ciphertext = Base64.getEncoder().encodeToString(ciphertext),
            iterations = ITERATIONS,
        )
        return json.encodeToString(EncryptedBackupEnvelope.serializer(), wrapper)
    }

    fun decode(text: String, passphrase: CharArray?): BackupDecodeResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return BackupDecodeResult.Error("ফাইলটি খালি")
        val encrypted = runCatching {
            json.decodeFromString(EncryptedBackupEnvelope.serializer(), trimmed)
        }.getOrNull()
        if (encrypted != null && encrypted.format == EncryptedBackupEnvelope.FORMAT_ENCRYPTED) {
            if (passphrase == null) return BackupDecodeResult.NeedsPassphrase
            if (passphrase.isEmpty()) return BackupDecodeResult.Error("পাসওয়ার্ড দিন")
            return runCatching {
                val salt = Base64.getDecoder().decode(encrypted.salt)
                val iv = Base64.getDecoder().decode(encrypted.iv)
                val ciphertext = Base64.getDecoder().decode(encrypted.ciphertext)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt, encrypted.iterations), GCMParameterSpec(TAG_BITS, iv))
                val plain = cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
                val envelope = json.decodeFromString(BackupEnvelope.serializer(), plain)
                validate(envelope)
            }.getOrElse {
                BackupDecodeResult.Error("পাসওয়ার্ড ভুল অথবা ব্যাকআপ ফাইলটি ক্ষতিগ্রস্ত")
            }
        }
        return runCatching {
            val envelope = json.decodeFromString(BackupEnvelope.serializer(), trimmed)
            validate(envelope)
        }.getOrElse {
            BackupDecodeResult.Error("এটি বৈধ হিসাবওএস ব্যাকআপ ফাইল নয়")
        }
    }

    private fun validate(envelope: BackupEnvelope): BackupDecodeResult {
        if (envelope.format != BackupEnvelope.FORMAT) {
            return BackupDecodeResult.Error("অজানা ব্যাকআপ ফরম্যাট")
        }
        if (envelope.version <= 0 || envelope.version > BackupEnvelope.CURRENT_VERSION) {
            return BackupDecodeResult.Error("এই ব্যাকআপ নতুন অ্যাপ সংস্করণের; অনুগ্রহ করে অ্যাপ হালনাগাদ করুন")
        }
        val accountIds = envelope.accounts.map { it.id }.toSet()
        val categoryIds = envelope.categories.map { it.id }.toSet()
        val shopIds = envelope.shops.map { it.id }.toSet()
        val personIds = envelope.people.map { it.id }.toSet()
        val purchaseIds = envelope.purchases.map { it.id }.toSet()
        for (tx in envelope.transactions) {
            if (tx.accountId != null && tx.accountId !in accountIds) {
                return BackupDecodeResult.Error("ব্যাকআপে অসম্পূর্ণ লেনদেন পাওয়া গেছে (হিসাব অনুপস্থিত)")
            }
            if (tx.counterAccountId != null && tx.counterAccountId !in accountIds) {
                return BackupDecodeResult.Error("ব্যাকআপে অসম্পূর্ণ লেনদেন পাওয়া গেছে (হিসাব অনুপস্থিত)")
            }
            if (tx.categoryId != null && tx.categoryId !in categoryIds) {
                return BackupDecodeResult.Error("ব্যাকআপে অসম্পূর্ণ লেনদেন পাওয়া গেছে (ক্যাটাগরি অনুপস্থিত)")
            }
            if (tx.shopId != null && tx.shopId !in shopIds) {
                return BackupDecodeResult.Error("ব্যাকআপে অসম্পূর্ণ লেনদেন পাওয়া গেছে (দোকান অনুপস্থিত)")
            }
            if (tx.personId != null && tx.personId !in personIds) {
                return BackupDecodeResult.Error("ব্যাকআপে অসম্পূর্ণ লেনদেন পাওয়া গেছে (ব্যক্তি অনুপস্থিত)")
            }
            if (tx.amountMinor <= 0) return BackupDecodeResult.Error("ব্যাকআপে অমান্য লেনদেনের অঙ্ক পাওয়া গেছে")
        }
        for (item in envelope.purchaseItems) {
            if (item.purchaseId !in purchaseIds) {
                return BackupDecodeResult.Error("ব্যাকআপে অসম্পূর্ণ বাজার রেকর্ড পাওয়া গেছে")
            }
        }
        for (plan in envelope.savingsPlans) {
            if (plan.accountId !in accountIds) {
                return BackupDecodeResult.Error("সঞ্চয় হিসাবের তথ্য অসম্পূর্ণ")
            }
        }
        return BackupDecodeResult.Success(envelope)
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val ITERATIONS = 120_000
}
