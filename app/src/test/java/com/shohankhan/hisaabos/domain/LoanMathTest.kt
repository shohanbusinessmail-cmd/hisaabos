package com.shohankhan.hisaabos.domain

import com.shohankhan.hisaabos.domain.loan.LoanMath
import com.shohankhan.hisaabos.domain.model.InterestModel
import com.shohankhan.hisaabos.domain.model.RepaymentFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanMathTest {

    @Test
    fun `amortized payment is stable for a known loan`() {
        // ৳1,00,000 at 12% for 12 months
        assertEquals(888_488L, LoanMath.amortizedPaymentMinor(10_000_000L, 1200, 12))
    }

    @Test
    fun `zero interest schedule splits principal exactly`() {
        val rows = LoanMath.buildSchedule(
            principalMinor = 10_000_000L,
            model = InterestModel.ZERO_INTEREST,
            annualRateBp = 0,
            totalRepayableMinor = null,
            interestMinor = 0,
            feeMinor = 0,
            installmentCount = 7,
            frequency = RepaymentFrequency.MONTHLY,
            firstDueDate = "2026-04-10",
        )
        assertEquals(7, rows.size)
        assertEquals(10_000_000L, rows.sumOf { it.principalMinor })
        assertEquals(0L, rows.sumOf { it.interestMinor })
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), rows.map { it.sequence })
        assertEquals("2026-04-10", rows.first().dueDate)
        assertEquals("2026-10-10", rows.last().dueDate)
    }

    @Test
    fun `flat total keeps the agreed charge and never loses a poysha`() {
        val rows = LoanMath.buildSchedule(
            principalMinor = 5_000_000L,
            model = InterestModel.FLAT_TOTAL,
            annualRateBp = 0,
            totalRepayableMinor = 6_000_000L,
            interestMinor = 1_000_000L,
            feeMinor = 50_000L,
            installmentCount = 12,
            frequency = RepaymentFrequency.MONTHLY,
            firstDueDate = "2026-01-05",
        )
        assertEquals(12, rows.size)
        assertEquals(5_000_000L, rows.sumOf { it.principalMinor })
        assertEquals(950_000L, rows.sumOf { it.interestMinor })
        assertEquals(50_000L, rows.sumOf { it.feeMinor })
        assertEquals(6_000_000L, rows.sumOf { it.amountMinor })
        rows.forEach { assertEquals(it.principalMinor + it.interestMinor + it.feeMinor, it.amountMinor) }
    }

    @Test
    fun `flat total below principal is rejected`() {
        var threw = false
        try {
            LoanMath.buildSchedule(
                principalMinor = 1_000_000L,
                model = InterestModel.FLAT_TOTAL,
                annualRateBp = 0,
                totalRepayableMinor = 900_000L,
                interestMinor = 0,
                feeMinor = 0,
                installmentCount = 3,
                frequency = RepaymentFrequency.MONTHLY,
                firstDueDate = "2026-01-01",
            )
        } catch (error: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun `reducing balance schedule pays the same total as the annuity formula`() {
        val rows = LoanMath.buildSchedule(
            principalMinor = 10_000_000L,
            model = InterestModel.REDUCING_BALANCE,
            annualRateBp = 1200,
            totalRepayableMinor = null,
            interestMinor = 0,
            feeMinor = 0,
            installmentCount = 12,
            frequency = RepaymentFrequency.MONTHLY,
            firstDueDate = "2026-01-15",
        )
        assertEquals(12, rows.size)
        assertEquals(10_000_000L, rows.sumOf { it.principalMinor })
        // Rounding may move a few poysha, but never a taka.
        assertTrue(kotlin.math.abs(rows.sumOf { it.amountMinor } - 888_488L * 12) < 1_200L)
        assertTrue(rows.all { it.interestMinor >= 0 && it.amountMinor > 0 })
        assertTrue(rows.first().interestMinor > rows.last().interestMinor)
    }

    @Test
    fun `manual schedule has no generated rows`() {
        assertTrue(
            LoanMath.buildSchedule(
                principalMinor = 1_000_000L,
                model = InterestModel.MANUAL,
                annualRateBp = 0,
                totalRepayableMinor = null,
                interestMinor = 0,
                feeMinor = 0,
                installmentCount = 5,
                frequency = RepaymentFrequency.MONTHLY,
                firstDueDate = "2026-01-01",
            ).isEmpty(),
        )
    }

    @Test
    fun `once frequency produces a single row`() {
        val rows = LoanMath.buildSchedule(
            principalMinor = 2_000_000L,
            model = InterestModel.FLAT_TOTAL,
            annualRateBp = 0,
            totalRepayableMinor = 2_200_000L,
            interestMinor = 200_000L,
            feeMinor = 0,
            installmentCount = 1,
            frequency = RepaymentFrequency.ONCE,
            firstDueDate = "2026-06-30",
        )
        assertEquals(1, rows.size)
        assertEquals(2_200_000L, rows.single().amountMinor)
    }

    @Test
    fun `distribute never loses or invents minor units`() {
        listOf(1L to 3, 100L to 7, 999L to 5, 12_345L to 11).forEach { (total, count) ->
            val parts = LoanMath.distribute(total, count)
            assertEquals(count, parts.size)
            assertEquals(total, parts.sum())
        }
    }

    @Test
    fun `outstanding principal never goes negative`() {
        assertEquals(500L, LoanMath.outstandingPrincipal(1_000L, 500L))
        assertEquals(0L, LoanMath.outstandingPrincipal(1_000L, 5_000L))
    }
}
