package com.shohankhan.hisaabos.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.HisaabDatabase
import com.shohankhan.hisaabos.data.db.ShopEntity
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.ledger.PurchaseInput
import com.shohankhan.hisaabos.domain.ledger.PurchaseItemInput
import com.shohankhan.hisaabos.domain.ledger.TxDraft
import com.shohankhan.hisaabos.domain.ledger.TransactionService
import com.shohankhan.hisaabos.domain.ledger.WriteResult
import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.domain.model.TxType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LedgerDatabaseTest {

    private lateinit var db: HisaabDatabase
    private lateinit var repo: HisaabRepository
    private lateinit var service: TransactionService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HisaabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        service = TransactionService(db)
        repo = HisaabRepository(db, service)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun balances() = runBlocking { repo.balances() }

    private suspend fun account(name: String, type: AccountType = AccountType.CASH): Long =
        repo.createAccount(name = name, type = type)

    @Test
    fun `income and expense update the ledger from postings only`() = runBlocking {
        val cash = account("নগদ")
        assertTrue(service.post(TxDraft(TxType.INCOME, 100_000, accountId = cash, title = "বেতন")) is WriteResult.Success)
        assertTrue(service.post(TxDraft(TxType.EXPENSE, 25_000, accountId = cash, title = "বাজার")) is WriteResult.Success)
        assertEquals(75_000L, balances()[cash])
        val postings = db.transactionDao().allPostings().filter { it.accountId == cash }
        assertEquals(2, postings.size)
        assertEquals(75_000L, postings.sumOf { it.deltaMinor })
    }

    @Test
    fun `transfer moves money without changing total or split income`() = runBlocking {
        val cash = account("নগদ")
        val bank = account("ব্যাংক", AccountType.BANK)
        service.post(TxDraft(TxType.INCOME, 200_000, accountId = cash))
        val result = service.transfer(
            fromAccountId = cash,
            toAccountId = bank,
            amountMinor = 80_000,
            occurredAt = 1_760_000_000_000,
            businessDate = "2026-10-21",
            note = "ব্যাংকে জমা",
        )
        assertTrue(result is WriteResult.Success)
        val map = balances()
        assertEquals(120_000L, map[cash])
        assertEquals(80_000L, map[bank])
        assertEquals(200_000L, map.values.sum())
        val tx = db.transactionDao().allActive().first { it.type == TxType.TRANSFER }
        assertEquals(80_000L, tx.amountMinor)
        assertEquals(2, db.transactionDao().postingsFor(tx.id).size)
    }

    @Test
    fun `invalid draft writes nothing at all`() = runBlocking {
        val before = db.transactionDao().countActive()
        val result = service.post(TxDraft(TxType.EXPENSE, 500_000, accountId = null))
        assertTrue(result is WriteResult.Invalid)
        assertEquals(before, db.transactionDao().countActive())
        assertTrue(db.transactionDao().allPostings().isEmpty())
    }

    @Test
    fun `deleting a transaction removes its postings and restores the balance`() = runBlocking {
        val cash = account("নগদ")
        val posted = service.post(TxDraft(TxType.EXPENSE, 12_000, accountId = cash)) as WriteResult.Success
        assertEquals(-12_000L, balances()[cash])
        assertTrue(service.delete(posted.id) is WriteResult.Success)
        assertEquals(0L, balances()[cash] ?: 0L)
        assertTrue(db.transactionDao().postingsFor(posted.id).isEmpty())
        assertNotNull(db.transactionDao().byId(posted.id))
        // A soft deleted row stays auditable.
        assertEquals(0, db.transactionDao().countActive())
    }

    @Test
    fun `foreign keys reject postings to unknown accounts`() = runBlocking {
        var failed = false
        try {
            service.post(TxDraft(TxType.INCOME, 1_000, accountId = 9_999))
        } catch (error: Throwable) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun `credit purchase increases the shop payable and never touches cash`() = runBlocking {
        val cash = account("নগদ")
        val shopId = repo.createShop("করিম স্টোর")
        val result = service.post(
            TxDraft(
                type = TxType.CREDIT_PURCHASE,
                amountMinor = 30_000,
                accountId = null,
                shopId = shopId,
                title = "বাকি বাজার",
                businessDate = "2026-10-21",
            )
        )
        assertTrue(result is WriteResult.Success)
        assertEquals(0L, balances()[cash] ?: 0L)
        val totals = repo.observeShopTotals().first()
        assertTrue(totals.isNotEmpty())
        assertEquals(30_000L, totals.first { it.shopId == shopId }.totalMinor)
    }

    @Test
    fun `itemised purchase splits cash and credit atomically`() = runBlocking {
        val cash = account("নগদ")
        val shopId = repo.createShop("রহিম স্টোর")
        val result = service.createPurchase(
            PurchaseInput(
                shopId = shopId,
                businessDate = "2026-10-21",
                occurredAt = 1_760_000_000_000,
                items = listOf(
                    PurchaseItemInput(name = "চাল", quantityMilli = 5_000, unit = "কেজি", unitPriceMinor = 7_000),
                    PurchaseItemInput(name = "ডাল", quantityMilli = 2_000, unit = "কেজি", unitPriceMinor = 14_000),
                ),
                paidMinor = 40_000,
                accountId = cash,
                creditMinor = 23_000,
            )
        )
        assertTrue(result is WriteResult.Success)
        val purchase = db.purchaseDao().byId((result as WriteResult.Success).id)!!
        assertEquals(63_000L, purchase.totalMinor)
        assertEquals(40_000L, purchase.paidMinor)
        assertEquals(23_000L, purchase.creditMinor)
        assertEquals(2, db.purchaseDao().items(purchase.id).size)
        assertEquals(-40_000L, balances()[cash])
        assertEquals(2, db.transactionDao().countActive())
    }

    @Test
    fun `debts repay interest but not principal as expense`() = runBlocking {
        val cash = account("নগদ")
        val person = repo.createPerson("আবুল")
        service.post(TxDraft(TxType.INCOME, 100_000, accountId = cash))
        val debt = service.createDebt(
            com.shohankhan.hisaabos.domain.ledger.DebtInput(
                direction = com.shohankhan.hisaabos.domain.model.DebtDirection.BORROWED,
                personId = person,
                principalMinor = 50_000,
                interestMinor = 5_000,
                occurredAt = 1_760_000_000_000,
                businessDate = "2026-10-21",
                accountId = cash,
            )
        )
        assertTrue(debt is WriteResult.Success)
        assertEquals(150_000L, balances()[cash])
        val settled = service.settleDebt(
            debtId = (debt as WriteResult.Success).id,
            amountMinor = 55_000,
            principalMinor = 50_000,
            interestMinor = 5_000,
            feeMinor = 0,
            occurredAt = 1_760_086_400_000,
            businessDate = "2026-10-22",
            accountId = cash,
        )
        assertTrue(settled is WriteResult.Success)
        assertEquals(95_000L, balances()[cash])
        val rows = db.transactionDao().allActive()
        val settlement = rows.first { it.type == TxType.DEBT_REPAID }
        assertEquals(5_000L, settlement.interestMinor)
        assertEquals(50_000L, settlement.amountMinor - settlement.interestMinor)
    }

    @Test
    fun `integrity verification passes on a clean database`() = runBlocking {
        val cash = account("নগদ")
        service.post(TxDraft(TxType.OPENING_BALANCE, 500_000, accountId = cash))
        service.post(TxDraft(TxType.EXPENSE, 10_000, accountId = cash))
        val issues = service.verifyIntegrity()
        assertTrue("unexpected integrity issues: $issues", issues.isEmpty())
    }
}
