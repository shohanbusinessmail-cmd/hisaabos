package com.shohankhan.hisaabos.domain.ledger

import com.shohankhan.hisaabos.domain.model.Money

/**
 * Market quantities use thousandths (1.5 kg = 1500) so a receipt can be
 * reconstructed exactly without floating point. Only ASCII digits and one
 * decimal point are accepted after Bangla digits are normalised.
 */
object QuantityMath {

    fun parseMilli(raw: String): Long? {
        val cleaned = Money.toLatinDigits(raw.trim()).replace(",", "").replace(" ", "")
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split('.')
        if (parts.size > 2) return null
        val wholeText = parts[0].ifEmpty { "0" }
        val fractionText = parts.getOrNull(1) ?: ""
        if (fractionText.length > 3) return null
        if (!wholeText.all { it.isDigit() } || !fractionText.all { it.isDigit() }) return null
        val whole = wholeText.toLongOrNull() ?: return null
        val fraction = fractionText.padEnd(3, '0').toLong()
        return whole * 1000 + fraction
    }

    fun formatMilli(value: Long): String {
        val whole = value / 1000
        val fraction = value % 1000
        return if (fraction == 0L) {
            Money.toBanglaDigits(whole.toString())
        } else {
            val trimmed = fraction.toString().padStart(3, '0').trimEnd('0')
            Money.toBanglaDigits("$whole.$trimmed")
        }
    }
}
