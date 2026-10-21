package com.shohankhan.hisaabos.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shohankhan.hisaabos.backup.BackupCodec
import com.shohankhan.hisaabos.backup.BackupDecodeResult
import com.shohankhan.hisaabos.backup.BackupEnvelope
import com.shohankhan.hisaabos.backup.BackupService
import com.shohankhan.hisaabos.backup.RestoreMode
import com.shohankhan.hisaabos.backup.RestoreResult
import com.shohankhan.hisaabos.data.db.HisaabDatabase
import com.shohankhan.hisaabos.data.prefs.AppPreferences
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.ledger.TxDraft
import com.shohankhan.hisaabos.domain.ledger.TransactionService
import com.shohankhan.hisaabos.domain.ledger.WriteResult
import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.domain.model.CategoryKind
import com.shohankhan.hisaabos.domain.model.TxType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestoreTest {

    private lateinit var db: HisaabDatabase
    private lateinit var repo: HisaabRepository
    private lateinit var service: TransactionService
    private lateinit var backup: BackupService
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, HisaabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        service = TransactionService(db)
        repo = HisaabRepository(db, service)
        backup = BackupService(db, AppPreferences(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun seed() = runBlocking {
        val cash = repo.createAccount("নগদ", AccountType.CASH)
        val bank = repo.createAccount("ব্যাংক", AccountType.BANK)
        val food = repo.createCategory("খাবার", CategoryKind.EXPENSE)
        val shop = repo.createShop("করিম স্টোর")
        service.post(TxDraft(TxType.OPENING_BALANCE, 500_000, accountId = cash, businessDate = "2026-10-01"))
        service.post(TxDraft(TxType.EXPENSE, 12_000, accountId = cash, categoryId = food, shopId = shop, title = "দুপুরের খাবার"))
        service.transfer(cash, bank, 40_000, 1_760_000_000_000, "2026-10-21", "সঞ্চয়")
        cash to bank
    }

    @Test
    fun `full backup can be restored after a wipe`() = runBlocking {
        val accounts = seed()
        val countsBefore = backup.export(passphrase = null)
        val envelope = (BackupCodec.decode(countsBefore, null) as BackupDecodeResult.Success).envelope
        assertEquals(2, envelope.accounts.size)
        assertEquals(3, envelope.transactions.size)

        // Wipe everything through a replace restore of an empty book.
        val wiped = backup.restore(BackupEnvelope(appVersionName = "test"), RestoreMode.REPLACE_ALL)
        assertTrue(wiped is RestoreResult.Success)
        assertEquals(0, db.transactionDao().countActive())

        val restored = backup.restore(envelope, RestoreMode.REPLACE_ALL)
        assertTrue("restore failed: $restored", restored is RestoreResult.Success)
        assertEquals(3, db.transactionDao().countActive())
        val balances = repo.balances()
        assertEquals(448_000L, balances[accounts.first])
        assertEquals(40_000L, balances[accounts.second])
        assertEquals(500_000L, balances.values.sum())
        assertTrue(service.verifyIntegrity().isEmpty())
    }

    @Test
    fun `encrypted backup round-trips with the right passphrase`() = runBlocking {
        seed()
        val passphrase = "সোনার সংসার ২০২৬".toCharArray()
        val text = backup.export(passphrase)
        assertTrue(text.trimStart().startsWith("{"))
        val decoded = BackupCodec.decode(text, passphrase) as BackupDecodeResult.Success
        val result = backup.restore(decoded.envelope, RestoreMode.REPLACE_ALL)
        assertTrue(result is RestoreResult.Success)
        assertEquals(3, db.transactionDao().countActive())
    }

    @Test
    fun `merge import keeps existing books and adds new ones`() = runBlocking {
        val accounts = seed()
        val firstExport = backup.export(null)
        val envelope = (BackupCodec.decode(firstExport, null) as BackupDecodeResult.Success).envelope
        val merged = backup.restore(envelope, RestoreMode.MERGE)
        assertTrue("merge failed: $merged", merged is RestoreResult.Success)
        // Same master records and transactions must not be duplicated.
        assertEquals(2, db.accountDao().active().size)
        assertEquals(3, db.transactionDao().countActive())
        assertEquals(448_000L, repo.balances()[accounts.first])
    }

    @Test
    fun `a restore that fails midway leaves the previous data intact`() = runBlocking {
        val accounts = seed()
        val envelope = (BackupCodec.decode(backup.export(null), null) as BackupDecodeResult.Success).envelope
        val broken = envelope.copy(
            transactions = envelope.transactions + envelope.transactions.first().copy(
                id = 999_999,
                accountId = 999_999,
                amountMinor = 100,
                businessDate = "2026-10-22",
            ),
        )
        val result = backup.restore(broken, RestoreMode.REPLACE_ALL)
        assertTrue(result is RestoreResult.Failed)
        assertEquals(3, db.transactionDao().countActive())
        assertEquals(448_000L, repo.balances()[accounts.first])
    }

    @Test
    fun `backup does not contain the pin verifier`() = runBlocking {
        seed()
        val text = backup.export(null)
        val envelope = (BackupCodec.decode(text, null) as BackupDecodeResult.Success).envelope
        assertEquals(BackupEnvelope.CURRENT_VERSION, envelope.version)
        assertEquals("hisaabos-backup", envelope.format)
        assertTrue(!text.contains("pinHash"))
        assertTrue(!text.contains("pinSalt"))
        assertTrue(envelope.settings.let { it.autoLockSeconds >= 0 })
        val counts = db.transactionDao().observeRecent(10).first()
        assertEquals(3, counts.size)
    }
}
