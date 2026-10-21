package com.shohankhan.hisaabos.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.Pill
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.components.DividerLine
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldSoft
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.NeutralSoft
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary

@Composable
fun AccountsScreen(
    state: AccountsState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onAccountClick: (Long) -> Unit,
    onToggleArchived: () -> Unit,
) {
    val active = state.accounts.filter { !it.isArchived }
    val archived = state.accounts.filter { it.isArchived }
    val total = active.sumOf { state.balances[it.id] ?: 0L }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = Bn.accounts,
            onBack = onBack,
            actions = {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Outlined.Add, contentDescription = Bn.newAccount, tint = Emerald)
                }
            },
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            HisaabCard(containerColor = EmeraldSoft, borderColor = EmeraldSoft) {
                Text(Bn.total, style = MaterialTheme.typography.labelMedium, color = Emerald)
                AmountText(minor = total, color = Emerald, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "সব হিসাবের মোট ব্যালেন্স",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(16.dp))
            SectionHeader(title = "সক্রিয় হিসাব")
            if (active.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = Bn.emptyAccountHint,
                    message = "নগদ, ব্যাংক, মোবাইল ব্যাংকিং বা ওয়ালেট — যেটা ব্যবহার করেন সেটি যোগ করুন।",
                    actionLabel = Bn.newAccount,
                    onAction = onAdd,
                )
            } else {
                HisaabCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
                    active.forEachIndexed { index, account ->
                        AccountRow(account, state.balances[account.id] ?: 0L) { onAccountClick(account.id) }
                        if (index < active.size - 1) DividerLine(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
            if (archived.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onToggleArchived) {
                    Text(
                        if (state.showArchived) "সংরক্ষিত হিসাব লুকান" else "সংরক্ষিত হিসাব দেখান (${archived.size})",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (state.showArchived) {
                    HisaabCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
                        archived.forEach { account ->
                            AccountRow(account, state.balances[account.id] ?: 0L) { onAccountClick(account.id) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
fun AccountRow(account: AccountEntity, balanceMinor: Long, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(
            icon = if (account.type == AccountType.SAVINGS) Icons.Outlined.Savings else Icons.Outlined.AccountBalanceWallet,
            tint = if (account.isLiability) ExpenseRed else Emerald,
            background = if (account.isLiability) com.shohankhan.hisaabos.ui.theme.ExpenseRedSoft else EmeraldSoft,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(account.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, maxLines = 1)
                if (account.isLiability) {
                    Spacer(Modifier.width(6.dp))
                    Pill("দায়", color = ExpenseRed, background = com.shohankhan.hisaabos.ui.theme.ExpenseRedSoft)
                }
            }
            Text(
                buildString {
                    append(Bn.accountType(account.type))
                    account.institution?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
            )
        }
        AmountText(
            minor = balanceMinor,
            color = if (balanceMinor < 0) ExpenseRed else TextPrimary,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
