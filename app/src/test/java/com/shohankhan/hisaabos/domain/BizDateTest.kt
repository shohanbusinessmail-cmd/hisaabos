package com.shohankhan.hisaabos.domain

import com.shohankhan.hisaabos.domain.model.BizDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class BizDateTest {

    @Test
    fun `business date follows the local calendar day`() {
        val zone = ZoneId.of("Asia/Dhaka")
        // 2026-05-20T20:00:00Z is 2026-05-21T02:00 in Dhaka
        val millis = 1_779_307_200_000L
        assertEquals("2026-05-21", BizDate.of(millis, zone))
    }

    @Test
    fun `parse accepts only iso dates`() {
        assertEquals(2026, BizDate.parse("2026-05-21")?.year)
        assertNull(BizDate.parse("21/05/2026"))
        assertNull(BizDate.parse(null))
    }

    @Test
    fun `day boundaries are half open at midnight`() {
        val zone = ZoneId.of("UTC")
        val start = BizDate.startOfDayMillis("2026-05-21", zone)
        val end = BizDate.endOfDayExclusiveMillis("2026-05-21", zone)
        assertEquals(86_400_000L, end - start)
    }

    @Test
    fun `bangla date rendering uses Bangla digits and month names`() {
        assertEquals("২১ মে ২০২৬", BizDate.formatBn("2026-05-21"))
        assertTrue(BizDate.formatBn("2026-05-21", withWeekday = true).length > "২১ মে ২০২৬".length)
        assertEquals("মে ২০২৬", BizDate.formatMonthYearBn("2026-05-21"))
    }

    @Test
    fun `due labels explain overdue and upcoming dates`() {
        assertEquals("আজই পরিশোধ করতে হবে", BizDate.dueLabelBn("2026-05-21", todayIso = "2026-05-21"))
        assertEquals("আগামীকাল", BizDate.dueLabelBn("2026-05-22", todayIso = "2026-05-21"))
        assertTrue(BizDate.dueLabelBn("2026-05-19", todayIso = "2026-05-21").contains("পার হয়েছে"))
        assertEquals("তারিখ নেই", BizDate.dueLabelBn(null))
    }

    @Test
    fun `relative dates never claim a future transaction is today`() {
        assertEquals("আজ", BizDate.relativeBn("2026-05-21", "2026-05-21"))
        assertEquals("গতকাল", BizDate.relativeBn("2026-05-20", "2026-05-21"))
        assertEquals("২২ মে ২০২৬", BizDate.relativeBn("2026-05-22", "2026-05-21"))
    }
}
