package com.shohankhan.hisaabos.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.data.db.PersonEntity
import com.shohankhan.hisaabos.data.db.ShopEntity
import com.shohankhan.hisaabos.data.db.TxListItem
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.ui.components.DividerLine
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.components.TxRow
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val transactions: List<TxListItem> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
    val shops: List<ShopEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val searching: Boolean = false,
)

class SearchViewModel(private val repository: HisaabRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    private val query = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    fun initSearch() {
        viewModelScope.launch {
            query.debounce(180).collectLatest { text ->
                val trimmed = text.trim()
                if (trimmed.length < 2) {
                    _state.value = _state.value.copy(query = text, transactions = emptyList(), people = emptyList(), shops = emptyList(), categories = emptyList(), searching = false)
                    return@collectLatest
                }
                _state.value = _state.value.copy(query = text, searching = true)
                val results = repository.searchTransactions(trimmed)
                val people = repository.allPeople().filter { it.name.contains(trimmed, ignoreCase = true) }
                val shops = repository.allShops().filter { it.name.contains(trimmed, ignoreCase = true) || it.shopkeeperName?.contains(trimmed, ignoreCase = true) == true }
                val categories = repository.allCategories().filter { it.name.contains(trimmed, ignoreCase = true) }
                _state.value = _state.value.copy(
                    transactions = results,
                    people = people,
                    shops = shops,
                    categories = categories,
                    searching = false,
                )
            }
        }
    }

    fun setQuery(text: String) {
        _state.value = _state.value.copy(query = text)
        query.value = text
    }
}

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenPerson: (Long) -> Unit,
    onOpenShop: (Long) -> Unit,
    onOpenCategory: () -> Unit,
) {
    val vm = appViewModel { SearchViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var initialized by remember { mutableStateOf(false) }
    if (!initialized) {
        vm.initSearch()
        initialized = true
    }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(title = Bn.searchTitle, onBack = onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            HisaabTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                label = Bn.search,
                placeholder = Bn.searchHint,
                imeAction = ImeAction.Search,
            )
        }
        if (state.query.trim().length < 2) {
            EmptyState(
                icon = Icons.Outlined.Search,
                title = Bn.noSearchResults,
                message = "কমপক্ষে দুই অক্ষর লিখে লেনদেন, ব্যক্তি, দোকান বা ক্যাটাগরি খুঁজুন।",
            )
            return@Column
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            if (state.people.isNotEmpty()) {
                item { SectionHeader(title = Bn.people) }
                items(state.people, key = { "p-${it.id}" }) { person ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenPerson(person.id) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(icon = Icons.Outlined.People, size = 36)
                        Spacer(Modifier.height(0.dp))
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(person.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            person.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
                        }
                    }
                    DividerLine()
                }
            }
            if (state.shops.isNotEmpty()) {
                item { SectionHeader(title = Bn.shops) }
                items(state.shops, key = { "s-${it.id}" }) { shop ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenShop(shop.id) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(icon = Icons.Outlined.Storefront, size = 36)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(shop.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            shop.shopkeeperName?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
                        }
                    }
                    DividerLine()
                }
            }
            if (state.categories.isNotEmpty()) {
                item { SectionHeader(title = Bn.categories) }
                items(state.categories, key = { "c-${it.id}" }) { category ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenCategory() }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(icon = Icons.Outlined.Category, size = 36)
                        Text(category.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.padding(start = 10.dp))
                    }
                    DividerLine()
                }
            }
            if (state.transactions.isNotEmpty()) {
                item { SectionHeader(title = Bn.navTransactions) }
                items(state.transactions, key = { "t-${it.tx.id}" }) { item ->
                    TxRow(item = item, onClick = { onOpenTransaction(item.tx.id) })
                    DividerLine()
                }
            }
            if (!state.searching && state.transactions.isEmpty() && state.people.isEmpty() && state.shops.isEmpty() && state.categories.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Search,
                        title = Bn.noSearchResults,
                        message = "এই শব্দে কিছু পাওয়া যায়নি। অন্য নাম বা নোট দিয়ে চেষ্টা করুন।",
                    )
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }
}
