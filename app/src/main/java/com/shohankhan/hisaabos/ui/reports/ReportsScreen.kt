package com.shohankhan.hisaabos.ui.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.report.MonthPoint
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.CategoryChip
import com.shohankhan.hisaabos.ui.components.DividerLine
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.ProgressTrack
import com.shohankhan.hisaabos.ui.components.SecondaryButton
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.components.colorFromArgb
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Divider
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
import com.shohankhan.hisaabos.ui.theme.EmeraldSoft
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(
    state: ReportsState,
    onPeriodChange: (BizDate.Period) -> Unit,
    onCustomRange: (String, String) -> Unit,
    onExportCsv: suspend () -> String,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var showRange by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                message = runCatching {
                    val csv = onExportCsv()
                    context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                }.fold(
                    onSuccess = { "CSV ফাইল সংরক্ষণ হয়েছে" },
                    onFailure = { "ফাইল সংরক্ষণ করা যায়নি" },
                )
            }
        }
    }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(title = Bn.reportsTitle, subtitle = "আপনার আসল তথ্য থেকে তৈরি")
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    BizDate.Period.TODAY to Bn.periodToday,
                    BizDate.Period.WEEK to Bn.periodWeek,
                    BizDate.Period.MONTH to Bn.periodMonth,
                    BizDate.Period.YEAR to Bn.periodYear,
                ).forEach { (period, label) ->
                    CategoryChip(label, state.period == period) { onPeriodChange(period) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CategoryChip(Bn.periodAll, state.period == BizDate.Period.ALL) { onPeriodChange(BizDate.Period.ALL) }
                CategoryChip(Bn.customRange, state.period == BizDate.Period.CUSTOM) { showRange = true }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "${BizDate.formatBn(state.from)} — ${BizDate.formatBn(state.to)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            if (state.period == BizDate.Period.CUSTOM && showRange) {
                CustomRangeDialog(
                    from = state.from,
                    to = state.to,
                    onDismiss = { showRange = false },
                    onApply = { from, to ->
                        showRange = false
                        onCustomRange(from, to)
                    },
                )
            }

            if (state.transactionCount == 0) {
                EmptyState(
                    icon = Icons.Outlined.Download,
                    title = Bn.noReportData,
                    message = Bn.noReportHint,
                )
                return@Column
            }

            HisaabCard {
                Text(Bn.summary, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(Bn.todayIncome, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        AmountText(minor = state.totals.incomeMinor, color = Emerald, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(Bn.todayExpense, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        AmountText(minor = state.totals.expenseMinor, color = ExpenseRed, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(10.dp))
                DividerLine()
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("নিট", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                    AmountText(
                        minor = state.totals.netIncomeMinor,
                        color = if (state.totals.netIncomeMinor >= 0) EmeraldDeep else ExpenseRed,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (state.totals.accrualExpenseMinor > 0 || state.totals.interestExpenseMinor > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "এর মধ্যে বাকিতে কেনা ${Money.format(state.totals.accrualExpenseMinor)} ও সুদ/ফি ${Money.format(state.totals.interestExpenseMinor)} আছে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            SectionHeader(title = Bn.monthlyTrend)
            MonthlyBarChart(state.monthly)
            Spacer(Modifier.height(14.dp))

            SectionHeader(title = Bn.categoryBreakdown)
            if (state.categories.isEmpty()) {
                Text(Bn.noReportData, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            } else {
                HisaabCard {
                    val max = state.categories.maxOf { it.totalMinor }.coerceAtLeast(1)
                    state.categories.take(8).forEachIndexed { index, slice ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(slice.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                            AmountText(minor = slice.totalMinor, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Spacer(Modifier.height(4.dp))
                        ProgressTrack(
                            progress = slice.totalMinor.toFloat() / max,
                            color = colorFromArgb(slice.colorArgb),
                        )
                        if (index < state.categories.take(8).size - 1) Spacer(Modifier.height(10.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            SectionHeader(title = Bn.accountBalances)
            HisaabCard(contentPadding = PaddingValues(vertical = 6.dp)) {
                state.accounts.filter { !it.isArchived }.forEachIndexed { index, account ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(account.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                        AmountText(
                            minor = state.balances[account.id] ?: 0L,
                            color = if ((state.balances[account.id] ?: 0L) < 0) ExpenseRed else TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (index < state.accounts.filter { !it.isArchived }.size - 1) DividerLine(Modifier.padding(horizontal = 16.dp))
                }
            }
            if (state.shops.any { (state.shopOutstanding[it.id] ?: 0L) > 0 }) {
                Spacer(Modifier.height(14.dp))
                SectionHeader(title = Bn.shopReport)
                HisaabCard(contentPadding = PaddingValues(vertical = 6.dp)) {
                    state.shops.filter { (state.shopOutstanding[it.id] ?: 0L) > 0 }.forEach { shop ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text(shop.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                            AmountText(minor = state.shopOutstanding[shop.id] ?: 0L, color = ExpenseRed, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            SecondaryButton(
                text = Bn.exportCsv,
                icon = Icons.Outlined.Download,
                onClick = { exportLauncher.launch("hisaabos-report-${state.from}-to-${state.to}.csv") },
            )
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                InfoNote(text = message!!, tone = NoteTone.SUCCESS)
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun MonthlyBarChart(series: List<MonthPoint>) {
    if (series.isEmpty()) {
        Text(Bn.noReportData, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        return
    }
    val max = series.maxOf { maxOf(it.incomeMinor, it.expenseMinor) }.coerceAtLeast(1)
    HisaabCard {
        Row(
            Modifier.fillMaxWidth().height(140.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            series.forEach { point ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Row(
                        modifier = Modifier.height(110.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Box(
                            Modifier
                                .width(9.dp)
                                .height((110 * (point.incomeMinor.toFloat() / max)).dp.coerceAtLeast(2.dp))
                                .clip(RoundedCornerShape(4.dp))
                                .background(Emerald),
                        )
                        Box(
                            Modifier
                                .width(9.dp)
                                .height((110 * (point.expenseMinor.toFloat() / max)).dp.coerceAtLeast(2.dp))
                                .clip(RoundedCornerShape(4.dp))
                                .background(ExpenseRed),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        monthLabel(point.month),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(Emerald, Bn.todayIncome)
            LegendDot(ExpenseRed, Bn.todayExpense)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(10.dp).height(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

private fun monthLabel(month: String): String {
    val value = month.takeLast(2).toIntOrNull() ?: return month
    return BizDate.monthNameBn(value).take(3)
}

@Composable
private fun CustomRangeDialog(
    from: String,
    to: String,
    onDismiss: () -> Unit,
    onApply: (String, String) -> Unit,
) {
    var start by remember { mutableStateOf(from) }
    var end by remember { mutableStateOf(to) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Bn.customRange) },
        text = {
            Column {
                com.shohankhan.hisaabos.ui.components.DateField(label = "শুরু", value = start, onSelect = { start = it })
                Spacer(Modifier.height(8.dp))
                com.shohankhan.hisaabos.ui.components.DateField(label = "শেষ", value = end, onSelect = { end = it })
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onApply(start, end) }) { Text(Bn.apply, color = Emerald) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(Bn.cancel) }
        },
    )
}
