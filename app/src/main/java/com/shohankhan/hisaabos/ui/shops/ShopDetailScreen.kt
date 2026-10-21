package com.shohankhan.hisaabos.ui.shops

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
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
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.DividerLine
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.components.SecondaryButton
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.components.TxRow
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun ShopDetailScreen(
    shopId: Long,
    onBack: () -> Unit,
    onAddPurchase: (Long) -> Unit,
    onOpenTransaction: (Long) -> Unit,
) {
    val vm = appViewModel(key = "shop-$shopId") { container -> ShopDetailViewModel(container.repository, shopId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val container = com.shohankhan.hisaabos.di.LocalAppContainer.current
    var accounts by remember { mutableStateOf<List<AccountEntity>>(emptyList()) }
    var showPay by remember { mutableStateOf(false) }

    LaunchedEffect(shopId) {
        accounts = container.repository.activeAccounts()
    }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = state.shop?.name ?: Bn.shop,
            subtitle = state.shop?.shopkeeperName ?: "দোকানের বাকির খাতা",
            onBack = onBack,
        )
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                HisaabCard {
                    Text(Bn.payable, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    AmountText(
                        minor = state.outstandingMinor,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (state.outstandingMinor > 0) ExpenseRed else EmeraldDeep,
                    )
                    Spacer(Modifier.height(8.dp))
                    DividerLine()
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("মোট বাজার", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = state.creditMinor, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("মোট পরিশোধ", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = state.paidMinor, style = MaterialTheme.typography.bodyMedium, color = EmeraldDeep)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("আগের বাকি", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = state.shop?.openingBalanceMinor ?: 0, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row {
                        PrimaryButton(
                            text = Bn.payMoney,
                            onClick = { showPay = true },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.height(0.dp))
                        SecondaryButton(
                            text = Bn.addMarket,
                            onClick = { onAddPurchase(shopId) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (state.message != null) {
                    Spacer(Modifier.height(8.dp))
                    InfoNote(
                        text = state.message!!,
                        tone = if (state.message!!.contains("সংরক্ষণ")) NoteTone.SUCCESS else NoteTone.ERROR,
                    )
                }
                Spacer(Modifier.height(14.dp))
                SectionHeader(title = Bn.purchaseHistory)
            }
            if (state.purchases.isEmpty()) {
                item {
                    Text(Bn.noItems, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            items(state.purchases, key = { it.id }) { purchase ->
                HisaabCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                BizDate.formatBn(purchase.businessDate, withWeekday = true),
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            Text(
                                "বাকি ${Money.format(purchase.creditMinor)} · পরিশোধ ${Money.format(purchase.paidMinor)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        AmountText(minor = purchase.totalMinor, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    }
                    purchase.note?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
            item {
                Spacer(Modifier.height(14.dp))
                SectionHeader(title = "খাতার লেনদেন")
            }
            items(state.transactions, key = { it.tx.id }) { item ->
                TxRow(item = item, onClick = { onOpenTransaction(item.tx.id) })
                DividerLine(Modifier.padding(horizontal = 16.dp))
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showPay) {
        var amount by remember { mutableStateOf("") }
        var accountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
        var date by remember { mutableStateOf(BizDate.today()) }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPay = false },
            title = { Text("দোকান বিল পরিশোধ") },
            text = {
                Column {
                    AmountField(value = amount, onValueChange = { amount = it }, label = Bn.howMuch)
                    Spacer(Modifier.height(8.dp))
                    DropdownField(
                        label = Bn.fromWhichAccount,
                        options = accounts,
                        selected = accounts.firstOrNull { it.id == accountId },
                        optionLabel = { it.name },
                        onSelect = { accountId = it.id },
                    )
                    Spacer(Modifier.height(8.dp))
                    com.shohankhan.hisaabos.ui.components.DateField(label = Bn.date, value = date, onSelect = { date = it })
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = note, onValueChange = { note = it }, label = Bn.noteOptional)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.pay(amount, accountId, date, note)
                    showPay = false
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { showPay = false }) { Text(Bn.cancel) } },
        )
    }
}
