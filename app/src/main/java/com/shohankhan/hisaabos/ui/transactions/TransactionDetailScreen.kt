package com.shohankhan.hisaabos.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Restore
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.KeyValueRow
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.components.SecondaryButton
import com.shohankhan.hisaabos.ui.components.txIcon
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun TransactionDetailScreen(
    state: TxDetailState,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (onDone: () -> Unit) -> Unit,
    onRestore: () -> Unit,
    onOpenShop: (Long) -> Unit,
    onOpenPerson: (Long) -> Unit,
) {
    val tx = state.transaction
    var showDelete by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground),
    ) {
        PageHeader(title = Bn.transactionDetail, onBack = onBack)
        if (tx == null) {
            InfoNote(
                text = state.error ?: Bn.noData,
                tone = NoteTone.ERROR,
                modifier = Modifier.padding(16.dp),
            )
            return@Column
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            HisaabCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = txIcon(tx.type), size = 46)
                    Spacer(Modifier.padding(5.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            tx.title?.takeIf { it.isNotBlank() } ?: Bn.txType(tx.type),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(Bn.txType(tx.type), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                AmountText(
                    minor = tx.amountMinor,
                    color = if (tx.type == TxType.INCOME) Emerald else ExpenseRed,
                    style = MaterialTheme.typography.displaySmall,
                )
                if (state.deleted) {
                    Spacer(Modifier.height(8.dp))
                    InfoNote(text = "এই লেনদেনটি মুছে ফেলা হয়েছে; এটি কোনো ব্যালেন্সে যোগ হচ্ছে না।", tone = NoteTone.WARNING)
                }
            }
            Spacer(Modifier.height(12.dp))
            HisaabCard {
                KeyValueRow(Bn.typeLabel, Bn.txType(tx.type))
                KeyValueRow(Bn.date, BizDate.formatDateTimeBn(tx.occurredAt))
                state.accountName?.let { KeyValueRow(Bn.account, it) }
                state.counterAccountName?.let { KeyValueRow("গন্তব্য", it) }
                state.categoryName?.let { KeyValueRow(Bn.category, it) }
                state.shopName?.let { KeyValueRow(Bn.shop, it) }
                state.personName?.let { KeyValueRow(Bn.person, it) }
                if (tx.interestMinor > 0) KeyValueRow(Bn.interest, Money.formatPlain(tx.interestMinor))
                if (tx.feeMinor > 0) KeyValueRow(Bn.fee, Money.formatPlain(tx.feeMinor))
                if (!tx.note.isNullOrBlank()) KeyValueRow(Bn.note, tx.note)
            }
            if (state.shopName != null && tx.shopId != null) {
                Spacer(Modifier.height(12.dp))
                SecondaryButton(text = "দোকানের হিসাব দেখুন", onClick = { onOpenShop(tx.shopId) })
            }
            if (state.personName != null && tx.personId != null) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton(text = "ব্যক্তির হিসাব দেখুন", onClick = { onOpenPerson(tx.personId) })
            }
            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                InfoNote(text = state.error, tone = NoteTone.ERROR)
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            if (!state.deleted) {
                PrimaryButton(
                    text = Bn.edit,
                    onClick = { onEdit(tx.id) },
                    icon = Icons.Outlined.Edit,
                )
                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    text = Bn.delete,
                    onClick = { showDelete = true },
                    icon = Icons.Outlined.DeleteOutline,
                    danger = true,
                )
            } else {
                PrimaryButton(
                    text = "ফিরিয়ে আনুন",
                    onClick = onRestore,
                    icon = Icons.Outlined.Restore,
                )
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(Bn.deleteTransaction) },
            text = { Text(Bn.deleteTransactionBody) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete { }
                }) { Text(Bn.delete, color = ExpenseRed) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(Bn.cancel) } },
        )
    }
}
