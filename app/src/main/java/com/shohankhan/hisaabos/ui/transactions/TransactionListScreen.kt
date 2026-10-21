package com.shohankhan.hisaabos.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.data.db.TxListItem
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.CategoryChip
import com.shohankhan.hisaabos.ui.components.DateField
import com.shohankhan.hisaabos.ui.components.DividerLine
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.Pill
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.components.SecondaryButton
import com.shohankhan.hisaabos.ui.components.TxRow
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun TransactionListScreen(
    state: TxListState,
    onSearch: (String) -> Unit,
    onApplyFilters: (TxFilters) -> Unit,
    onClearFilters: () -> Unit,
    onSort: (String) -> Unit,
    onLoadMore: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    var showFilters by remember { mutableStateOf(false) }
    val groups = remember(state.items) { groupByDay(state.items) }

    Column(Modifier.fillMaxWidth().background(AppBackground)) {
        PageHeader(
            title = Bn.navTransactions,
            subtitle = "${Money.toBanglaDigits(state.items.size.toString())}টি লেনদেন দেখা যাচ্ছে",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.filters.search,
                onValueChange = onSearch,
                modifier = Modifier.weight(1f),
                placeholder = { Text(Bn.searchHint, style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            IconButton(onClick = { showFilters = true }) {
                Icon(
                    Icons.Outlined.FilterList,
                    contentDescription = Bn.filter,
                    tint = if (state.filters.activeCount > 0) Emerald else TextSecondary,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.filters.activeCount > 0) {
                Pill(
                    text = "${Money.toBanglaDigits(state.filters.activeCount.toString())}টি ফিল্টার",
                    color = Emerald,
                    background = com.shohankhan.hisaabos.ui.theme.EmeraldSoft,
                )
                TextButton(onClick = onClearFilters) { Text(Bn.clearFilters, color = TextSecondary, style = MaterialTheme.typography.labelMedium) }
            }
            Spacer(Modifier.weight(1f))
            listOf("NEWEST" to Bn.newest, "OLDEST" to Bn.oldest).forEach { (key, label) ->
                TextButton(onClick = { onSort(key) }) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.filters.sort == key) Emerald else TextSecondary,
                    )
                }
            }
        }

        if (groups.isEmpty() && !state.loading) {
            EmptyState(
                icon = Icons.Outlined.Search,
                title = Bn.noResults,
                message = if (state.filters.activeCount > 0) Bn.clearFilters else Bn.noTransactionsHint,
                actionLabel = if (state.filters.activeCount > 0) Bn.clearFilters else Bn.addTransaction,
                onAction = if (state.filters.activeCount > 0) onClearFilters else onAdd,
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                groups.forEach { group ->
                    item(key = "h-${group.date}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                BizDate.relativeBn(group.date),
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            if (group.incomeMinor > 0) {
                                AmountText(minor = group.incomeMinor, signed = true, color = Emerald, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.width(10.dp))
                            }
                            if (group.expenseMinor != 0L) {
                                AmountText(minor = -group.expenseMinor, signed = true, color = ExpenseRed, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    item(key = "c-${group.date}") {
                        HisaabCard(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                        ) {
                            group.items.forEachIndexed { index, item ->
                                TxRow(item = item, showDate = false, onClick = { onTransactionClick(item.tx.id) })
                                if (index < group.items.size - 1) DividerLine(Modifier.padding(horizontal = 16.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
                if (state.items.size >= TransactionListViewModel.PAGE_SIZE) {
                    item(key = "more") {
                        SecondaryButton(
                            text = "আরও লেনদেন দেখুন",
                            onClick = onLoadMore,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterDialog(
            filters = state.filters,
            state = state,
            onDismiss = { showFilters = false },
            onApply = {
                onApplyFilters(it)
                showFilters = false
            },
        )
    }
}

@Composable
private fun FilterDialog(
    filters: TxFilters,
    state: TxListState,
    onDismiss: () -> Unit,
    onApply: (TxFilters) -> Unit,
) {
    var local by remember { mutableStateOf(filters) }
    var minText by remember { mutableStateOf(filters.minAmountMinor?.let { Money.formatAmountOnly(it).replace(",", "") } ?: "") }
    var maxText by remember { mutableStateOf(filters.maxAmountMinor?.let { Money.formatAmountOnly(it).replace(",", "") } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Bn.filter) },
        text = {
            Column {
                Text(Bn.typeFilter, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(TxType.INCOME, TxType.EXPENSE, TxType.TRANSFER).forEach { type ->
                        CategoryChip(Bn.txType(type), local.types.contains(type)) {
                            local = local.copy(
                                types = if (local.types.contains(type)) local.types - type else local.types + type,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    listOf(TxType.CREDIT_PURCHASE, TxType.SHOP_PAYMENT).forEach { type ->
                        CategoryChip(Bn.txType(type), local.types.contains(type)) {
                            local = local.copy(
                                types = if (local.types.contains(type)) local.types - type else local.types + type,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField(
                        label = "শুরু",
                        value = local.from ?: BizDate.today(),
                        onSelect = { local = local.copy(from = it) },
                        modifier = Modifier.weight(1f),
                    )
                    DateField(
                        label = "শেষ",
                        value = local.to ?: BizDate.today(),
                        onSelect = { local = local.copy(to = it) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    label = Bn.category,
                    options = state.categories,
                    selected = state.categories.firstOrNull { it.id == local.categoryId },
                    optionLabel = { it.name },
                    onSelect = { local = local.copy(categoryId = it.id) },
                )
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    label = Bn.account,
                    options = state.accounts,
                    selected = state.accounts.firstOrNull { it.id == local.accountId },
                    optionLabel = { it.name },
                    onSelect = { local = local.copy(accountId = it.id) },
                )
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    label = Bn.shop,
                    options = state.shops,
                    selected = state.shops.firstOrNull { it.id == local.shopId },
                    optionLabel = { it.name },
                    onSelect = { local = local.copy(shopId = it.id) },
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minText,
                        onValueChange = { minText = it },
                        label = { Text(Bn.minAmount) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    OutlinedTextField(
                        value = maxText,
                        onValueChange = { maxText = it },
                        label = { Text(Bn.maxAmount) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = {
                        local = TxFilters(search = filters.search)
                        minText = ""
                        maxText = ""
                    },
                ) { Text(Bn.clearFilters, color = TextSecondary) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(
                    local.copy(
                        minAmountMinor = Money.parse(minText),
                        maxAmountMinor = Money.parse(maxText),
                    )
                )
            }) { Text(Bn.apply, color = Emerald) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Bn.cancel) } },
    )
}
