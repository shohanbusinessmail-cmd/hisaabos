package com.shohankhan.hisaabos.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.data.db.DebtEntity
import com.shohankhan.hisaabos.data.db.LoanEntity
import com.shohankhan.hisaabos.data.db.ShopEntity
import com.shohankhan.hisaabos.data.db.TransactionEntity
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.report.CategorySlice
import com.shohankhan.hisaabos.domain.report.MonthPoint
import com.shohankhan.hisaabos.domain.report.ReportEngine
import com.shohankhan.hisaabos.domain.report.Totals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReportsState(
    val loading: Boolean = true,
    val period: BizDate.Period = BizDate.Period.MONTH,
    val from: String = LocalDate.now().withDayOfMonth(1).toString(),
    val to: String = BizDate.today(),
    val totals: Totals = Totals(),
    val categories: List<CategorySlice> = emptyList(),
    val monthly: List<MonthPoint> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val balances: Map<Long, Long> = emptyMap(),
    val shops: List<ShopEntity> = emptyList(),
    val shopOutstanding: Map<Long, Long> = emptyMap(),
    val debts: List<DebtEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val transactionCount: Int = 0,
)

class ReportsViewModel(private val repository: HisaabRepository) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    private var currentTransactions: List<TransactionEntity> = emptyList()
    private var categoriesCache: List<CategoryEntity> = emptyList()
    private var peopleCache: List<com.shohankhan.hisaabos.data.db.PersonEntity> = emptyList()

    init {
        viewModelScope.launch {
            repository.invalidation(
                "transactions", "accounts", "categories", "shops", "debts", "loans", "emi_purchases",
            ).collect { load(_state.value.period, _state.value.from, _state.value.to) }
        }
    }

    fun setPeriod(period: BizDate.Period) {
        val today = LocalDate.now()
        val range = when (period) {
            BizDate.Period.TODAY -> today.toString() to today.toString()
            BizDate.Period.WEEK -> today.minusDays(6).toString() to today.toString()
            BizDate.Period.MONTH -> today.withDayOfMonth(1).toString() to today.toString()
            BizDate.Period.YEAR -> today.withDayOfYear(1).toString() to today.toString()
            BizDate.Period.ALL -> "1900-01-01" to "2999-12-31"
            BizDate.Period.CUSTOM -> _state.value.from to _state.value.to
        }
        viewModelScope.launch { load(period, range.first, range.second) }
    }

    fun setCustomRange(from: String, to: String) {
        viewModelScope.launch { load(BizDate.Period.CUSTOM, from, to) }
    }

    private suspend fun load(period: BizDate.Period, from: String, to: String) {
        val transactions = repository.transactionsBetween(from, to)
        currentTransactions = transactions
        categoriesCache = repository.allCategories()
        peopleCache = repository.allPeople()
        val balances = repository.balances()
        val accounts = repository.observeAccounts().first()

        val shopRows = repository.observeShopTotalsList().first()
        val shopOutstanding = HashMap<Long, Long>()
        repository.allShops().forEach { shop ->
            var credit = 0L
            var paid = 0L
            shopRows.filter { it.shopId == shop.id }.forEach { row ->
                if (row.type == com.shohankhan.hisaabos.domain.model.TxType.CREDIT_PURCHASE) credit += row.totalMinor
                if (row.type == com.shohankhan.hisaabos.domain.model.TxType.SHOP_PAYMENT) paid += row.totalMinor
            }
            shopOutstanding[shop.id] = shop.openingBalanceMinor + credit - paid
        }

        _state.value = ReportsState(
            loading = false,
            period = period,
            from = from,
            to = to,
            totals = ReportEngine.totals(transactions),
            categories = ReportEngine.spendingByCategory(transactions, categoriesCache),
            monthly = ReportEngine.monthlySeries(repository.allActiveTransactions(), 6),
            accounts = accounts,
            balances = balances,
            shops = repository.allShops(),
            shopOutstanding = shopOutstanding,
            debts = repository.openDebts(),
            loans = repository.observeLoans().first(),
            transactionCount = transactions.size,
        )
    }

    /** CSV of the current period, with a header and machine-friendly numbers. */
    suspend fun buildCsv(): String {
        val builder = StringBuilder()
        builder.append("date,type,amount_bdt,account,category,shop,person,title,note\n")
        for (tx in currentTransactions) {
            builder.append(tx.businessDate).append(',')
            builder.append(tx.type.name).append(',')
            builder.append(Money.formatAmountOnly(tx.amountMinor).replace(",", "")).append(',')
            builder.append(escape(accountsName(tx.accountId))).append(',')
            builder.append(escape(categoriesCache.firstOrNull { it.id == tx.categoryId }?.name ?: "")).append(',')
            builder.append(escape(tx.partyName ?: "")).append(',')
            builder.append(escape(tx.personId?.let { id -> peopleCache.firstOrNull { it.id == id }?.name } ?: "")).append(',')
            builder.append(escape(tx.title ?: "")).append(',')
            builder.append(escape(tx.note ?: ""))
            builder.append('\n')
        }
        return builder.toString()
    }

    private fun accountsName(id: Long?): String {
        if (id == null) return ""
        return _state.value.accounts.firstOrNull { it.id == id }?.name ?: ""
    }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
