package com.shohankhan.hisaabos.ui.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.CategoryChip
import com.shohankhan.hisaabos.ui.components.DateField
import com.shohankhan.hisaabos.ui.components.DividerLine
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.components.SecondaryButton
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.components.AmountText

@Composable
fun PurchaseEntryScreen(
    shopId: Long?,
    personId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val vm = appViewModel(key = "purchase-${shopId ?: 0}-${personId ?: 0}") { container ->
        PurchaseEntryViewModel(container.repository, shopId, personId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    var showShopDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedId) {
        if (state.savedId != null) onSaved()
    }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = "বাজারের হিসাব",
            subtitle = "পণ্য লিখে মোট হিসাব রাখুন",
            onBack = onBack,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (state.shops.isEmpty() && shopId == null) {
                InfoNote(text = "বাজার লিখতে আগে একটি দোকান যোগ করুন।", tone = NoteTone.WARNING)
                Spacer(Modifier.height(8.dp))
                PrimaryButton(text = Bn.newShop, icon = Icons.Outlined.Add, onClick = { showShopDialog = true })
            } else {
                DropdownField(
                    label = Bn.shop,
                    options = state.shops,
                    selected = state.shops.firstOrNull { it.id == state.shopId },
                    optionLabel = { it.name },
                    onSelect = { vm.setShop(it.id) },
                )
                Spacer(Modifier.height(6.dp))
                CategoryChip(Bn.newShop, false) { showShopDialog = true }
            }
            Spacer(Modifier.height(12.dp))
            DropdownField(
                label = Bn.account,
                options = state.accounts,
                selected = state.accounts.firstOrNull { it.id == state.accountId },
                optionLabel = { it.name },
                onSelect = { vm.setAccount(it.id) },
            )
            Spacer(Modifier.height(10.dp))
            DropdownField(
                label = Bn.category,
                options = state.categories,
                selected = state.categories.firstOrNull { it.id == state.categoryId },
                optionLabel = { it.name },
                onSelect = { vm.setCategory(it.id) },
            )
            Spacer(Modifier.height(10.dp))
            DateField(label = Bn.date, value = state.date, onSelect = vm::setDate)

            Spacer(Modifier.height(16.dp))
            Text("পণ্যের তালিকা", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            state.items.forEach { item ->
                HisaabCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HisaabTextField(
                            value = item.name,
                            onValueChange = { value -> vm.updateItem(item.key) { it.copy(name = value) } },
                            label = Bn.productName,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { vm.removeItem(item.key) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = Bn.delete, tint = ExpenseRed)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HisaabTextField(
                            value = item.quantity,
                            onValueChange = { value -> vm.updateItem(item.key) { it.copy(quantity = value) } },
                            label = Bn.quantity,
                            modifier = Modifier.weight(1f),
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        )
                        HisaabTextField(
                            value = item.unit,
                            onValueChange = { value -> vm.updateItem(item.key) { it.copy(unit = value) } },
                            label = Bn.unit,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AmountField(
                            value = item.price,
                            onValueChange = { value -> vm.updateItem(item.key) { it.copy(price = value) } },
                            label = Bn.unitPrice,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Bn.lineTotal, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = item.lineTotalMinor, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
            }
            SecondaryButton(
                text = Bn.addItem,
                icon = Icons.Outlined.Add,
                onClick = { vm.addItem() },
            )

            Spacer(Modifier.height(16.dp))
            HisaabCard {
                Row {
                    Text(Bn.grandTotal, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                    AmountText(minor = state.totalMinor, style = MaterialTheme.typography.titleMedium, color = EmeraldDeep)
                }
                Spacer(Modifier.height(8.dp))
                DividerLine()
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmountField(
                        value = state.discount,
                        onValueChange = vm::setDiscount,
                        label = Bn.discount,
                        modifier = Modifier.weight(1f),
                    )
                    AmountField(
                        value = state.extraCharge,
                        onValueChange = vm::setExtraCharge,
                        label = Bn.extraCharge,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                AmountField(value = state.paid, onValueChange = vm::setPaid, label = Bn.paidNow)
                Spacer(Modifier.height(6.dp))
                Row {
                    Text("বাকি থাকবে", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
                    AmountText(
                        minor = state.creditMinor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.creditMinor > 0) ExpenseRed else EmeraldDeep,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HisaabTextField(
                value = state.note,
                onValueChange = vm::setNote,
                label = Bn.noteOptional,
                singleLine = false,
                maxLines = 3,
            )
            if (state.message != null) {
                Spacer(Modifier.height(10.dp))
                InfoNote(
                    text = state.message!!,
                    tone = if (state.savedId != null) NoteTone.SUCCESS else NoteTone.ERROR,
                )
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = Bn.save,
                onClick = { vm.save() },
                enabled = !state.saving && state.hasItems,
                loading = state.saving,
            )
            Spacer(Modifier.height(96.dp))
        }
    }

    if (showShopDialog) {
        var name by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showShopDialog = false },
            title = { Text(Bn.newShop) },
            text = {
                HisaabTextField(value = name, onValueChange = { name = it }, label = Bn.shopName)
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    vm.createShop(name)
                    showShopDialog = false
                }) { Text(Bn.save, color = com.shohankhan.hisaabos.ui.theme.Emerald) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showShopDialog = false }) { Text(Bn.cancel) }
            },
        )
    }
}
