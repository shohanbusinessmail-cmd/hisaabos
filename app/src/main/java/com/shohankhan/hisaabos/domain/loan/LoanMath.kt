package com.shohankhan.hisaabos.domain.loan

import com.shohankhan.hisaabos.domain.model.InterestModel
import com.shohankhan.hisaabos.domain.model.RepaymentFrequency
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate

data class ScheduleRow(
    val sequence: Int,
    val dueDate: String,
    val principalMinor: Long,
    val interestMinor: Long,
    val feeMinor: Long,
    val amountMinor: Long,
) {
    init {
        require(principalMinor >= 0 && interestMinor >= 0 && feeMinor >= 0) { "negative schedule component" }
        require(amountMinor == principalMinor + interestMinor + feeMinor) { "schedule row must reconcile" }
    }
}

/**
 * Deterministic loan / EMI mathematics. Every result is in minor units and every
 * schedule reconciles exactly with its contract totals. No rate is invented:
 * callers must supply the model and the agreed figures.
 */
object LoanMath {

    /** Standard amortising payment: P·r·(1+r)^n / ((1+r)^n − 1). */
    fun amortizedPaymentMinor(principalMinor: Long, annualRateBp: Int, installmentCount: Int): Long {
        require(principalMinor > 0) { "principal must be positive" }
        require(installmentCount > 0) { "installment count must be positive" }
        require(annualRateBp >= 0) { "rate must not be negative" }
        if (annualRateBp == 0) return centsDiv(principalMinor, installmentCount.toLong())

        val monthlyRate = BigDecimal(annualRateBp)
            .divide(BigDecimal(120_000), MathContext(24))
        val factor = (BigDecimal.ONE + monthlyRate).pow(installmentCount, MathContext(30))
        val payment = BigDecimal(principalMinor)
            .multiply(monthlyRate)
            .multiply(factor)
            .divide(factor - BigDecimal.ONE, 0, RoundingMode.HALF_UP)
        return payment.toLong()
    }

    fun roundDiv(numerator: Long, denominator: Long): Long {
        require(denominator > 0) { "denominator must be positive" }
        return BigDecimal(numerator).divide(BigDecimal(denominator), 0, RoundingMode.HALF_UP).toLong()
    }

    private fun centsDiv(numerator: Long, denominator: Long): Long = roundDiv(numerator, denominator)

    /** Splits [total] into [count] parts that sum exactly to total (remainder in the last part). */
    fun distribute(total: Long, count: Int): List<Long> {
        require(count > 0) { "count must be positive" }
        require(total >= 0) { "total must not be negative" }
        val base = total / count
        val remainder = total - base * count
        return List(count) { index -> if (index == count - 1) base + remainder else base }
    }

    fun dueDate(firstDueDate: String, frequency: RepaymentFrequency, index: Int): String {
        val first = LocalDate.parse(firstDueDate)
        val date = when (frequency) {
            RepaymentFrequency.WEEKLY -> first.plusWeeks(index.toLong())
            RepaymentFrequency.BIWEEKLY -> first.plusWeeks(2L * index)
            RepaymentFrequency.MONTHLY -> first.plusMonths(index.toLong())
            RepaymentFrequency.ONCE -> first
        }
        return date.toString()
    }

    /**
     * Builds a repayment schedule for the given contract.
     *
     * @param principalMinor original principal
     * @param totalRepayableMinor total the borrower must pay (principal + charges); when null it is derived
     * @param interestMinor explicit total interest/charge when known
     * @param feeMinor explicit fees (recorded separately from interest)
     */
    fun buildSchedule(
        principalMinor: Long,
        model: InterestModel,
        annualRateBp: Int,
        totalRepayableMinor: Long?,
        interestMinor: Long,
        feeMinor: Long,
        installmentCount: Int,
        frequency: RepaymentFrequency,
        firstDueDate: String,
    ): List<ScheduleRow> {
        if (installmentCount <= 0 || principalMinor <= 0L) return emptyList()
        if (frequency == RepaymentFrequency.ONCE) return onceOff(principalMinor, totalRepayableMinor, interestMinor, feeMinor, firstDueDate)

        val rows = when (model) {
            InterestModel.ZERO_INTEREST -> {
                val principalParts = distribute(principalMinor, installmentCount)
                val feeParts = distribute(feeMinor.coerceAtLeast(0L), installmentCount)
                principalParts.mapIndexed { index, principal ->
                    row(index, frequency, firstDueDate, principal, 0L, feeParts[index])
                }
            }

            InterestModel.FLAT_TOTAL -> {
                val total = totalRepayableMinor
                    ?: (principalMinor + interestMinor.coerceAtLeast(0L) + feeMinor.coerceAtLeast(0L))
                require(total >= principalMinor) { "total repayable cannot be less than principal" }
                val charge = total - principalMinor
                val principalParts = distribute(principalMinor, installmentCount)
                val chargeParts = distribute(charge, installmentCount)
                principalParts.mapIndexed { index, principal ->
                    // Fees stay identifiable; everything else is treated as the agreed charge.
                    val fee = if (feeMinor in 1..charge) feePartsAt(index, installmentCount, feeMinor) else 0L
                    row(index, frequency, firstDueDate, principal, chargeParts[index] - fee, fee)
                }
            }

            InterestModel.REDUCING_BALANCE -> {
                if (annualRateBp <= 0) {
                    val principalParts = distribute(principalMinor, installmentCount)
                    val feeParts = distribute(feeMinor.coerceAtLeast(0L), installmentCount)
                    principalParts.mapIndexed { index, principal ->
                        row(index, frequency, firstDueDate, principal, 0L, feeParts[index])
                    }
                } else {
                    reducing(principalMinor, annualRateBp, installmentCount, feeMinor, frequency, firstDueDate)
                }
            }

            InterestModel.MANUAL -> emptyList()
        }
        return rows
    }

