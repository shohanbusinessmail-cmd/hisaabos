package com.shohankhan.hisaabos.ui.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.BudgetEntity
import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.BudgetPeriod
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.report.BudgetProgress
import com.shohankhan.hisaabos.domain.report.ReportEngine
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.CategoryChip
import com.shohankhan.hisaabos.ui.components.DateField
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.ProgressTrack
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.components.SecondaryButton
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.WarningAmber
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class BudgetRow(val budget: BudgetEntity, val progress: BudgetProgress, val categories: List<Long>)

data class BudgetsState(
    val loading: Boolean = true,
    val rows: List<BudgetRow> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val message: String? = null,
)

class BudgetsViewModel(private val repository: HisaabRepository) : ViewModel() {
    private val _state = MutableStateFlow(BudgetsState())
    val state: StateFlow<BudgetsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeBudgets(),
                repository.invalidation("transactions", "budgets"),
            ) { budgets, _ -> budgets }
                .collect { budgets ->
                    val rows = budgets.map { budget ->
                        val to = budget.endDate ?: BizDate.today()
                        val txs = repository.transactionsBetween(budget.startDate, to)
                        val ids = repository.budgetCategories(budget.id).map { it.categoryId }
                        BudgetRow(
                            budget = budget,
                            progress = ReportEngine.budgetProgress(
                                transactions = txs,
                                limitMinor = budget.limitMinor,
                                from = budget.startDate,
                                to = to,
                                categoryIds = ids.toSet(),
                                includeAllCategories = budget.includeAllCategories,
                            ),
                            categories = ids,
                        )
                    }
                    _state.value = _state.value.copy(
                        loading = false,
                        rows = rows,
                        categories = repository.allCategories().filter { it.kind != com.shohankhan.hisaabos.domain.model.CategoryKind.INCOME },
                    )
                }
        }
    }

    fun create(name: String, limitText: String, period: BudgetPeriod, start: String, end: String?, includeAll: Boolean, categoryIds: List<Long>) {
        val limit = Money.parsePositive(limitText)
        if (name.isBlank()) {
            _state.value = _state.value.copy(message = "বাজেটের নাম লিখুন")
            return
        }
        if (limit == null) {
            _state.value = _state.value.copy(message = "সীমার পরিমাণ ঠিক নেই")
            return
        }
        viewModelScope.launch {
            repository.createBudget(name, period, start, end, limit, includeAll, categoryIds, null)
            _state.value = _state.value.copy(message = "বাজেট তৈরি হয়েছে")
        }
    }

    fun archive(id: Long) {
        viewModelScope.launch { repository.archiveBudget(id, true) }
    }
}

@Composable
fun BudgetsScreen(onBack: () -> Unit) {
    val vm = appViewModel { BudgetsViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = Bn.budgets,
            subtitle = "খরচের সীমা ও অগ্রগতি",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showForm = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = Bn.newBudget, tint = Emerald)
                }
            },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (state.message != null) {
                InfoNote(text = state.message!!, tone = NoteTone.INFO)
                Spacer(Modifier.height(8.dp))
            }
            if (state.rows.isEmpty() && !state.loading) {
                EmptyState(
                    icon = Icons.Outlined.BarChart,
                    title = Bn.noBudgets,
                    message = "মাসিক খরচের সীমা ঠিক করুন। কোন ক্যাটাগরি বাজেটের মধ্যে গণ্য হবে তা আপনি বেছে নেবেন।",
                    actionLabel = Bn.newBudget,
                    onAction = { showForm = true },
                )
            }
            SectionHeader(title = "চলমান বাজেট")
            state.rows.forEach { row ->
                val progress = row.progress
                HisaabCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.budget.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                "${BizDate.formatBn(row.budget.startDate)} — ${row.budget.endDate?.let { BizDate.formatBn(it) } ?: "চলবে"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Text("${progress.percentUsed}%", style = MaterialTheme.typography.titleMedium, color = if (progress.isOver) ExpenseRed else EmeraldDeep)
                    }
                    Spacer(Modifier.height(8.dp))
                    ProgressTrack(
                        progress = progress.percentUsed / 100f,
                        color = if (progress.isOver) ExpenseRed else Emerald,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Text("খরচ ${Money.format(progress.spentMinor)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
                        Text("সীমা ${Money.format(progress.limitMinor)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (progress.isOver) "সীমা ছাড়িয়েছে ${Money.format((progress.spentMinor - progress.limitMinor).coerceAtLeast(0))}"
                        else "বাকি ${Money.format(progress.remainingMinor)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (progress.isOver) ExpenseRed else EmeraldDeep,
                    )
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(text = "আর্কাইভ", onClick = { vm.archive(row.budget.id) })
                }
            }
            Spacer(Modifier.height(90.dp))
        }
    }

    if (showForm) {
        var name by remember { mutableStateOf("") }
        var limit by remember { mutableStateOf("") }
        var period by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
        var start by remember { mutableStateOf(BizDate.today()) }
        var end by remember { mutableStateOf(BizDate.today()) }
        var includeAll by remember { mutableStateOf(true) }
        val selected = remember { mutableListOf<Long>() }
        var selectedVersion by remember { mutableStateOf(0) }
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(Bn.newBudget) },
            text = {
                Column(Modifier.height(420.dp).verticalScroll(rememberScrollState())) {
                    HisaabTextField(value = name, onValueChange = { name = it }, label = Bn.budgetHint)
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = limit, onValueChange = { limit = it }, label = Bn.budgetLimit)
                    Spacer(Modifier.height(8.dp))
                    DropdownField(Bn.typeLabel, BudgetPeriod.entries, period, {
                        when (it) {
                            BudgetPeriod.WEEKLY -> "সাপ্তাহিক"
                            BudgetPeriod.MONTHLY -> "মাসিক"
                            BudgetPeriod.CUSTOM -> "নিজের সময়"
                        }
                    }, { period = it })
                    Spacer(Modifier.height(8.dp))
                    DateField("শুরু", start) { start = it }
                    Spacer(Modifier.height(8.dp))
                    DateField("শেষ", end) { end = it }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryChip("সব ক্যাটাগরি", includeAll) { includeAll = true }
                        Spacer(Modifier.height(0.dp))
                        CategoryChip("নির্বাচিত", !includeAll) { includeAll = false }
                    }
                    if (!includeAll) {
                        Spacer(Modifier.height(6.dp))
                        state.categories.forEach { category ->
                            CategoryChip(
                                category.name,
                                selected.contains(category.id) && selectedVersion >= 0,
                            ) {
                                if (selected.contains(category.id)) selected.remove(category.id) else selected.add(category.id)
                                selectedVersion++
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.create(name, limit, period, start, end, includeAll, selected.toList())
                    showForm = false
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text(Bn.cancel) } },
        )
    }
}
