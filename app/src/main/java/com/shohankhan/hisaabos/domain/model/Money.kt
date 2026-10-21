package com.shohankhan.hisaabos.domain.model

/**
 * Money is always handled as signed 64-bit integer **minor units** (poysha).
 * 1 BDT = 100 poysha. Binary floating point is never used for persisted money.
 *
 * `format()` is display-only; calculations must use the Long values.
 */
object Money {

    const val MINOR_PER_TAKA: Long = 100L
    const val CURRENCY_SYMBOL: String = "৳"
    private const val MAX_MINOR: Long = 99_999_999_999_999L // ~৳1 trillion

    /** Converts whole taka to minor units, throwing on overflow instead of silently overflowing. */
    fun fromTaka(taka: Long): Long = Math.multiplyExact(taka, MINOR_PER_TAKA)

    /** True when a user-entered amount is a sane, positive, two-decimal value. */
    fun parsePositive(input: String): Long? {
        val value = parse(input) ?: return null
        return if (value > 0L) value else null
    }

    /**
     * Parses user input into minor units.
     * Accepts Bangla or Latin digits, optional currency symbol, spaces, comma grouping
     * and at most two decimal places. Returns null for anything else.
     */
    fun parse(input: String): Long? {
        if (input.isBlank()) return null
        val normalized = buildString {
            for (ch in input) {
                when {
                    ch in '০'..'৯' -> append('0' + (ch - '০'))
                    ch in '0'..'9' -> append(ch)
                    ch == '.' || ch == ','  -> append(ch)
                    ch == '-' -> append('-')
                    ch.isWhitespace() -> Unit
                    ch == '৳' || ch == 'T' || ch == 't' || ch == 'k' || ch == 'K' -> Unit
                    else -> return null
                }
            }
        }
        if (normalized.isEmpty()) return null

        val negative = normalized.startsWith("-")
        val body = normalized.removePrefix("-")
        if (body.count { it == '.' } > 1) return null
        val integerPart = body.substringBefore('.').replace(",", "")
        val fractionPart = body.substringAfter('.', "")
        if (integerPart.isEmpty() && fractionPart.isEmpty()) return null
        if (fractionPart.length > 2) return null
        if (integerPart.any { !it.isDigit() } || fractionPart.any { !it.isDigit() }) return null

        val taka = if (integerPart.isEmpty()) 0L else integerPart.toLongOrNull() ?: return null
        val poysha = when (fractionPart.length) {
            0 -> 0L
            1 -> fractionPart.toLong() * 10L
            else -> fractionPart.toLong()
        }
        val magnitude = taka * MINOR_PER_TAKA + poysha
        if (magnitude > MAX_MINOR) return null
        return if (negative) -magnitude else magnitude
    }

    /** Renders e.g. ৳1,23,456.78 using Indian/Bangladeshi lakh grouping. */
    fun format(minor: Long, banglaDigits: Boolean = true): String {
        val text = formatPlain(minor)
        return if (banglaDigits) toBanglaDigits(text) else text
    }

    /** Same as [format] but always Latin digits (used for CSV / machine files). */
    fun formatPlain(minor: Long): String {
        val negative = minor < 0
        val abs = if (minor == Long.MIN_VALUE) Long.MAX_VALUE else if (negative) -minor else minor
        val taka = abs / MINOR_PER_TAKA
        val poysha = abs % MINOR_PER_TAKA
        val grouped = groupIndian(taka.toString())
        val sign = if (negative) "-" else ""
        return "$CURRENCY_SYMBOL$sign$grouped.${poysha.toString().padStart(2, '0')}"
    }

    /** Amount without the currency symbol, e.g. 1,23,456.78 */
    fun formatAmountOnly(minor: Long): String = formatPlain(minor).removePrefix(CURRENCY_SYMBOL)

    /** Signed amount with an explicit plus sign for inflows: +৳1,000.00 / -৳250.00 */
    fun formatSigned(minor: Long, banglaDigits: Boolean = true): String {
        val base = format(minor, banglaDigits)
        return if (minor > 0) "+$base" else base
    }

    /** Compact chart label: ৳১.২৫ লাখ / ৳১২.৫ হাজার */
    fun formatCompact(minor: Long, banglaDigits: Boolean = true): String {
        val abs = kotlin.math.abs(minor)
        val taka = abs / MINOR_PER_TAKA
        val value = when {
            taka >= 10_000_000L -> BigDecimalText.scaled(taka, 10_000_000L) + " কোটি"
            taka >= 100_000L -> BigDecimalText.scaled(taka, 100_000L) + " লাখ"
            taka >= 1_000L -> BigDecimalText.scaled(taka, 1_000L) + " হাজার"
            else -> groupIndian(taka.toString())
        }
        val text = CURRENCY_SYMBOL + (if (minor < 0) "-" else "") + value
        return if (banglaDigits) toBanglaDigits(text) else text
    }

    fun toBanglaDigits(text: String): String = buildString {
        for (ch in text) {
            if (ch in '0'..'9') append('০' + (ch - '0')) else append(ch)
        }
    }

    fun toLatinDigits(text: String): String = buildString {
        for (ch in text) {
            if (ch in '০'..'৯') append('0' + (ch - '০')) else append(ch)
        }
    }

    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        var rest = digits.dropLast(3)
        val parts = ArrayList<String>()
        while (rest.length > 2) {
            parts.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        return parts.joinToString(",") + "," + lastThree
    }

    private object BigDecimalText {
        fun scaled(value: Long, unit: Long): String {
            val whole = value / unit
            val fraction = ((value % unit) * 100 / unit)
            return if (fraction == 0L) whole.toString() else "$whole.${fraction.toString().padStart(2, '0').trimEnd('0')}"
        }
    }
}
