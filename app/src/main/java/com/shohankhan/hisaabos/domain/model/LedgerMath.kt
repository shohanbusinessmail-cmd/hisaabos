package com.shohankhan.hisaabos.domain.model

/** The minimal shape needed to derive ledger effects of one transaction row. */
interface LedgerEntry {
    val type: TxType
    val amountMinor: Long
    /** +1 for a normal posting, -1 only for OPENING_BALANCE / ADJUSTMENT corrections. */
    val flowSign: Int
    val accountId: Long?
    val counterAccountId: Long?
    val interestMinor: Long get() = 0L
    val feeMinor: Long get() = 0L
}

data class AccountDelta(val accountId: Long, val amountMinor: Long)

/**
 * Single source of truth for how every transaction type moves money.
 *
 * Debits/credits are derived from persisted rows only; the UI never computes a
 * balance. Transfers between owned accounts never create income or expense;
 * borrowed money, loan proceeds and savings movements are never income.
 */
object LedgerMath {

    fun deltas(entry: LedgerEntry): List<AccountDelta> {
        val sign = if (entry.flowSign < 0) -1L else 1L
        val amount = entry.amountMinor
        if (amount <= 0L) return emptyList()
        val out = ArrayList<AccountDelta>(2)

        fun post(accountId: Long?, delta: Long) {
            if (accountId != null && delta != 0L) out.add(AccountDelta(accountId, delta))
        }

        when (entry.type) {
            TxType.INCOME,
            TxType.REFUND,
            TxType.DEBT_BORROWED,
            TxType.LOAN_COLLECTION,
            TxType.LOAN_DISBURSEMENT,
            -> post(entry.accountId, amount)

            TxType.EXPENSE,
            TxType.SHOP_PAYMENT,
            TxType.DEBT_REPAID,
            TxType.LENDING,
            TxType.LOAN_REPAYMENT,
            TxType.EMI_PAYMENT,
            -> post(entry.accountId, -amount)

            TxType.TRANSFER,
            TxType.SAVINGS_DEPOSIT,
            TxType.SAVINGS_WITHDRAWAL,
            -> {
                post(entry.accountId, -amount)
                post(entry.counterAccountId, amount)
            }

            TxType.OPENING_BALANCE,
            TxType.ADJUSTMENT,
            -> post(entry.accountId, amount * sign)

            // No cash movement: a payable/liability is created instead.
            TxType.CREDIT_PURCHASE,
            TxType.EMI_PURCHASE,
            -> Unit
        }
        return out
    }

    /** Cash-flow contribution of a row (positive = money came in to owned accounts). */
    fun cashFlowMinor(entry: LedgerEntry): Long = deltas(entry).sumOf { it.amountMinor }

    /**
     * Spending recognised in reports. Interest/fee portions of settlements are
     * added separately by [interestExpenseMinor] so principal is never expensed.
     */
    fun recognisedExpenseMinor(entry: LedgerEntry): Long = when {
        entry.type == TxType.EXPENSE -> entry.amountMinor
        entry.type.isAccrualExpense -> entry.amountMinor
        entry.type == TxType.REFUND -> -entry.amountMinor
        else -> interestExpenseMinor(entry)
    }

    /** Interest, fees and penalties recorded on a settlement row: these are real expenses. */
    fun interestExpenseMinor(entry: LedgerEntry): Long =
        if (entry.type.isLiabilitySettlement) {
            (entry.interestMinor + entry.feeMinor).coerceAtLeast(0L)
        } else {
            0L
        }

    /** Income recognised in reports: earned income plus interest received on collections. */
    fun recognisedIncomeMinor(entry: LedgerEntry): Long = when (entry.type) {
        TxType.INCOME -> entry.amountMinor
        TxType.LOAN_COLLECTION -> entry.interestMinor.coerceAtLeast(0L)
        else -> 0L
    }

    /** Shop payable movement caused by a row, keyed by shop id. */
    fun shopPayableDelta(shopId: Long?, type: TxType, amountMinor: Long): Long = when {
        shopId == null -> 0L
        type == TxType.CREDIT_PURCHASE -> amountMinor
        type == TxType.SHOP_PAYMENT -> -amountMinor
        else -> 0L
    }
}
