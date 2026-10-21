package com.shohankhan.hisaabos.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.data.db.PersonEntity
import com.shohankhan.hisaabos.data.db.ShopEntity
import com.shohankhan.hisaabos.data.db.TxListItem
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.model.TxType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class TxFilters(
    val from: String? = null,
    val to: String? = null,
    val types: Set<TxType> = emptySet(),
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val shopId: Long? = null,
    val personId: Long? = null,
    val minAmountMinor: Long? = null,
    val maxAmountMinor: Long? = null,
    val search: String = "",
    val sort: String = "NEWEST",
) {
    val activeCount: Int
        get() = listOf(
            from != null || to != null,
            types.isNotEmpty(),
            categoryId != null,
            accountId != null,
            shopId != null,
            personId != null,
            minAmountMinor != null || maxAmountMinor != null,
        ).count { it }
}

data class TxListState(
    val loading: Boolean = true,
    val items: List<TxListItem> = emptyList(),
    val filters: TxFilters = TxFilters(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val shops: List<ShopEntity> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModel(private val repository: HisaabRepository) : ViewModel() {

    private val filters = MutableStateFlow(TxFilters())
    private val page = MutableStateFlow(1)
    private val masters = combine(
        repository.observeAccounts(),
        repository.observeCategories(),
        repository.observeShops(),
        repository.observePeople(),
    ) { accounts, categories, shops, people -> Masters(accounts, categories, shops, people) }

    private val entries = combine(filters, page) { f, p -> f to p }
        .flatMapLatest { (filter, pageIndex) ->
            repository.observeTransactionList(
                from = filter.from,
                to = filter.to,
                types = filter.types.toList(),
                categoryId = filter.categoryId,
                accountId = filter.accountId,
                shopId = filter.shopId,
                personId = filter.personId,
                minAmount = filter.minAmountMinor,
                maxAmount = filter.maxAmountMinor,
                search = filter.search,
                sort = filter.sort,
                limit = PAGE_SIZE * pageIndex,
                offset = 0,
            )
        }

    val state: StateFlow<TxListState> = combine(entries, filters, masters) { items, filter, masters ->
        TxListState(
            loading = false,
            items = items,
            filters = filter,
            accounts = masters.accounts,
            categories = masters.categories,
            shops = masters.shops,
            people = masters.people,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TxListState())

    fun applyFilters(newFilters: TxFilters) {
        filters.value = newFilters
        page.value = 1
    }

    fun setSearch(text: String) {
        filters.value = filters.value.copy(search = text)
        page.value = 1
    }

    fun setSort(sort: String) {
        filters.value = filters.value.copy(sort = sort)
    }

    fun clearFilters() {
        val search = filters.value.search
        filters.value = TxFilters(search = search)
        page.value = 1
    }

    fun loadMore() {
        page.value += 1
    }

    private data class Masters(
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val shops: List<ShopEntity>,
        val people: List<PersonEntity>,
    )

    companion object {
        const val PAGE_SIZE = 60
    }
}

/** One date section with correct daily totals (transfers and financing excluded). */
data class TxDayGroup(
    val date: String,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val items: List<TxListItem>,
)

fun groupByDay(items: List<TxListItem>): List<TxDayGroup> = items
    .groupBy { it.tx.businessDate }
    .map { (date, list) ->
        var income = 0L
        var expense = 0L
        for (item in list) {
            val tx = item.tx
            if (tx.type == TxType.INCOME) income += tx.amountMinor
            if (tx.type == TxType.EXPENSE || tx.type.isAccrualExpense) expense += tx.amountMinor
            if (tx.type == TxType.REFUND) expense -= tx.amountMinor
        }
        TxDayGroup(date = date, incomeMinor = income, expenseMinor = expense, items = list.sortedByDescending { it.tx.occurredAt })
    }
    .sortedByDescending { it.date }
