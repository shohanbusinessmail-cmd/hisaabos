package com.shohankhan.hisaabos.ui.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import com.shohankhan.hisaabos.ui.components.DividerLine
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.KeyValueRow
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.Pill
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.components.SecondaryButton
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun LoanDetailScreen(loanId: Long, onBack: () -> Unit) {
    val vm = appViewModel(key = "loan-$loanId") { container -> LoanDetailViewModel(container.repository, loanId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showRepay by remember { mutableStateOf(false) }
    var showClose by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = state.loan?.institution ?: Bn.loansTitle,
            subtitle = state.loan?.let { Bn.loanKind(it.kind) + " · " + Bn.interestModel(it.model) } ?: "",
            onBack = onBack,
        )
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                HisaabCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("মোট বাকি", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                        Pill(if (state.loan?.isClosed == true) "বন্ধ" else "চলছে", if (state.loan?.isClosed == true) EmeraldDeep else ExpenseRed)
                    }
                    AmountText(minor = state.outstandingTotalMinor, style = MaterialTheme.typography.headlineMedium, color = ExpenseRed)
                    Spacer(Modifier.height(8.dp))
                    KeyValueRow("মূল ঋণ", Money.formatPlain(state.loan?.principalMinor ?: 0))
                    KeyValueRow("বাকি মূল", Money.formatPlain(state.outstandingPrincipalMinor))
                    KeyValueRow("সুদ + ফি", Money.formatPlain((state.loan?.interestMinor ?: 0) + (state.loan?.feeMinor ?: 0)))
                    KeyValueRow("পরিশোধিত", Money.formatPlain(state.settlement.amountMinor))
                    state.loan?.firstDueDate?.let { KeyValueRow("প্রথম কিস্তি", BizDate.formatBn(it)) }
                    Spacer(Modifier.height(10.dp))
                    if (state.loan?.isClosed == false) {
                        PrimaryButton(
                            text = if (state.next != null) "কিস্তি পরিশোধ (${Money.format(state.next!!.amountMinor)})" else "টাকা পরিশোধ",
                            onClick = { showRepay = true },
                        )
                        Spacer(Modifier.height(8.dp))
                        SecondaryButton(text = "ঋণ বন্ধ করুন", onClick = { showClose = true }, danger = true)
                    }
                }
                if (state.message != null) {
                    Spacer(Modifier.height(8.dp))
                    InfoNote(text = state.message!!, tone = NoteTone.SUCCESS)
                }
                Spacer(Modifier.height(14.dp))
                SectionHeader(title = "কিস্তির সূচি")
            }
            if (state.schedule.isEmpty()) {
                item {
                    InfoNote(text = "এই ঋণের জন্য কিস্তির সূচি নেই। প্রতিটি পরিশোধ আপনি নিজে তারিখ দিয়ে সংরক্ষণ করতে পারবেন।")
                }
            }
            items(state.schedule, key = { it.id }) { row ->
                val paid = row.sequence in state.paidIndexes
                HisaabCard(modifier = Modifier.padding(bottom = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${row.sequence} নম্বর কিস্তি", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(BizDate.formatBn(row.dueDate), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            AmountText(minor = row.amountMinor, style = MaterialTheme.typography.titleSmall, color = if (paid) EmeraldDeep else TextPrimary)
                            Text(if (paid) "পরিশোধিত" else "বাকি", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                    if (row.interestMinor > 0 || row.feeMinor > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "মূল ${Money.format(row.principalMinor)} · সুদ ${Money.format(row.interestMinor)} · ফি ${Money.format(row.feeMinor)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showRepay) {
        val next = state.next
        var amount by remember { mutableStateOf(next?.amountMinor?.let { Money.formatAmountOnly(it).replace(",", "") } ?: "") }
        var interest by remember { mutableStateOf(next?.interestMinor?.let { Money.formatAmountOnly(it).replace(",", "") } ?: "0") }
        var fee by remember { mutableStateOf(next?.feeMinor?.let { Money.formatAmountOnly(it).replace(",", "") } ?: "0") }
        var date by remember { mutableStateOf(BizDate.today()) }
        var accountId by remember { mutableStateOf<Long?>(state.accounts.firstOrNull()?.id) }
        AlertDialog(
            onDismissRequest = { showRepay = false },
            title = { Text("ঋণের কিস্তি পরিশোধ") },
            text = {
                Column {
                    AmountField(value = amount, onValueChange = { amount = it }, label = "পরিশোধের পরিমাণ")
                    Spacer(Modifier.height(8.dp))
                    Row {
                        AmountField(value = interest, onValueChange = { interest = it }, label = "এর মধ্যে সুদ", modifier = Modifier.weight(1f))
                        Spacer(Modifier.height(0.dp))
                        AmountField(value = fee, onValueChange = { fee = it }, label = "ফি", modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    DropdownField(
                        label = Bn.fromWhichAccount,
                        options = state.accounts,
                        selected = state.accounts.firstOrNull { it.id == accountId },
                        optionLabel = { it.name },
                        onSelect = { accountId = it.id },
                    )
                    Spacer(Modifier.height(8.dp))
                    DateField(label = Bn.date, value = date, onSelect = { date = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.repay(amount, interest, fee, date, accountId, next?.sequence)
                    showRepay = false
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { showRepay = false }) { Text(Bn.cancel) } },
        )
    }

    if (showClose) {
        AlertDialog(
            onDismissRequest = { showClose = false },
            title = { Text("ঋণ বন্ধ করবেন?") },
            text = { Text("সব কিস্তি পরিশোধ হয়ে থাকলে ঋণটি বন্ধ করুন। বন্ধ করা ঋণ আর পরিশোধের তালিকায় দেখাবে না।") },
            confirmButton = {
                TextButton(onClick = { vm.close(); showClose = false }) { Text("হ্যাঁ, বন্ধ করুন", color = ExpenseRed) }
            },
            dismissButton = { TextButton(onClick = { showClose = false }) { Text(Bn.cancel) } },
        )
    }
}
