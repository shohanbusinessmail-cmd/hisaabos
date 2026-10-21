package com.shohankhan.hisaabos.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.domain.report.FinanceOverview
import com.shohankhan.hisaabos.domain.report.FinanceSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BooksViewModel(private val repository: HisaabRepository) : ViewModel() {

    private val _snapshot = MutableStateFlow(FinanceSnapshot())
    val snapshot: StateFlow<FinanceSnapshot> = _snapshot.asStateFlow()

    init {
        viewModelScope.launch {
            repository.invalidation(
                "accounts", "transactions", "shops", "debts", "loans", "emi_purchases", "savings_plans", "schedule_items",
            ).collect { load() }
        }
    }

    private suspend fun load() {
        val shopTotals = HashMap<Long, Pair<Long, Long>>()
        repository.observeShopTotalsList().first().forEach { row ->
            val current = shopTotals[row.shopId] ?: (0L to 0L)
            shopTotals[row.shopId] = when (row.type) {
                TxType.CREDIT_PURCHASE -> current.first + row.totalMinor to current.second
                TxType.SHOP_PAYMENT -> current.first to current.second + row.totalMinor
                else -> current
            }
        }
        _snapshot.value = FinanceOverview.build(
            accounts = repository.observeAccounts().first(),
            balances = repository.balances(),
            shops = repository.allShops(),
            shopTotals = shopTotals,
            debts = repository.openDebts(),
            people = repository.allPeople(),
            debtPayments = FinanceOverview.paymentTotals(
                repository.paidByLink(LinkKind.DEBT, TxType.DEBT_REPAID) +
                    repository.paidByLink(LinkKind.DEBT, TxType.LOAN_COLLECTION),
            ),
            loans = repository.observeLoans().first(),
            loanPayments = FinanceOverview.paymentTotals(repository.paidByLink(LinkKind.LOAN, TxType.LOAN_REPAYMENT)),
            emis = repository.observeEmis().first(),
            emiPayments = FinanceOverview.paymentTotals(repository.paidByLink(LinkKind.EMI, TxType.EMI_PAYMENT)),
            savingsPlans = repository.allSavings(),
        )
    }
}
