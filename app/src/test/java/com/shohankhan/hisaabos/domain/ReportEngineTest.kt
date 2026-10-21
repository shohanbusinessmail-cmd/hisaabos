package com.shohankhan.hisaabos.domain

import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.data.db.TransactionEntity
import com.shohankhan.hisaabos.domain.model.CategoryKind
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.domain.report.ReportEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportEngineTest {

    private fun tx(
        type: TxType,
        amount: Long,
        date: String = "2026-05-10",
        categoryId: Long? = null,
        interest: Long = 0,
        fee: Long = 0,
        accountId: Long? = 1,
        counter: Long? = null,
    ) = TransactionEntity(
        id = 0,
        type = type,
        amountMinor = amount,
        occurredAt = 0,
        businessDate = date,
        accountId = accountId,
        counterAccountId = counter,
        categoryId = categoryId,
        interestMinor = interest,
        feeMinor = fee,
    )

    @Test
    fun `transfers and savings do not distort income or expense`() {
        val totals = ReportEngine.totals(
            listOf(
                tx(TxType.INCOME, 100_000),
                tx(TxType.EXPENSE, 30_000),
                tx(TxType.TRANSFER, 50_000, counter = 2),
                tx(TxType.SAVINGS_DEPOSIT, 20_000, counter = 3),
                tx(TxType.SAVINGS_WITHDRAWAL, 5_000, accountId = 3, counter = 1),
            ),
        )
        assertEquals(100_000L, totals.incomeMinor)
        assertEquals(30_000L, totals.expenseMinor)
        assertEquals(70_000L, totals.netIncomeMinor)
        assertEquals(2, totals.transferCount)
        // 100000 - 30000 + 0 (transfer) + 0 (savings) = 70000 cash flow
        assertEquals(70_000L, totals.netCashFlowMinor)
    }

    @Test
    fun `liability settlements expense only interest and fees`() {
        val totals = ReportEngine.totals(
            listOf(
                tx(TxType.DEBT_REPAID, 105_000, interest = 5_000),
                tx(TxType.LOAN_REPAYMENT, 52_000, interest = 2_000, fee = 100),
                tx(TxType.DEBT_BORROWED, 200_000),
                tx(TxType.LOAN_DISBURSEMENT, 300_000),
            ),
        )
        assertEquals(0L, totals.incomeMinor)
        assertEquals(7_100L, totals.expenseMinor)
        assertEquals(-157_000L + 500_000L, totals.netCashFlowMinor)
    }

    @Test
    fun `credit purchases count as spending but not as cash expense`() {
        val totals = ReportEngine.totals(listOf(tx(TxType.CREDIT_PURCHASE, 25_000, accountId = null)))
        assertEquals(25_000L, totals.expenseMinor)
        assertEquals(25_000L, totals.accrualExpenseMinor)
        assertEquals(0L, totals.cashExpenseMinor)
        assertEquals(0L, totals.netCashFlowMinor)
    }

    @Test
    fun `category breakdown groups expenses and ignores income rows`() {
        val categories = listOf(
            CategoryEntity(id = 1, name = "খাবার", kind = CategoryKind.EXPENSE, colorArgb = 0xFFFF0000),
            CategoryEntity(id = 2, name = "যাতায়াত", kind = CategoryKind.EXPENSE, colorArgb = 0xFF00FF00),
        )
        val slices = ReportEngine.spendingByCategory(
            transactions = listOf(
                tx(TxType.EXPENSE, 10_000, categoryId = 1),
                tx(TxType.EXPENSE, 5_000, categoryId = 1, date = "2026-05-11"),
                tx(TxType.EXPENSE, 7_000, categoryId = 2),
                tx(TxType.INCOME, 90_000, categoryId = null),
                tx(TxType.CREDIT_PURCHASE, 3_000, categoryId = 2, accountId = null),
            ),
            categories = categories,
        )
        assertEquals(2, slices.size)
        assertEquals("খাবার", slices[0].name)
        assertEquals(15_000L, slices[0].totalMinor)
        assertEquals(10_000L, slices[1].totalMinor)
        assertEquals(25_000L, slices.sumOf { it.totalMinor })
    }

    @Test
    fun `monthly series has a bucket per month with zero fill`() {
        val points = ReportEngine.monthlySeries(
            transactions = listOf(
                tx(TxType.INCOME, 1_000, date = "2026-03-02"),
                tx(TxType.EXPENSE, 400, date = "2026-05-01"),
            ),
            months = 3,
            todayIso = "2026-05-21",
        )
        assertEquals(listOf("2026-03", "2026-04", "2026-05"), points.map { it.month })
        assertEquals(1_000L, points[0].incomeMinor)
        assertEquals(0L, points[1].incomeMinor)
        assertEquals(400L, points[2].expenseMinor)
    }

    @Test
    fun `budget progress respects range categories and refunds`() {
        val progress = ReportEngine.budgetProgress(
            transactions = listOf(
                tx(TxType.EXPENSE, 10_000, date = "2026-05-02", categoryId = 1),
                tx(TxType.EXPENSE, 4_000, date = "2026-05-03", categoryId = 2),
                tx(TxType.REFUND, 2_000, date = "2026-05-04", categoryId = 1),
                tx(TxType.EXPENSE, 9_000, date = "2026-04-30", categoryId = 1),
            ),
            limitMinor = 20_000,
            from = "2026-05-01",
            to = "2026-05-31",
            categoryIds = setOf(1),
            includeAllCategories = false,
        )
        assertEquals(8_000L, progress.spentMinor)
        assertEquals(12_000L, progress.remainingMinor)
        assertEquals(40, progress.percentUsed)
        assertTrue(!progress.isOver)
    }

    @Test
    fun `budget over limit is flagged`() {
        val progress = ReportEngine.budgetProgress(
            transactions = listOf(tx(TxType.EXPENSE, 25_000)),
            limitMinor = 20_000,
            from = "2026-05-01",
            to = "2026-05-31",
            categoryIds = null,
            includeAllCategories = true,
        )
        assertEquals(-5_000L, progress.remainingMinor)
        assertTrue(progress.isOver)
        assertEquals(125, progress.percentUsed)
    }

    @Test
    fun `daily series aggregates by business date`() {
        val points = ReportEngine.dailySeries(
            transactions = listOf(
                tx(TxType.INCOME, 500, date = "2026-05-01"),
                tx(TxType.INCOME, 700, date = "2026-05-01"),
                tx(TxType.EXPENSE, 200, date = "2026-05-02"),
                tx(TxType.EXPENSE, 999, date = "2026-06-01"),
            ),
            from = "2026-05-01",
            to = "2026-05-31",
        )
        assertEquals(2, points.size)
        assertEquals(1_200L, points[0].incomeMinor)
        assertEquals(200L, points[1].expenseMinor)
    }
}
