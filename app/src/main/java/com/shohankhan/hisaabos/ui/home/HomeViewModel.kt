package com.shohankhan.hisaabos.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.TxListItem
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.ScheduleOwner
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.domain.report.FinanceOverview
import com.shohankhan.hisaabos.domain.report.FinanceSnapshot
import com.shohankhan.hisaabos.domain.report.Obligation
import com.shohankhan.hisaabos.domain.report.ObligationEngine
import com.shohankhan.hisaabos.domain.report.ReportEngine
import com.shohankhan.hisaabos.domain.report.Totals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeState(
    val loading: Boolean = true,
    val accounts: List<AccountEntity> = emptyList(),
    val snapshot: FinanceSnapshot = FinanceSnapshot(),
    val today: Totals = Totals(),
    val month: Totals = Totals(),
    val previousMonthExpense: Long = 0,
    val recent: List<TxListItem> = emptyList(),
    val obligations: List<Obligation> = emptyList(),
    val topCategoryName: String? = null,
    val topCategoryMinor: Long = 0,
    val error: String? = null,
) {
    val hasAccounts: Boolean get() = accounts.any { !it.isArchived }
    val hasTransactions: Boolean get() = recent.isNotEmpty()
}

private val WATCHED_TABLES = arrayOf(
    "accounts", "categories", "shops", "people", "debts", "loans",
    "emi_purchases", "savings_plans", "schedule_items", "reminders", "transactions",
)

class HomeViewModel(private val repository: HisaabRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.invalidation(*WATCHED_TABLES).collect {
                refresh()
            }
        }
    }

    private suspend fun refresh() {
        runCatching { loadSnapshot() }
            .onSuccess { _state.value = it.copy(loading = false, error = null) }
            .onFailure {
                _state.value = _state.value.copy(
                    loading = false,
                    error = "তথ্য পড়তে সমস্যা হয়েছে। আবার চেষ্টা করুন।",
                )
            }
    }

    private suspend fun loadSnapshot(): HomeState {
        val todayIso = BizDate.today()
        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1).toString()
        val previousMonthStart = today.withDayOfMonth(1).minusMonths(1).toString()
        val previousMonthEnd = today.withDayOfMonth(1).minusDays(1).toString()

        val accounts = repository.observeAccounts().first()
        val balances = repository.balances()
        val shops = repository.allShops()
        val shopTotals = HashMap<Long, Pair<Long, Long>>()
        repository.observeShopTotalsList().first().forEach { row ->
            val current = shopTotals[row.shopId] ?: (0L to 0L)
            shopTotals[row.shopId] = when (row.type) {
                TxType.CREDIT_PURCHASE -> current.first + row.totalMinor to current.second
                TxType.SHOP_PAYMENT -> current.first to current.second + row.totalMinor
                else -> current
            }
        }
        val debts = repository.openDebts()
        val people = repository.allPeople()
        val debtPayments = repository.paidByLink(LinkKind.DEBT, TxType.DEBT_REPAID) +
            repository.paidByLink(LinkKind.DEBT, TxType.LOAN_COLLECTION)
        val loans = repository.observeLoans().first()
        val loanPayments = repository.paidByLink(LinkKind.LOAN, TxType.LOAN_REPAYMENT)
        val emis = repository.observeEmis().first()
        val emiPayments = repository.paidByLink(LinkKind.EMI, TxType.EMI_PAYMENT)
        val savingsPlans = repository.allSavings()
        val schedule = repository.allSchedule()
        val reminders = repository.openReminders()

        val snapshot = FinanceOverview.build(
            accounts = accounts,
            balances = balances,
            shops = shops,
            shopTotals = shopTotals,
            debts = debts,
            people = people,
            debtPayments = FinanceOverview.paymentTotals(debtPayments),
            loans = loans,
            loanPayments = FinanceOverview.paymentTotals(loanPayments),
            emis = emis,
            emiPayments = FinanceOverview.paymentTotals(emiPayments),
            savingsPlans = savingsPlans,
        )

        val todayTx = repository.transactionsBetween(todayIso, todayIso)
        val monthTx = repository.transactionsBetween(monthStart, todayIso)
        val previousMonthTx = repository.transactionsBetween(previousMonthStart, previousMonthEnd)

        val loanTx = repository.allForLinkKind(LinkKind.LOAN)
        val emiTx = repository.allForLinkKind(LinkKind.EMI)
        val savingsTx = repository.allForLinkKind(LinkKind.SAVINGS)
        val savingsPayments = savingsTx.groupBy { it.linkId ?: -1L }
            .mapValues { (_, list) -> list.maxOf { it.occurredAt } }

        val obligations = ObligationEngine.build(
            loans = loans,
            loanSchedule = schedule.filter { it.ownerKind == ScheduleOwner.LOAN },
            loanPayments = loanTx,
            emis = emis,
            emiSchedule = schedule.filter { it.ownerKind == ScheduleOwner.EMI },
            emiPayments = emiTx,
            debts = snapshot.debts,
            savings = snapshot.savings,
            reminders = reminders,
            savingsPayments = savingsPayments,
        )

        val topSlice = ReportEngine.spendingByCategory(monthTx, repository.allCategories()).firstOrNull()

        return HomeState(
            loading = false,
            accounts = accounts,
            snapshot = snapshot,
            today = ReportEngine.totals(todayTx),
            month = ReportEngine.totals(monthTx),
            previousMonthExpense = ReportEngine.totals(previousMonthTx).expenseMinor,
            recent = repository.recentTransactions(6),
            obligations = obligations.filter { it.remainingMinor > 0 }.take(12),
            topCategoryName = topSlice?.name,
            topCategoryMinor = topSlice?.totalMinor ?: 0,
        )
    }
}
