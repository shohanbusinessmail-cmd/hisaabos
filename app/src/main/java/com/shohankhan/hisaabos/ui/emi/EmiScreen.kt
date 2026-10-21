package com.shohankhan.hisaabos.ui.emi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.DateField
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.Pill
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.loans.EmiFormViewModel
import com.shohankhan.hisaabos.ui.loans.EmiViewModel
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun EmiScreen(onBack: () -> Unit, onOpenEmi: (Long) -> Unit) {
    val vm = appViewModel { EmiViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val formVm = appViewModel { EmiFormViewModel(it.repository) }
    val formState by formVm.state.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }
    var newId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(newId) {
        newId?.let {
            showForm = false
            newId = null
            onOpenEmi(it)
        }
    }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = Bn.emiPurchase,
            subtitle = "কিস্তিতে কেনা পণ্যের হিসাব",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showForm = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = Bn.newEmi, tint = Emerald)
                }
            },
        )
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                HisaabCard {
                    Text("মোট বাকি কিস্তি", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    AmountText(minor = state.outstandingMinor, style = MaterialTheme.typography.headlineSmall, color = ExpenseRed)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "কিস্তিতে কেনা পণ্য কেনার দিনেই খরচ হিসেবে ধরা হয়; পরে কিস্তি পরিশোধ শুধু দায় কমায়।",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                SectionHeader(title = "${state.emis.count { !it.emi.isSettled }}টি চলমান চুক্তি")
            }
            if (state.emis.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.CreditCard,
                        title = "কোনো কিস্তি চুক্তি নেই",
                        message = "ফ্রিজ, ফোন বা আসবাব কিস্তিতে কিনলে চুক্তিটি যোগ করুন। কিস্তির সূচি নিজে থেকেই তৈরি হবে।",
                        actionLabel = Bn.newEmi,
                        onAction = { showForm = true },
                    )
                }
            }
            items(state.emis, key = { it.emi.id }) { row ->
                HisaabCard(
                    modifier = Modifier.padding(bottom = 8.dp).clickable { onOpenEmi(row.emi.id) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Outlined.CreditCard, size = 40)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(row.emi.productName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                listOfNotNull(
                                    row.emi.merchant,
                                    row.nextDue?.let { "পরের কিস্তি ${BizDate.formatBn(it.dueDate)}" },
                                ).joinToString(" · ").ifBlank { "${row.emi.installmentCount}টি কিস্তি" },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Pill(if (row.emi.isSettled) "শেষ" else "চলছে", if (row.emi.isSettled) EmeraldDeep else ExpenseRed)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("মোট মূল্য", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = row.emi.totalFinancedPriceMinor, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("বাকি", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = row.outstandingTotalMinor, style = MaterialTheme.typography.bodyMedium, color = ExpenseRed)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("কিস্তি", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = row.emi.installmentAmountMinor, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showForm) {
        var product by remember { mutableStateOf("") }
        var merchant by remember { mutableStateOf("") }
        var total by remember { mutableStateOf("") }
        var cash by remember { mutableStateOf("") }
        var down by remember { mutableStateOf("") }
        var interest by remember { mutableStateOf("") }
        var count by remember { mutableStateOf("6") }
        var firstDue by remember { mutableStateOf(BizDate.today()) }
        var accountId by remember { mutableStateOf<Long?>(formState.accounts.firstOrNull()?.id) }
        var notes by remember { mutableStateOf("") }
        LaunchedEffect(formState.accounts) {
            if (accountId == null) accountId = formState.accounts.firstOrNull()?.id
        }
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(Bn.newEmi) },
            text = {
                Column(Modifier.height(440.dp).verticalScroll(rememberScrollState())) {
                    HisaabTextField(value = product, onValueChange = { product = it }, label = Bn.productName)
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = merchant, onValueChange = { merchant = it }, label = Bn.merchant)
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = total, onValueChange = { total = it }, label = "কিস্তির মোট মূল্য")
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = cash, onValueChange = { cash = it }, label = "নগদ মূল্য (${Bn.optional})")
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = down, onValueChange = { down = it }, label = Bn.downPayment)
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = interest, onValueChange = { interest = it }, label = "সুদের অংশ (${Bn.optional})")
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = count, onValueChange = { count = it }, label = Bn.installmentCount)
                    Spacer(Modifier.height(8.dp))
                    DateField(label = Bn.firstDue, value = firstDue, onSelect = { firstDue = it })
                    Spacer(Modifier.height(8.dp))
                    DropdownField(Bn.account, formState.accounts, formState.accounts.firstOrNull { it.id == accountId }, { it.name }, { accountId = it.id })
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = notes, onValueChange = { notes = it }, label = Bn.noteOptional)
                    if (formState.message != null) {
                        Spacer(Modifier.height(8.dp))
                        InfoNote(text = formState.message!!, tone = if (formState.createdId != null) NoteTone.SUCCESS else NoteTone.ERROR)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    formVm.create(product, merchant, total, cash, down, interest, count, firstDue, accountId, notes)
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text(Bn.cancel) } },
        )
        LaunchedEffect(formState.createdId) {
            formState.createdId?.let { newId = it }
        }
    }
}
