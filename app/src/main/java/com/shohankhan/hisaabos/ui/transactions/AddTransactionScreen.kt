package com.shohankhan.hisaabos.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.CategoryChip
import com.shohankhan.hisaabos.ui.components.DateField
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.components.txIcon
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.CardSurface
import com.shohankhan.hisaabos.ui.theme.Divider
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun AddTransactionScreen(
    state: AddTxState,
    onUpdate: ((AddTxState) -> AddTxState) -> Unit,
    onTypeChange: (TxType) -> Unit,
    onSave: () -> Unit,
    onSaveDespiteDuplicate: () -> Unit,
    onDismissDuplicate: () -> Unit,
    onCreateAccount: (String, AccountType) -> Unit,
    onCreateCategory: (String) -> Unit,
    onCreateShop: (String) -> Unit,
    onBack: () -> Unit,
) {
    var showTypePicker by remember { mutableStateOf(false) }
    var showNewAccount by remember { mutableStateOf(false) }
    var showNewCategory by remember { mutableStateOf(false) }
    var showNewShop by remember { mutableStateOf(false) }

    val type = state.type
    val usesAccount = type != TxType.CREDIT_PURCHASE
    val usesDestination = type == TxType.TRANSFER
    val usesCategory = type == TxType.INCOME || type == TxType.EXPENSE || type == TxType.REFUND || type == TxType.CREDIT_PURCHASE
    val usesShop = type == TxType.CREDIT_PURCHASE || type == TxType.SHOP_PAYMENT || type == TxType.EXPENSE

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .imePadding(),
    ) {
        PageHeader(
            title = Bn.txType(type),
            subtitle = "লেনদেন যোগ করুন",
            onBack = onBack,
            actions = {
                TextButton(onClick = { showTypePicker = true }) { Text("ধরন বদলান", color = Emerald) }
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = txIcon(type), size = 44)
                Spacer(Modifier.width(10.dp))
                Text(
                    Bn.txType(type),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(14.dp))
            if (state.accounts.isEmpty()) {
                InfoNote(text = "লেনদেন যোগ করার আগে একটি হিসাব তৈরি করুন।", tone = NoteTone.WARNING)
                Spacer(Modifier.height(10.dp))
            }
            AmountField(
                value = state.amountText,
                onValueChange = { text -> onUpdate { it.copy(amountText = text) } },
                isError = state.error == Bn.errorAmount,
            )
            Spacer(Modifier.height(12.dp))

            if (usesAccount) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DropdownField(
                        label = if (usesDestination) "কোন হিসাব থেকে?" else Bn.account,
                        options = state.accounts,
                        selected = state.accounts.firstOrNull { it.id == state.accountId },
                        optionLabel = { "${it.name} · ${Bn.accountType(it.type)}" },
                        onSelect = { account -> onUpdate { it.copy(accountId = account.id) } },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showNewAccount = true }) { Text("＋", color = Emerald) }
                }
                if (usesDestination) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DropdownField(
                            label = Bn.toWhichAccount,
                            options = state.accounts,
                            selected = state.accounts.firstOrNull { it.id == state.counterAccountId },
                            optionLabel = { it.name },
                            onSelect = { account -> onUpdate { it.copy(counterAccountId = account.id) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (usesShop) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DropdownField(
                        label = if (type == TxType.CREDIT_PURCHASE) "কোন দোকানে বাকি?" else Bn.shop,
                        options = state.shops,
                        selected = state.shops.firstOrNull { it.id == state.shopId },
                        optionLabel = { it.name },
                        onSelect = { shop -> onUpdate { it.copy(shopId = shop.id) } },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showNewShop = true }) { Text("＋", color = Emerald) }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (usesCategory) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DropdownField(
                        label = Bn.category,
                        options = state.categories,
                        selected = state.categories.firstOrNull { it.id == state.categoryId },
                        optionLabel = { it.name },
                        onSelect = { category -> onUpdate { it.copy(categoryId = category.id) } },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showNewCategory = true }) { Text("＋", color = Emerald) }
                }
                if (state.categories.isEmpty()) {
                    Text(
                        "এখনো কোনো ক্যাটাগরি নেই — নিজের ক্যাটাগরি তৈরি করুন বা ছাড়া সংরক্ষণ করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            DateField(label = Bn.whenDate, value = state.date, onSelect = { date -> onUpdate { it.copy(date = date) } })
            Spacer(Modifier.height(10.dp))
            HisaabTextField(
                value = state.note,
                onValueChange = { text -> onUpdate { it.copy(note = text) } },
                label = Bn.noteOptional,
                singleLine = false,
                maxLines = 3,
            )

            if (type == TxType.CREDIT_PURCHASE) {
                Spacer(Modifier.height(10.dp))
                InfoNote(
                    text = "বাকিতে কেনা নগদ থেকে কাটবে না। এটি দোকানের বাকিতে যোগ হবে এবং খরচ হিসেবে রিপোর্টে দেখাবে।",
                )
            }
            if (type == TxType.TRANSFER) {
                Spacer(Modifier.height(10.dp))
                InfoNote(text = "হিসাব স্থানান্তর কোনো আয় বা খরচ নয় — শুধু টাকা এক হিসাব থেকে অন্য হিসাবে যাবে।")
            }
            if (type == TxType.SHOP_PAYMENT) {
                Spacer(Modifier.height(10.dp))
                InfoNote(text = "দোকানের বিল পরিশোধ করলে নগদ কমবে ও বাকি কমবে; এটি নতুন খরচ নয়।")
            }
            if (state.error != null) {
                Spacer(Modifier.height(10.dp))
                InfoNote(text = state.error, tone = NoteTone.ERROR)
            }
            if (state.duplicateWarning) {
                Spacer(Modifier.height(10.dp))
                InfoNote(text = Bn.duplicateWarning, tone = NoteTone.WARNING)
            }
            Spacer(Modifier.height(20.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(CardSurface)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            PrimaryButton(
                text = Bn.save,
                onClick = {
                    if (state.duplicateWarning) onSaveDespiteDuplicate() else onSave()
                },
                enabled = state.amountText.isNotBlank() && !state.saving && state.accounts.isNotEmpty(),
                loading = state.saving,
            )
        }
    }

    if (showTypePicker) {
        TypePickerDialog(
            current = type,
            onDismiss = { showTypePicker = false },
            onSelect = {
                showTypePicker = false
                onTypeChange(it)
            },
        )
    }
    if (showNewAccount) {
        NewTextDialog(
            title = Bn.newAccount,
            label = Bn.accountName,
            onDismiss = { showNewAccount = false },
            onConfirm = { name ->
                showNewAccount = false
                onCreateAccount(name, AccountType.CASH)
            },
        )
    }
    if (showNewCategory) {
        NewTextDialog(
            title = Bn.newCategory,
            label = Bn.categoryName,
            onDismiss = { showNewCategory = false },
            onConfirm = { name ->
                showNewCategory = false
                onCreateCategory(name)
            },
        )
    }
    if (showNewShop) {
        NewTextDialog(
            title = Bn.newShop,
            label = Bn.shopName,
            onDismiss = { showNewShop = false },
            onConfirm = { name ->
                showNewShop = false
                onCreateShop(name)
            },
        )
    }
    if (state.duplicateWarning) {
        AlertDialog(
            onDismissRequest = onDismissDuplicate,
            title = { Text("একই ধরনের লেনদেন আছে") },
            text = { Text(Bn.duplicateWarning) },
            confirmButton = {
                TextButton(onClick = onSaveDespiteDuplicate) { Text("তবুও সংরক্ষণ", color = Emerald) }
            },
            dismissButton = { TextButton(onClick = onDismissDuplicate) { Text(Bn.cancel) } },
        )
    }
}

@Composable
fun TypePickerDialog(current: TxType?, onDismiss: () -> Unit, onSelect: (TxType) -> Unit) {
    val types = listOf(
        TxType.INCOME, TxType.EXPENSE, TxType.TRANSFER,
        TxType.CREDIT_PURCHASE, TxType.SHOP_PAYMENT, TxType.REFUND,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("লেনদেনের ধরন") },
        text = {
            Column {
                types.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { type ->
                            Column(Modifier.weight(1f)) {
                                CategoryChip(Bn.txType(type), current == type) { onSelect(type) }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(Bn.close) } },
    )
}

@Composable
fun NewTextDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            HisaabTextField(value = text, onValueChange = { text = it }, label = label)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text(Bn.save, color = Emerald) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Bn.cancel) } },
    )
}
