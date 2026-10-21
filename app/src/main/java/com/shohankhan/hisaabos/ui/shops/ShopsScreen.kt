package com.shohankhan.hisaabos.ui.shops

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun ShopsScreen(
    onBack: () -> Unit,
    onOpenShop: (Long) -> Unit,
    onAddPurchase: (Long) -> Unit,
) {
    val vm = appViewModel { ShopsViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = Bn.shops,
            subtitle = "দোকানভিত্তিক বাকির খাতা",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = Bn.newShop, tint = Emerald)
                }
            },
        )
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                HisaabCard {
                    Text("মোট বাকি", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    AmountText(
                        minor = state.totalOutstandingMinor,
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (state.totalOutstandingMinor > 0) ExpenseRed else EmeraldDeep,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "বাকি কোনো আয় বা খরচ নয় — শুধু দোকানে পাওনা।",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                SectionHeader(title = "${state.shops.size}টি দোকান")
            }
            if (state.shops.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Storefront,
                        title = Bn.noShops,
                        message = "যে দোকানে বাকিতে বাজার করেন, সেটি যোগ করুন। পরে প্রতিটি বাজারের হিসাব আলাদা থাকবে।",
                        actionLabel = Bn.newShop,
                        onAction = { showAdd = true },
                    )
                }
            }
            items(state.shops, key = { it.shop.id }) { row ->
                HisaabCard(
                    modifier = Modifier.padding(bottom = 8.dp).clickable { onOpenShop(row.shop.id) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Outlined.Storefront, size = 40)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(row.shop.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                listOfNotNull(
                                    row.shop.shopkeeperName,
                                    row.shop.phone,
                                ).joinToString(" · ").ifBlank { "বাকির খাতা" },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (row.outstandingMinor > 0) "বাকি" else "পরিশোধিত",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                            AmountText(
                                minor = row.outstandingMinor,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (row.outstandingMinor > 0) ExpenseRed else EmeraldDeep,
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = { onAddPurchase(row.shop.id) }) {
                            Text("বাজার যোগ", color = Emerald, style = MaterialTheme.typography.labelLarge)
                        }
                        TextButton(onClick = { vm.archiveShop(row.shop.id, true) }) {
                            Text("আর্কাইভ", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var keeper by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var opening by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(Bn.newShop) },
            text = {
                Column {
                    HisaabTextField(value = name, onValueChange = { name = it }, label = Bn.shopName)
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = keeper, onValueChange = { keeper = it }, label = Bn.shopkeeper, placeholder = Bn.optional)
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = phone, onValueChange = { phone = it }, label = Bn.phone, placeholder = Bn.optional)
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = opening, onValueChange = { opening = it }, label = "আগের বাকি (${Bn.optional})")
                    if (state.error != null) {
                        Spacer(Modifier.height(8.dp))
                        InfoNote(text = state.error!!, tone = NoteTone.ERROR)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createShop(name, keeper, phone, opening) { showAdd = false }
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(Bn.cancel) } },
        )
    }
}
