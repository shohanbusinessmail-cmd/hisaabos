package com.shohankhan.hisaabos.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.CallReceived
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
import com.shohankhan.hisaabos.data.db.DebtEntity
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.DebtDirection
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.DateField
import com.shohankhan.hisaabos.ui.components.DividerLine
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.Pill
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

@Composable
fun PersonDetailScreen(
    personId: Long,
    onBack: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
) {
    val vm = appViewModel(key = "person-$personId") { container -> PersonDetailViewModel(container.repository, personId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showBorrow by remember { mutableStateOf(false) }
    var showLend by remember { mutableStateOf(false) }
    var settleTarget by remember { mutableStateOf<Pair<Long, DebtEntity>?>(null) }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = state.person?.name ?: Bn.person,
            subtitle = "ধার ও লেনদেনের খাতা",
            onBack = onBack,
        )
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                HisaabCard {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("আমি পাব", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            AmountText(minor = state.receivableMinor, style = MaterialTheme.typography.titleMedium, color = EmeraldDeep)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("আমি দেব", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            AmountText(minor = state.payableMinor, style = MaterialTheme.typography.titleMedium, color = ExpenseRed)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton(text = Bn.borrowMoney, icon = Icons.Outlined.CallReceived, onClick = { showBorrow = true })
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(text = Bn.lendMoney, icon = Icons.Outlined.CallMade, onClick = { showLend = true })
                }
                if (state.message != null) {
                    Spacer(Modifier.height(8.dp))
                    InfoNote(text = state.message!!, tone = NoteTone.INFO)
                }
                Spacer(Modifier.height(14.dp))
                SectionHeader(title = "ধারের তালিকা")
            }
            if (state.debts.isEmpty()) {
                item {
                    Text(Bn.noPeople, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            items(state.debts, key = { it.debt.id }) { row ->
                val debt = row.debt
                HisaabCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (debt.direction == DebtDirection.BORROWED) "ধার নিয়েছি" else "ধার দিয়েছি",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            Text(
                                BizDate.formatBn(debt.businessDate) + (debt.dueDate?.let { " · পরিশোধ ${BizDate.formatBn(it)}" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Pill(
                            text = if (row.isSettled) "মিটে গেছে" else "বাকি",
                            color = if (row.isSettled) EmeraldDeep else ExpenseRed,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("মূল ধার", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = debt.principalMinor, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("সুদ", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = debt.interestMinor, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("বাকি", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = row.outstandingMinor, style = MaterialTheme.typography.bodyMedium, color = ExpenseRed)
                        }
                    }
                    if (!row.isSettled) {
                        Spacer(Modifier.height(8.dp))
                        SecondaryButton(
                            text = if (debt.direction == DebtDirection.BORROWED) Bn.payMoney else Bn.collect,
                            onClick = { settleTarget = debt.id to debt },
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(14.dp))
                SectionHeader(title = "সব লেনদেন")
            }
            items(state.transactions, key = { it.tx.id }) { item ->
                TxRow(item = item, onClick = { onOpenTransaction(item.tx.id) })
                DividerLine(Modifier.padding(horizontal = 16.dp))
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showBorrow || showLend) {
        val direction = if (showBorrow) DebtDirection.BORROWED else DebtDirection.LENT
        var amount by remember { mutableStateOf("") }
        var interest by remember { mutableStateOf("") }
        var date by remember { mutableStateOf(BizDate.today()) }
        var due by remember { mutableStateOf(BizDate.today()) }
        var accountId by remember { mutableStateOf<Long?>(state.accounts.firstOrNull()?.id) }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBorrow = false; showLend = false },
            title = { Text(if (direction == DebtDirection.BORROWED) Bn.borrowMoney else Bn.lendMoney) },
            text = {
                Column {
                    AmountField(value = amount, onValueChange = { amount = it }, label = Bn.howMuch)
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = interest, onValueChange = { interest = it }, label = "সুদ (${Bn.optional})")
                    Spacer(Modifier.height(8.dp))
                    DropdownField(
                        label = if (direction == DebtDirection.BORROWED) Bn.toWhichAccount else Bn.fromWhichAccount,
                        options = state.accounts,
                        selected = state.accounts.firstOrNull { it.id == accountId },
                        optionLabel = { it.name },
                        onSelect = { accountId = it.id },
                    )
                    Spacer(Modifier.height(8.dp))
                    DateField(label = Bn.date, value = date, onSelect = { date = it })
                    Spacer(Modifier.height(8.dp))
                    DateField(label = Bn.dueDate, value = due, onSelect = { due = it })
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = note, onValueChange = { note = it }, label = Bn.noteOptional)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addDebt(direction, amount, interest, date, due, accountId, note)
                    showBorrow = false
                    showLend = false
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = {
                TextButton(onClick = { showBorrow = false; showLend = false }) { Text(Bn.cancel) }
            },
        )
    }

    settleTarget?.let { (debtId, debt) ->
        var amount by remember { mutableStateOf(Money.formatPlain(debt.principalMinor + debt.interestMinor).removePrefix("৳").replace(",", "")) }
        var interest by remember { mutableStateOf(Money.formatPlain(debt.interestMinor).removePrefix("৳").replace(",", "")) }
        var date by remember { mutableStateOf(BizDate.today()) }
        var accountId by remember { mutableStateOf<Long?>(state.accounts.firstOrNull()?.id) }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { settleTarget = null },
            title = { Text(if (debt.direction == DebtDirection.BORROWED) "ধার পরিশোধ" else "পাওনা আদায়") },
            text = {
                Column {
                    AmountField(value = amount, onValueChange = { amount = it }, label = Bn.howMuch)
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = interest, onValueChange = { interest = it }, label = "এর মধ্যে সুদ")
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
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = note, onValueChange = { note = it }, label = Bn.noteOptional)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.settle(debtId, debt, amount, interest, date, accountId, note)
                    settleTarget = null
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { settleTarget = null }) { Text(Bn.cancel) } },
        )
    }
}
