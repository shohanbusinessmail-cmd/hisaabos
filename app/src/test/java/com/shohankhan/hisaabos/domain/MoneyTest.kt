package com.shohankhan.hisaabos.domain

import com.shohankhan.hisaabos.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun `parses Bangla digits with Indian grouping`() {
        assertEquals(12_345_678L, Money.parse("১,২৩,৪৫৬.৭৮"))
        assertEquals(12_345_678L, Money.parse("123456.78"))
        assertEquals(100_000L, Money.parse("১,০০০"))
        assertEquals(5L, Money.parse("0.05"))
        assertEquals(50L, Money.parse("0.5"))
    }

    @Test
    fun `rejects malformed and negative input`() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("১২.৩৪৫"))
        assertNull(Money.parse("abc"))
        assertEquals(-500L, Money.parse("-5"))
        assertNull(Money.parsePositive("-5"))
        assertNull(Money.parsePositive("0"))
        assertNull(Money.parsePositive("0.00"))
        assertEquals(1L, Money.parsePositive("0.01"))
    }

    @Test
    fun `formats with the taka sign and Indian grouping`() {
        assertEquals("৳1,23,456.78", Money.format(12_345_678L, banglaDigits = false))
        assertEquals("৳১,২৩,৪৫৬.৭৮", Money.format(12_345_678L, banglaDigits = true))
        assertEquals("৳0.00", Money.format(0L, banglaDigits = false))
        assertEquals("1,23,456.78", Money.formatAmountOnly(12_345_678L))
    }

    @Test
    fun `signed and plain formatting stay consistent`() {
        assertEquals("+৳১.০০", Money.formatSigned(100L))
        assertEquals("-৳১.০০", Money.formatSigned(-100L))
        assertEquals("৳1.00", Money.formatPlain(100L))
        assertTrue(Money.formatCompact(12_345_678L, banglaDigits = false).contains("লাখ"))
    }

    @Test
    fun `digit conversion round-trips`() {
        val bangla = Money.toBanglaDigits("0123456789")
        assertEquals("০১২৩৪৫৬৭৮৯", bangla)
        assertEquals("9876543210", Money.toLatinDigits("৯৮৭৬৫৪৩২১০"))
    }
}
