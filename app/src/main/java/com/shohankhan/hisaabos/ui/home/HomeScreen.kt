package com.shohankhan.hisaabos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.domain.report.Obligation
import com.shohankhan.hisaabos.domain.report.ObligationKind
import com.shohankhan.hisaabos.domain.report.PositionSummary
import com.shohankhan.hisaabos.ui.components.ActionTile
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.AmountVisibilityToggle
import com.shohankhan.hisaabos.ui.components.DividerLine
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.components.LocalHideAmounts
import com.shohankhan.hisaabos.ui.components.Pill
import com.shohankhan.hisaabos.ui.components.ProgressTrack
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.components.StatTile
import com.shohankhan.hisaabos.ui.components.TxRow
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Divider
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
import com.shohankhan.hisaabos.ui.theme.EmeraldSoft
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.ExpenseRedSoft
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import com.shohankhan.hisaabos.ui.theme.WarningAmber
import com.shohankhan.hisaabos.ui.theme.WarningAmberSoft

@Composable
fun HomeScreen(
    state: HomeState,
    onToggleAmounts: () -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddPurchase: () -> Unit,
    onCreditPurchase: () -> Unit,
    onBorrow: () -> Unit,
    onLend: () -> Unit,
    onPayment: () -> Unit,
    onSavings: () -> Unit,
    onTransfer: () -> Unit,
    onAdjust: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOpenAccounts: () -> Unit,
    onNewAccount: () -> Unit,
    onSearch: () -> Unit,
    onObligationClick: (Obligation) -> Unit,
    onOpenLoans: () -> Unit,
    onOpenSavings: () -> Unit,
) {
    var expandedActions by remember { mutableStateOf(false) }
    val todayIso = BizDate.today()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item("header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(Bn.greeting, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(
                        BizDate.formatBn(todayIso, withWeekday = true),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                }
                IconButton(onClick = onSearch) {
                    Icon(Icons.Outlined.Search, contentDescription = Bn.search, tint = TextPrimary)
                }
                AmountVisibilityToggle(hidden = LocalHideAmounts.current, onToggle = onToggleAmounts)
            }
        }

        item("networth") {
            NetWorthCard(
                position = state.snapshot.position,
                onClick = onOpenAccounts,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item("today") {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                SectionHeader(title = "আজকের হিসাব")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = Bn.todayIncome,
                        value = Money.format(state.today.incomeMinor),
                        valueColor = Emerald,
                        background = EmeraldSoft,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = Bn.todayExpense,
                        value = Money.format(state.today.expenseMinor),
                        valueColor = ExpenseRed,
                        background = ExpenseRedSoft,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(icon = Icons.Outlined.SwapHoriz, size = 36)
                    Spacer(Modifier.width(10.dp))
                    Text(Bn.todayCashFlow, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                    AmountText(
                        minor = state.today.netCashFlowMinor,
                        signed = true,
                        color = if (state.today.netCashFlowMinor >= 0) EmeraldDeep else ExpenseRed,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }

        item("quick") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(title = Bn.quickActions)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActionTile(Icons.Outlined.TrendingUp, Bn.addIncome, onAddIncome, Modifier.weight(1f))
                    ActionTile(Icons.Outlined.ShoppingBag, Bn.addExpense, onAddExpense, Modifier.weight(1f))
                    ActionTile(Icons.Outlined.ShoppingCart, Bn.addMarket, onAddPurchase, Modifier.weight(1f))
                    ActionTile(Icons.Outlined.CreditCard, Bn.creditPurchase, onCreditPurchase, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (expandedActions) {
                        ActionTile(Icons.Outlined.CallReceived, Bn.borrowMoney, onBorrow, Modifier.weight(1f))
                        ActionTile(Icons.Outlined.CallMade, Bn.lendMoney, onLend, Modifier.weight(1f))
                        ActionTile(Icons.Outlined.Payments, Bn.payMoney, onPayment, Modifier.weight(1f))
                        ActionTile(Icons.Outlined.Savings, Bn.savingsDeposit, onSavings, Modifier.weight(1f))
                    } else {
                        ActionTile(
                            icon = Icons.Outlined.KeyboardArrowDown,
                            label = Bn.moreActions,
                            onClick = { expandedActions = true },
                            modifier = Modifier.weight(1f),
                            tint = TextSecondary,
                            background = com.shohankhan.hisaabos.ui.theme.NeutralSoft,
                        )
                        Spacer(Modifier.weight(3f))
                    }
                }
                if (expandedActions) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ActionTile(Icons.Outlined.SwapHoriz, Bn.transfer, onTransfer, Modifier.weight(1f))
                        ActionTile(Icons.Outlined.AccountBalance, Bn.adjust, onAdjust, Modifier.weight(1f))
                        Spacer(Modifier.weight(1f))
                        ActionTile(
                            icon = Icons.Outlined.KeyboardArrowUp,
                            label = "কম দেখান",
                            onClick = { expandedActions = false },
                            modifier = Modifier.weight(1f),
                            tint = TextSecondary,
                            background = com.shohankhan.hisaabos.ui.theme.NeutralSoft,
                        )
                    }
                }
            }
        }

        item("upcoming") {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                SectionHeader(title = Bn.upcoming)
                HisaabCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    if (state.obligations.isEmpty()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(icon = Icons.Outlined.AccountBalance, size = 36)
                            Spacer(Modifier.width(10.dp))
                            Text(Bn.noUpcoming, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    } else {
                        state.obligations.take(3).forEachIndexed { index, obligation ->
                            ObligationRow(obligation, todayIso) { onObligationClick(obligation) }
                            if (index < state.obligations.take(3).size - 1) DividerLine(Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        item("recent") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(title = Bn.recentActivity, actionLabel = Bn.viewAll, onAction = onSeeAllTransactions)
                HisaabCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    if (!state.hasAccounts) {
                        EmptyState(
                            icon = Icons.Outlined.Wallet,
                            title = Bn.emptyAccountHint,
                            message = "আয়, ব্যয় ও বাকি — সব হিসাব রাখতে প্রথমে একটি হিসাব তৈরি করুন।",
                            actionLabel = Bn.newAccount,
                            onAction = onNewAccount,
                        )
                    } else if (!state.hasTransactions) {
                        EmptyState(
                            icon = Icons.Outlined.ShoppingBag,
                            title = Bn.noTransactions,
                            message = Bn.noTransactionsHint,
                        )
                    } else {
                        state.recent.forEachIndexed { index, item ->
                            TxRow(item = item, onClick = { onTransactionClick(item.tx.id) })
                            if (index < state.recent.size - 1) DividerLine(Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        item("insights") {
            InsightSection(state = state, onOpenLoans = onOpenLoans, onOpenSavings = onOpenSavings)
        }
    }
}

@Composable
private fun NetWorthCard(position: PositionSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hidden = LocalHideAmounts.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Emerald, EmeraldDeep),
                )
            )
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Bn.netWorth,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Outlined.AccountBalance, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (hidden) "৳ ••••••" else Money.format(position.netWorthMinor),
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MiniStat(Bn.totalAssets, position.totalAssetsMinor, Modifier.weight(1f), hidden)
            MiniStat(Bn.totalLiabilities, position.totalLiabilitiesMinor, Modifier.weight(1f), hidden)
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.18f)),
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Wallet, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(Bn.availableCash, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
            Text(
                if (hidden) "৳ ••••" else Money.format(position.cashAndBankMinor),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Long, modifier: Modifier = Modifier, hidden: Boolean) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
        Text(
            if (hidden) "৳ ••••" else Money.format(value),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ObligationRow(obligation: Obligation, todayIso: String, onClick: () -> Unit) {
    val overdue = obligation.isOverdue(todayIso)
    val dueToday = obligation.isDueToday(todayIso)
    val icon = when (obligation.kind) {
        ObligationKind.DEBT -> Icons.Outlined.CallReceived
        ObligationKind.LOAN -> Icons.Outlined.AccountBalance
        ObligationKind.EMI -> Icons.Outlined.CreditCard
        ObligationKind.SAVINGS -> Icons.Outlined.Savings
        ObligationKind.CUSTOM -> Icons.Outlined.Payments
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(
            icon = icon,
            tint = if (overdue) ExpenseRed else if (dueToday) WarningAmber else Emerald,
            background = if (overdue) ExpenseRedSoft else if (dueToday) WarningAmberSoft else EmeraldSoft,
            size = 36,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(obligation.title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    BizDate.dueLabelBn(obligation.dueDate, todayIso),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) ExpenseRed else if (dueToday) WarningAmber else TextSecondary,
                    maxLines = 1,
                )
                obligation.subtitle?.let {
                    Text(" · $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            AmountText(
                minor = obligation.remainingMinor,
                style = MaterialTheme.typography.titleSmall,
                color = if (overdue) ExpenseRed else TextPrimary,
            )
            if (overdue) Pill("বাকি", color = ExpenseRed, background = ExpenseRedSoft)
        }
    }
}

@Composable
private fun InsightSection(state: HomeState, onOpenLoans: () -> Unit, onOpenSavings: () -> Unit) {
    val position = state.snapshot.position
    val hasInsight = state.topCategoryName != null || position.totalLiabilitiesMinor > 0
    if (!hasInsight) return
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader(title = Bn.insights)
        if (state.topCategoryName != null) {
            HisaabCard {
                Text(Bn.insightTopCategory, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Text(state.topCategoryName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                AmountText(minor = state.topCategoryMinor, color = EmeraldDeep, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(10.dp))
        }
        val previous = state.previousMonthExpense
        if (previous > 0) {
            val change = ((state.month.expenseMinor - previous) * 100) / previous
            HisaabCard {
                Text(Bn.insightVsPrevious, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (change >= 0) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = if (change > 0) ExpenseRed else Emerald,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (change == 0L) "গত মাসের সমান খরচ" else "${if (change > 0) "বেড়েছে" else "কমেছে"} ${Money.toBanglaDigits(kotlin.math.abs(change).toString())}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        if (position.totalLiabilitiesMinor > 0) {
            HisaabCard(onClick = onOpenLoans) {
                Text(Bn.insightDebt, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                AmountText(minor = position.totalLiabilitiesMinor, color = ExpenseRed, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                ProgressTrack(
                    progress = if (position.totalAssetsMinor > 0) {
                        1f - (position.totalLiabilitiesMinor.toFloat() / (position.totalAssetsMinor + position.totalLiabilitiesMinor).toFloat())
                    } else 0f,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        val goal = state.snapshot.savings.firstOrNull { it.plan.targetAmountMinor != null }
        if (goal != null) {
            HisaabCard(onClick = onOpenSavings) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Bn.insightSavings, style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                    Text("${goal.progressPercent}%", style = MaterialTheme.typography.labelMedium, color = Emerald)
                }
                Spacer(Modifier.height(4.dp))
                Text(goal.plan.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                ProgressTrack(progress = goal.progressPercent / 100f)
            }
        }
    }
}
