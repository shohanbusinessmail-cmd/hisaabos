package com.shohankhan.hisaabos.domain.report

import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.data.db.TransactionEntity
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.CategoryKind
import com.shohankhan.hisaabos.domain.model.LedgerMath
import com.shohankhan.hisaabos.domain.model.TxType

data class Totals(
    val incomeMinor: Long = 0,
    val expenseMinor: Long = 0,
    val cashExpenseMinor: Long = 0,
    val accrualExpenseMinor: Long = 0,
    val interestExpenseMinor: Long = 0,
    val refundMinor: Long = 0,
    val netCashFlowMinor: Long = 0,
    val transferCount: Int = 0,
    val transactionCount: Int = 0,
) {
    val netIncomeMinor: Long get() = incomeMinor - expenseMinor
}

data class CategorySlice(
    val categoryId: Long?,
    val name: String,
    val colorArgb: Long,
    val totalMinor: Long,
)

data class DayPoint(val date: String, val incomeMinor: Long, val expenseMinor: Long)

data class MonthPoint(val month: String, val incomeMinor: Long, val expenseMinor: Long)

data class BudgetProgress(
    val spentMinor: Long,
    val limitMinor: Long,
    val remainingMinor: Long,
    val percentUsed: Int,
    val isOver: Boolean,
)

/** All report maths lives here and is covered by unit tests. */
object ReportEngine {

    fun totals(transactions: List<TransactionEntity>): Totals {
        if (transactions.isEmpty()) return Totals()
        var income = 0L
        var expense = 0L
        var cashExpense = 0L
        var accrualExpense = 0L
        var interestExpense = 0L
        var refund = 0L
        var cashFlow = 0L
        var transfers = 0
        for (tx in transactions) {
            income += LedgerMath.recognisedIncomeMinor(tx)
            expense += LedgerMath.recognisedExpenseMinor(tx)
            when {
                tx.type == TxType.EXPENSE -> cashExpense += tx.amountMinor
                tx.type.isAccrualExpense -> accrualExpense += tx.amountMinor
                tx.type == TxType.REFUND -> refund += tx.amountMinor
                tx.type.isLiabilitySettlement -> interestExpense += tx.interestMinor + tx.feeMinor
            }
            cashFlow += LedgerMath.cashFlowMinor(tx)
            if (tx.type.isInternalTransfer) transfers++
        }
        return Totals(
            incomeMinor = income,
            expenseMinor = expense,
            cashExpenseMinor = cashExpense,
            accrualExpenseMinor = accrualExpense,
            interestExpenseMinor = interestExpense,
            refundMinor = refund,
            netCashFlowMinor = cashFlow,
            transferCount = transfers,
            transactionCount = transactions.size,
        )
    }

    /**
     * Spending grouped by user category. Only real spending counts: cash expenses,
     * credit/EMI accruals and financing charges. Transfers, savings movements,
     * loan principal and borrowing never appear here.
     */
    fun spendingByCategory(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        kind: CategoryKind = CategoryKind.EXPENSE,
    ): List<CategorySlice> {
        val names = categories.associateBy { it.id }
        val totals = HashMap<Long?, Long>()
        for (tx in transactions) {
            val amount = when {
                kind == CategoryKind.EXPENSE && (tx.type == TxType.EXPENSE || tx.type.isAccrualExpense) -> tx.amountMinor
                kind == CategoryKind.EXPENSE && tx.type == TxType.REFUND -> -tx.amountMinor
                kind == CategoryKind.EXPENSE && tx.type.isLiabilitySettlement -> tx.interestMinor + tx.feeMinor
                kind == CategoryKind.INCOME && tx.type == TxType.INCOME -> tx.amountMinor
                kind == CategoryKind.INCOME && tx.type == TxType.LOAN_COLLECTION -> tx.interestMinor
                else -> 0L
            }
            if (amount == 0L) continue
            totals[tx.categoryId] = (totals[tx.categoryId] ?: 0L) + amount
        }
        return totals.entries
            .filter { it.value != 0L }
            .map { entry ->
                val category = entry.key?.let { names[it] }
                CategorySlice(
                    categoryId = entry.key,
                    name = category?.name ?: "ক্যাটাগরি ছাড়া",
                    colorArgb = category?.colorArgb ?: 0xFF68766E,
                    totalMinor = entry.value,
                )
            }
            .sortedByDescending { it.totalMinor }
    }

    fun dailySeries(transactions: List<TransactionEntity>, from: String, to: String): List<DayPoint> {
        val income = HashMap<String, Long>()
        val expense = HashMap<String, Long>()
        for (tx in transactions) {
            if (tx.businessDate < from || tx.businessDate > to) continue
            val inc = LedgerMath.recognisedIncomeMinor(tx)
            val exp = LedgerMath.recognisedExpenseMinor(tx)
            if (inc != 0L) income[tx.businessDate] = (income[tx.businessDate] ?: 0L) + inc
            if (exp != 0L) expense[tx.businessDate] = (expense[tx.businessDate] ?: 0L) + exp
        }
        val dates = income.keys + expense.keys
        return dates.sorted().map { DayPoint(it, income[it] ?: 0L, expense[it] ?: 0L) }
    }

    /** Last [months] calendar months ending with the month of [todayIso]. */
    fun monthlySeries(transactions: List<TransactionEntity>, months: Int, todayIso: String = BizDate.today()): List<MonthPoint> {
        val today = BizDate.parse(todayIso) ?: return emptyList()
        val keys = ArrayList<String>(months)
        for (index in months - 1 downTo 0) {
            val date = today.minusMonths(index.toLong())
            keys.add("%04d-%02d".format(date.year, date.monthValue))
        }
        val income = HashMap<String, Long>()
        val expense = HashMap<String, Long>()
        for (tx in transactions) {
            val key = tx.businessDate.take(7)
            if (key !in keys) continue
            val inc = LedgerMath.recognisedIncomeMinor(tx)
            val exp = LedgerMath.recognisedExpenseMinor(tx)
            if (inc != 0L) income[key] = (income[key] ?: 0L) + inc
            if (exp != 0L) expense[key] = (expense[key] ?: 0L) + exp
        }
        return keys.map { MonthPoint(it, income[it] ?: 0L, expense[it] ?: 0L) }
    }

    fun budgetProgress(
        transactions: List<TransactionEntity>,
        limitMinor: Long,
        from: String,
        to: String,
        categoryIds: Set<Long>?,
        includeAllCategories: Boolean,
    ): BudgetProgress {
        var spent = 0L
        for (tx in transactions) {
            if (tx.businessDate < from || tx.businessDate > to) continue
            val isSpending = tx.type == TxType.EXPENSE || tx.type.isAccrualExpense || tx.type == TxType.REFUND
            if (!isSpending) continue
            if (tx.type.isLiabilitySettlement) continue
            if (!includeAllCategories && categoryIds != null) {
                if (tx.categoryId == null || tx.categoryId !in categoryIds) continue
            }
            spent += when (tx.type) {
                TxType.REFUND -> -tx.amountMinor
                else -> tx.amountMinor
            }
        }
        val remaining = limitMinor - spent
        val percent = if (limitMinor <= 0) 0 else ((spent * 100) / limitMinor).toInt()
        return BudgetProgress(
            spentMinor = spent,
            limitMinor = limitMinor,
            remainingMinor = remaining,
            percentUsed = percent.coerceIn(0, 100),
            isOver = spent > limitMinor,
        )
    }
}
