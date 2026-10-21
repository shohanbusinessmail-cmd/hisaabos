package com.shohankhan.hisaabos.backup

import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.ShopEntity
import com.shohankhan.hisaabos.data.db.TransactionEntity
import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.domain.model.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private fun envelope(): BackupEnvelope {
        val account = AccountEntity(id = 1, name = "নগদ", type = AccountType.CASH)
        val shop = ShopEntity(id = 1, name = "করিম স্টোর")
        return BackupEnvelope(
            accounts = listOf(account),
            shops = listOf(shop),
            transactions = listOf(
                TransactionEntity(
                    id = 1,
                    type = TxType.EXPENSE,
                    amountMinor = 50_000,
                    occurredAt = 1_700_000_000_000,
                    businessDate = "2026-05-01",
                    accountId = 1,
                    shopId = 1,
                ),
            ),
        )
    }

    @Test
    fun `plain backup round-trips unchanged`() {
        val text = BackupCodec.encode(envelope(), passphrase = null)
        assertTrue(text.contains("hisaabos-backup"))
        val decoded = BackupCodec.decode(text, passphrase = null)
        assertTrue(decoded is BackupDecodeResult.Success)
        val restored = (decoded as BackupDecodeResult.Success).envelope
        assertEquals(1, restored.accounts.size)
        assertEquals(50_000L, restored.transactions.single().amountMinor)
        assertEquals("করিম স্টোর", restored.shops.single().name)
    }

    @Test
    fun `encrypted backup hides content and needs the passphrase`() {
        val passphrase = "সঠিক-পাসওয়ার্ড".toCharArray()
        val text = BackupCodec.encode(envelope(), passphrase)
        assertTrue(text.contains("hisaabos-backup-encrypted"))
        assertTrue(!text.contains("করিম স্টোর"))
        assertEquals(BackupDecodeResult.NeedsPassphrase, BackupCodec.decode(text, null))
        val wrong = BackupCodec.decode(text, "ভুল".toCharArray())
        assertTrue(wrong is BackupDecodeResult.Error)
        val ok = BackupCodec.decode(text, passphrase)
        assertTrue(ok is BackupDecodeResult.Success)
        assertEquals(50_000L, (ok as BackupDecodeResult.Success).envelope.transactions.single().amountMinor)
    }

    @Test
    fun `backup with a dangling reference is rejected`() {
        val broken = envelope().copy(
            transactions = envelope().transactions.map { it.copy(accountId = 99) },
        )
        val text = BackupCodec.encode(broken, passphrase = null)
        val result = BackupCodec.decode(text, passphrase = null)
        assertTrue(result is BackupDecodeResult.Error)
    }

    @Test
    fun `future backup versions are refused instead of half imported`() {
        val future = envelope().copy(version = 99)
        val result = BackupCodec.decode(BackupCodec.encode(future, null), null)
        assertTrue(result is BackupDecodeResult.Error)
        assertTrue((result as BackupDecodeResult.Error).message.contains("সংস্করণ"))
    }

    @Test
    fun `foreign files are refused`() {
        assertTrue(BackupCodec.decode("", null) is BackupDecodeResult.Error)
        assertTrue(BackupCodec.decode("{\"hello\":1}", null) is BackupDecodeResult.Error)
        assertTrue(BackupCodec.decode("not json at all", null) is BackupDecodeResult.Error)
    }

    @Test
    fun `non positive amounts are refused`() {
        val invalid = envelope().copy(
            transactions = envelope().transactions.map { it.copy(amountMinor = 0) },
        )
        assertTrue(BackupCodec.decode(BackupCodec.encode(invalid, null), null) is BackupDecodeResult.Error)
    }
}
