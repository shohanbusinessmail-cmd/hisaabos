package com.shohankhan.hisaabos.domain

import com.shohankhan.hisaabos.data.db.TransactionEntity
import com.shohankhan.hisaabos.domain.model.LedgerEntry
import com.shohankhan.hisaabos.domain.model.LedgerMath
import com.shohankhan.hisaabos.domain.model.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private data class Entry(
    override val type: TxType,
    override val amountMinor: Long,
    override val accountId: Long? = 1L,
    override val counterAccountId: Long? = null,
    override val flowSign: Int = 1,
    override val interestMinor: Long = 0,
    override val feeMinor: Long = 0,
) : LedgerEntry

class LedgerMathTest {

    @Test
    fun `income and expense move cash one way`() {
        assertEquals(listOf(1L to 50_000L), LedgerMath.deltas(Entry(TxType.INCOME, 50_000L)).map { it.accountId to it.amountMinor })
        assertEquals(listOf(1L to -50_000L), LedgerMath.deltas(Entry(TxType.EXPENSE, 50_000L)).map { it.accountId to it.amountMinor })
    }

    @Test
    fun `transfer is two sided and never income or expense`() {
        val transfer = Entry(TxType.TRANSFER, 25_000L, accountId = 1, counterAccountId = 2)
        assertEquals(0L, LedgerMath.deltas(transfer).sumOf { it.amountMinor })
        assertEquals(0L, LedgerMath.cashFlowMinor(transfer))
        assertEquals(0L, LedgerMath.recognisedIncomeMinor(transfer))
        assertEquals(0L, LedgerMath.recognisedExpenseMinor(transfer))
    }

    @Test
    fun `borrowed money increases cash but is not income`() {
        val borrowed = Entry(TxType.DEBT_BORROWED, 100_000L)
        assertEquals(100_000L, LedgerMath.cashFlowMinor(borrowed))
        assertEquals(0L, LedgerMath.recognisedIncomeMinor(borrowed))
        assertEquals(0L, LedgerMath.recognisedExpenseMinor(borrowed))
    }

    @Test
    fun `lending decreases cash and is not expense until written off`() {
        val lent = Entry(TxType.LENDING, 100_000L)
        assertEquals(-100_000L, LedgerMath.cashFlowMinor(lent))
        assertEquals(0L, LedgerMath.recognisedExpenseMinor(lent))
    }

    @Test
    fun `debt repayment expenses only interest and fees`() {
        val repayment = Entry(TxType.DEBT_REPAID, 110_000L, interestMinor = 10_000L, feeMinor = 500L)
        assertEquals(-110_000L, LedgerMath.cashFlowMinor(repayment))
        assertEquals(0L, LedgerMath.recognisedIncomeMinor(repayment))
        assertEquals(10_500L, LedgerMath.recognisedExpenseMinor(repayment))
    }

    @Test
    fun `loan disbursement is cash but not income and principal repayment not expense`() {
        val disbursed = Entry(TxType.LOAN_DISBURSEMENT, 500_000L)
        assertEquals(500_000L, LedgerMath.cashFlowMinor(disbursed))
        assertEquals(0L, LedgerMath.recognisedIncomeMinor(disbursed))

        val repaid = Entry(TxType.LOAN_REPAYMENT, 52_000L, interestMinor = 2_000L)
        assertEquals(-52_000L, LedgerMath.cashFlowMinor(repaid))
        assertEquals(2_000L, LedgerMath.recognisedExpenseMinor(repaid))
    }

    @Test
    fun `collection income is only the interest component`() {
        val collection = Entry(TxType.LOAN_COLLECTION, 105_000L, interestMinor = 5_000L)
        assertEquals(105_000L, LedgerMath.cashFlowMinor(collection))
        assertEquals(5_000L, LedgerMath.recognisedIncomeMinor(collection))
        assertEquals(0L, LedgerMath.recognisedExpenseMinor(collection))
    }

    @Test
    fun `savings movement never changes net worth`() {
        val deposit = Entry(TxType.SAVINGS_DEPOSIT, 20_000L, accountId = 1, counterAccountId = 2)
        assertEquals(0L, LedgerMath.cashFlowMinor(deposit))
        assertEquals(0L, LedgerMath.recognisedIncomeMinor(deposit))
        assertEquals(0L, LedgerMath.recognisedExpenseMinor(deposit))
    }

    @Test
    fun `credit purchase is expense without cash movement`() {
        val credit = Entry(TxType.CREDIT_PURCHASE, 30_000L, accountId = 1)
        assertTrue(LedgerMath.deltas(credit).isEmpty())
        assertEquals(0L, LedgerMath.cashFlowMinor(credit))
        assertEquals(30_000L, LedgerMath.recognisedExpenseMinor(credit))
        assertFalse(credit.type.isCashExpense)
    }

    @Test
    fun `opening balance honours the flow sign`() {
        val opening = Entry(TxType.OPENING_BALANCE, 40_000L, flowSign = -1)
        assertEquals(-40_000L, LedgerMath.cashFlowMinor(opening))
        assertEquals(0L, LedgerMath.recognisedIncomeMinor(opening))
    }

    @Test
    fun `adjustment never counts as income or expense`() {
        val adjust = Entry(TxType.ADJUSTMENT, 5_000L, flowSign = 1)
        assertEquals(5_000L, LedgerMath.cashFlowMinor(adjust))
        assertEquals(0L, LedgerMath.recognisedIncomeMinor(adjust))
        assertEquals(0L, LedgerMath.recognisedExpenseMinor(adjust))
    }

    @Test
    fun `refund reduces expense`() {
        assertEquals(-3_000L, LedgerMath.recognisedExpenseMinor(Entry(TxType.REFUND, 3_000L)))
    }

    @Test
    fun `non positive amounts never post`() {
        assertTrue(LedgerMath.deltas(Entry(TxType.INCOME, 0L)).isEmpty())
        assertTrue(LedgerMath.deltas(Entry(TxType.EXPENSE, -100L)).isEmpty())
    }

    @Test
    fun `shop payable follows credit purchases and payments`() {
        assertEquals(10_000L, LedgerMath.shopPayableDelta(9L, TxType.CREDIT_PURCHASE, 10_000L))
        assertEquals(-10_000L, LedgerMath.shopPayableDelta(9L, TxType.SHOP_PAYMENT, 10_000L))
        assertEquals(0L, LedgerMath.shopPayableDelta(null, TxType.CREDIT_PURCHASE, 10_000L))
        assertEquals(0L, LedgerMath.shopPayableDelta(9L, TxType.INCOME, 10_000L))
    }

    @Test
    fun `transaction entity implements the ledger contract`() {
        val entity = TransactionEntity(
            type = TxType.EXPENSE,
            amountMinor = 1_000L,
            accountId = 3L,
            occurredAt = 0L,
            businessDate = "2026-01-01",
        )
        assertEquals(-1_000L, LedgerMath.cashFlowMinor(entity))
    }
}
