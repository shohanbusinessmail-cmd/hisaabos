package com.shohankhan.hisaabos.ui.accounts

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.shohankhan.hisaabos.data.db.TransactionEntity
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.AmountVisibilityToggle
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.SecondaryButton
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.components.TxIconColors
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun AccountDetailScreen(
    accountId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenTransaction: (Long) -> Unit,
) {
    val vm = appViewModel(key = "account-$accountId") { AccountDetailViewModel(it.repository, accountId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<String?>(null) }
    var showReconcile by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = state.account?.name ?: Bn.account,
            subtitle = state.account?.let { Bn.accountType(it.type) },
            onBack = onBack,
            actions = {
                if (state.account != null) {
                    SecondaryButton(
                        text = Bn.edit,
                        onClick = { onEdit(accountId) },
                        modifier = Modifier.width(96.dp),
                    )
                }
            },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            HisaabCard {
                Text(Bn.currentBalance, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                AmountText(minor = state.balanceMinor, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "ব্যালেন্স শুধু লেনদেন থেকে হিসাব হয়। হাতে গুনে মিললে \"ব্যালেন্স মিলান\" দিয়ে সমন্বয় লিখুন — এটি আয় বা খরচ নয়।",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Row {
                    SecondaryButton(
                        text = "ব্যালেন্স মিলান",
                        icon = Icons.Outlined.Tune,
                        onClick = { showReconcile = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (message != null) {
                    Spacer(Modifier.height(8.dp))
                    InfoNote(text = message!!, tone = NoteTone.SUCCESS)
                }
            }
            Spacer(Modifier.height(14.dp))
            SectionHeader(title = "লেনদেন")
            if (state.transactions.isEmpty()) {
                InfoNote(text = Bn.noTransactionsHint)
            }
            state.transactions.forEach { tx ->
                AccountTxRow(tx, onClick = { onOpenTransaction(tx.id) })
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(90.dp))
        }
    }

    if (showReconcile) {
        var actual by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showReconcile = false },
            title = { Text("ব্যালেন্স মিলান") },
            text = {
                Column {
                    Text(
                        "ফোনে আছে ${Money.format(state.balanceMinor)}। হাতে গুনে যা পেয়েছেন সেটি লিখুন — পার্থক্যটুকু সমন্বয় হিসেবে লেনদেন হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = actual, onValueChange = { actual = it }, label = "বাস্তব ব্যালেন্স")
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = reason, onValueChange = { reason = it }, label = Bn.adjustmentReason)
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    vm.reconcile(actual, reason) { result ->
                        message = result
                        showReconcile = false
                    }
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showReconcile = false }) { Text(Bn.cancel) }
            },
        )
    }
}

@Composable
private fun AccountTxRow(tx: TransactionEntity, onClick: () -> Unit) {
    val (tint, background, icon) = TxIconColors(tx.type)
    HisaabCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = icon, tint = tint, background = background, size = 38)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tx.title?.takeIf { it.isNotBlank() } ?: Bn.txType(tx.type),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                )
                Text(BizDate.formatBn(tx.businessDate), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            AmountText(minor = tx.amountMinor, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
    }
}
