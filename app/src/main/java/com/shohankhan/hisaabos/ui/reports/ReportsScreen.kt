package com.shohankhan.hisaabos.ui.reports

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.CategoryChip
import com.shohankhan.hisaabos.ui.components.DateField
import com.shohankhan.hisaabos.ui.components.DividerLine
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.ProgressTrack
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.components.colorFromArgb
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Divider
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
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
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var showCustom by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val csv = onExportCsv()
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            }.onSuccess { exportMessage = "CSV সংরক্ষণ হয়েছে — যেকোনো স্প্রেডশিটে খুলতে পারবেন" }
                .onFailure { exportMessage = "ফাইল লেখা যায়নি" }
        }
    }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(title = Bn.reportsTitle, subtitle = "আপনার তথ্য থেকেই তৈরি")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    BizDate.Period.TODAY to Bn.periodToday,
                    BizDate.Period.WEEK to Bn.periodWeek,
                    BizDate.Period.MONTH to Bn.periodMonth,
                    BizDate.Period.YEAR to Bn.periodYear,
                    BizDate.Period.ALL to Bn.periodAll,
                ).forEach { (period, label) ->
                    CategoryChip(label, state.period == period) { onPeriodChange(period) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row {
                CategoryChip(Bn.customRange, state.period == BizDate.Period.CUSTOM) { showCustom = true }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${BizDate.formatBn(state.from)} — ${BizDate.formatBn(state.to)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            Spacer(Modifier.height(12.dp))
            if (state.transactionCount == 0) {
                EmptyState(
                    icon = Icons.Outlined.Download,
                    title = Bn.noReportData,
                    message = Bn.noReportHint,
                )
            } else {
                HisaabCard {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(Bn.todayIncome, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = state.totals.incomeMinor, style = MaterialTheme.typography.titleMedium, color = EmeraldDeep)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(Bn.todayExpense, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = state.totals.expenseMinor, style = MaterialTheme.typography.titleMedium, color = ExpenseRed)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("নিট", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = state.totals.netIncomeMinor, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }
                    }
                    if (state.totals.accrualExpenseMinor > 0 || state.totals.interestExpenseMinor > 0) {
                        Spacer(Modifier.height(8.dp))
                        DividerLine()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "এর মধ্যে বাকিতে কেনা ${Money.format(state.totals.accrualExpenseMinor)} এবং সুদ-ফি ${Money.format(state.totals.interestExpenseMinor)} আছে — এগুলো নগদ খরচ নয়।",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                SectionHeader(title = Bn.monthlyTrend)
                HisaabCard {
                    MonthlyBars(state.monthly)
                }

                Spacer(Modifier.height(16.dp))
                SectionHeader(title = Bn.categoryBreakdown)
                if (state.categories.isEmpty()) {
                    InfoNote(text = "এই সময়ে কোনো ক্যাটাগরিভিত্তিক খরচ নেই।")
                } else {
                    val max = state.categories.maxOf { it.totalMinor }.coerceAtLeast(1)
                    HisaabCard {
                        state.categories.take(6).forEachIndexed { index, slice ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(slice.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                                AmountText(minor = slice.totalMinor, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            Spacer(Modifier.height(4.dp))
                            ProgressTrack(
                                progress = slice.totalMinor.toFloat() / max,
                                color = colorFromArgb(slice.colorArgb),
                            )
                            if (index != state.categories.take(6).lastIndex) Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                SectionHeader(title = Bn.accountBalances)
                HisaabCard {
                    state.accounts.filter { !it.isArchived }.forEach { account ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(account.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                            AmountText(
                                minor = state.balances[account.id] ?: 0,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if ((state.balances[account.id] ?: 0) < 0) ExpenseRed else TextPrimary,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                SectionHeader(title = "দোকানের বাকি")
                if (state.shops.isEmpty()) {
                    InfoNote(text = "এখনো কোনো দোকান যোগ করা হয়নি।")
                } else {
                    HisaabCard {
                        state.shops.forEach { shop ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(shop.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                                AmountText(
                                    minor = state.shopOutstanding[shop.id] ?: 0,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if ((state.shopOutstanding[shop.id] ?: 0) > 0) ExpenseRed else EmeraldDeep,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "মোট দোকান বাকি ${Money.format(state.shopOutstanding.values.sum())} — এটি দায়, খরচ নয়।",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader(title = Bn.exportData)
            InfoNote(text = "CSV ফাইলে বর্তমান সময়ের সব লেনদেন থাকবে — তারিখ, ধরন, টাকা, হিসাব, ক্যাটাগরি, দোকান ও নোট।")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.shohankhan.hisaabos.ui.components.PrimaryButton(
                    text = Bn.exportCsv,
                    icon = Icons.Outlined.Download,
                    onClick = { exportLauncher.launch("hisaabos-report-${state.from}-to-${state.to}.csv") },
                    modifier = Modifier.weight(1f),
                )
            }
            exportMessage?.let {
                Spacer(Modifier.height(8.dp))
                InfoNote(text = it, tone = NoteTone.SUCCESS)
            }
            Spacer(Modifier.height(96.dp))
        }
    }

    if (showCustom) {
        var from by remember(state.from) { mutableStateOf(state.from) }
        var to by remember(state.to) { mutableStateOf(state.to) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustom = false },
            title = { Text(Bn.customRange) },
            text = {
                Column {
                    DateField(label = "শুরু", value = from, onSelect = { from = it })
                    Spacer(Modifier.height(8.dp))
                    DateField(label = "শেষ", value = to, onSelect = { to = it })
                    if (from > to) {
                        Spacer(Modifier.height(6.dp))
                        Text("শুরুর তারিখ শেষের চেয়ে বড় হতে পারে না", color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    if (from <= to) {
                        onCustomRange(from, to)
                        showCustom = false
                    }
                }) { Text(Bn.apply, color = Emerald) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCustom = false }) { Text(Bn.cancel) }
            },
        )
    }
}

@Composable
private fun MonthlyBars(points: List<com.shohankhan.hisaabos.domain.report.MonthPoint>) {
    if (points.isEmpty()) {
        Text(Bn.noReportData, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        return
    }
    val max = points.maxOf { maxOf(it.incomeMinor, it.expenseMinor) }.coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(160.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEach { point ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(
                        Modifier
                            .width(8.dp)
                            .height((96 * (point.incomeMinor.toFloat() / max)).dp.coerceAtLeast(2.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(Emerald),
                    )
                    Box(
                        Modifier
                            .width(8.dp)
                            .height((96 * (point.expenseMinor.toFloat() / max)).dp.coerceAtLeast(2.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(ExpenseRed),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    BizDate.monthNameBn(point.month.takeLast(2).toIntOrNull() ?: 1).take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Legend(Emerald, Bn.todayIncome)
        Legend(ExpenseRed, Bn.todayExpense)
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(10.dp).height(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
