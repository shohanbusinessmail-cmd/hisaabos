package com.shohankhan.hisaabos.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Business-date rules used throughout HisaabOS.
 *
 * A transaction belongs to the local calendar day on which it happened
 * (device time zone at the moment of posting). A purchase made at 00:20 is
 * therefore part of the new day, not the previous one. Timestamps are stored
 * as UTC epoch millis and only converted for display.
 */
object BizDate {

    fun of(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toString()

    fun parse(iso: String?): LocalDate? =
        if (iso.isNullOrBlank()) null else runCatching { LocalDate.parse(iso) }.getOrNull()

    fun today(zone: ZoneId = ZoneId.systemDefault()): String = LocalDate.now(zone).toString()

    fun startOfDayMillis(iso: String, zone: ZoneId = ZoneId.systemDefault()): Long {
        val date = parse(iso) ?: LocalDate.now(zone)
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun endOfDayExclusiveMillis(iso: String, zone: ZoneId = ZoneId.systemDefault()): Long {
        val date = parse(iso) ?: LocalDate.now(zone)
        return date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun daysBetween(from: String, to: String): Long? {
        val a = parse(from) ?: return null
        val b = parse(to) ?: return null
        return ChronoUnit.DAYS.between(a, b)
    }

    fun plusDays(iso: String, days: Long): String? = parse(iso)?.plusDays(days)?.toString()

    fun formatBn(iso: String?, withWeekday: Boolean = false): String {
        val date = parse(iso) ?: return "—"
        val base = "${bnNumber(date.dayOfMonth)} ${monthNameBn(date.monthValue)} ${bnNumber(date.year)}"
        return if (withWeekday) "$base, ${weekdayBn(date.dayOfWeek)}" else base
    }

    fun formatMonthYearBn(iso: String?): String {
        val date = parse(iso) ?: return "—"
        return "${monthNameBn(date.monthValue)} ${bnNumber(date.year)}"
    }

    fun formatTimeBn(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val time = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()
        val hour24 = time.hour
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val suffix = if (hour24 < 12) "পূর্বাহ্ণ" else "অপরাহ্ণ"
        return "${bnNumber(hour12)}:${bnNumber(time.minute).padStart(2, '০')} $suffix"
    }

    fun formatDateTimeBn(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        "${formatBn(of(epochMillis, zone))} · ${formatTimeBn(epochMillis, zone)}"

    /** আজ / গতকাল / n দিন আগে / date */
    fun relativeBn(iso: String?, todayIso: String = today()): String {
        val date = parse(iso) ?: return "—"
        val today = parse(todayIso) ?: return formatBn(iso)
        val diff = ChronoUnit.DAYS.between(date, today)
        return when {
            diff == 0L -> "আজ"
            diff == 1L -> "গতকাল"
            diff == 2L -> "২ দিন আগে"
            diff in 3..6 -> "${bnNumber(diff.toInt())} দিন আগে"
            else -> formatBn(iso)
        }
    }

    /** Due-date urgency label used by obligation lists. */
    fun dueLabelBn(iso: String?, todayIso: String = today()): String {
        val date = parse(iso) ?: return "তারিখ নেই"
        val today = parse(todayIso) ?: return "তারিখ নেই"
        val diff = ChronoUnit.DAYS.between(today, date)
        return when {
            diff < 0 -> "${bnNumber((-diff).toInt())} দিন পার হয়েছে"
            diff == 0L -> "আজই পরিশোধ করতে হবে"
            diff == 1L -> "আগামীকাল"
            diff <= 7 -> "${bnNumber(diff.toInt())} দিন বাকি"
            else -> formatBn(iso)
        }
    }

    fun monthNameBn(month: Int): String = when (month) {
        1 -> "জানুয়ারি"
        2 -> "ফেব্রুয়ারি"
        3 -> "মার্চ"
        4 -> "এপ্রিল"
        5 -> "মে"
        6 -> "জুন"
        7 -> "জুলাই"
        8 -> "আগস্ট"
        9 -> "সেপ্টেম্বর"
        10 -> "অক্টোবর"
        11 -> "নভেম্বর"
        12 -> "ডিসেম্বর"
        else -> "—"
    }

    fun weekdayBn(day: DayOfWeek): String = when (day) {
        DayOfWeek.SATURDAY -> "শনিবার"
        DayOfWeek.SUNDAY -> "রবিবার"
        DayOfWeek.MONDAY -> "সোমবার"
        DayOfWeek.TUESDAY -> "মঙ্গলবার"
        DayOfWeek.WEDNESDAY -> "বুধবার"
        DayOfWeek.THURSDAY -> "বৃহস্পতিবার"
        DayOfWeek.FRIDAY -> "শুক্রবার"
    }

    fun bnNumber(value: Int): String = Money.toBanglaDigits(value.toString())

    /** Calendar period used by dashboard and reports. */
    enum class Period { TODAY, WEEK, MONTH, YEAR, ALL, CUSTOM }

    fun rangeOf(period: Period, anchor: LocalDate = LocalDate.now()): Pair<String, String>? = when (period) {
        Period.TODAY -> anchor.toString() to anchor.toString()
        Period.WEEK -> anchor.with(DayOfWeek.SATURDAY).toString() to anchor.toString()
        Period.MONTH -> anchor.withDayOfMonth(1).toString() to anchor.toString()
        Period.YEAR -> anchor.withDayOfYear(1).toString() to anchor.toString()
        Period.ALL -> null
        Period.CUSTOM -> null
    }

    fun previousRangeOf(period: Period, anchor: LocalDate = LocalDate.now()): Pair<String, String>? = when (period) {
        Period.TODAY -> anchor.minusDays(1).toString() to anchor.minusDays(1).toString()
        Period.WEEK -> anchor.with(DayOfWeek.SATURDAY).minusWeeks(1).toString() to
            anchor.with(DayOfWeek.SATURDAY).minusDays(1).toString()
        Period.MONTH -> anchor.withDayOfMonth(1).minusMonths(1).toString() to
            anchor.withDayOfMonth(1).minusDays(1).toString()
        Period.YEAR -> anchor.withDayOfYear(1).minusYears(1).toString() to
            anchor.withDayOfYear(1).minusDays(1).toString()
        Period.ALL, Period.CUSTOM -> null
    }
}