    private fun feePartsAt(index: Int, count: Int, totalFee: Long): Long {
        val base = totalFee / count
        val remainder = totalFee - base * count
        return if (index == count - 1) base + remainder else base
    }

    private fun onceOff(
        principalMinor: Long,
        totalRepayableMinor: Long?,
        interestMinor: Long,
        feeMinor: Long,
        dueDate: String,
    ): List<ScheduleRow> {
        val interest = interestMinor.coerceAtLeast(0L)
        val fee = feeMinor.coerceAtLeast(0L)
        val total = totalRepayableMinor ?: (principalMinor + interest + fee)
        val effectiveInterest = (total - principalMinor - fee).coerceAtLeast(0L)
        return listOf(
            ScheduleRow(
                sequence = 1,
                dueDate = dueDate,
                principalMinor = principalMinor,
                interestMinor = effectiveInterest,
                feeMinor = fee,
                amountMinor = principalMinor + effectiveInterest + fee,
            )
        )
    }

    private fun reducing(
        principalMinor: Long,
        annualRateBp: Int,
        count: Int,
        feeMinor: Long,
        frequency: RepaymentFrequency,
        firstDueDate: String,
    ): List<ScheduleRow> {
        // The amortisation formula assumes periodic (monthly) payments.
        val payment = amortizedPaymentMinor(principalMinor, annualRateBp, count)
        val monthlyRate = BigDecimal(annualRateBp).divide(BigDecimal(120_000), MathContext(24))
        val rows = ArrayList<ScheduleRow>(count)
        var balance = principalMinor
        for (index in 0 until count) {
            val interest = BigDecimal(balance).multiply(monthlyRate).setScale(0, RoundingMode.HALF_UP).toLong()
            val principalPart = if (index == count - 1) balance else (payment - interest).coerceAtMost(balance)
            val fee = feePartsAt(index, count, feeMinor.coerceAtLeast(0L))
            balance -= principalPart
            rows.add(
                ScheduleRow(
                    sequence = index + 1,
                    dueDate = dueDate(firstDueDate, frequency, index),
                    principalMinor = principalPart,
                    interestMinor = interest,
                    feeMinor = fee,
                    amountMinor = principalPart + interest + fee,
                )
            )
        }
        return rows
    }

    private fun row(
        index: Int,
        frequency: RepaymentFrequency,
        firstDueDate: String,
        principal: Long,
        interest: Long,
        fee: Long,
    ) = ScheduleRow(
        sequence = index + 1,
        dueDate = dueDate(firstDueDate, frequency, index),
        principalMinor = principal,
        interestMinor = interest,
        feeMinor = fee,
        amountMinor = principal + interest + fee,
    )

    /** Remaining principal = original principal − principal components already paid. */
    fun outstandingPrincipal(principalMinor: Long, principalPaidMinor: Long): Long =
        (principalMinor - principalPaidMinor).coerceAtLeast(0L)

    /**
     * Splits a settlement into principal / interest / fee components and validates
     * that nothing exceeds the outstanding obligation.
     */
    fun validateSettlement(
        amountMinor: Long,
        principalMinor: Long,
        interestMinor: Long,
        feeMinor: Long,
        outstandingPrincipalMinor: Long,
        allowOverpayment: Boolean = false,
    ): String? {
        if (amountMinor <= 0) return "টাকার পরিমাণ শূন্যের বেশি হতে হবে"
        if (principalMinor < 0 || interestMinor < 0 || feeMinor < 0) return "কোনো অংশ ঋণাত্মক হতে পারবে না"
        if (principalMinor + interestMinor + feeMinor != amountMinor) {
            return "মূল টাকা, সুদ ও ফি-র যোগফল মোট পরিশোধের সমান হতে হবে"
        }
        if (!allowOverpayment && principalMinor > outstandingPrincipalMinor) {
            return "মূল টাকার চেয়ে বেশি পরিশোধ করা যাবে না"
        }
        return null
    }
}
