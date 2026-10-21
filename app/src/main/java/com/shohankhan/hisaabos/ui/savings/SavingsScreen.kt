package com.shohankhan.hisaabos.ui.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Savings
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.SavingsPlanEntity
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.domain.ledger.SavingsPlanInput
import com.shohankhan.hisaabos.domain.ledger.WriteResult
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.SavingsKind
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.DateField
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.PrimaryButton
import com.shohankhan.hisaabos.ui.components.ProgressTrack
import com.shohankhan.hisaabos.ui.components.SecondaryButton
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SavingsRow(val plan: SavingsPlanEntity, val balanceMinor: Long) {
    val progress: Int
        get() {
            val target = plan.targetAmountMinor ?: return 0
            if (target <= 0) return 0
            return ((balanceMinor * 100) / target).toInt().coerceIn(0, 100)
        }
}

data class SavingsState(
    val loading: Boolean = true,
    val plans: List<SavingsRow> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val totalMinor: Long = 0,
    val message: String? = null,
)

class SavingsViewModel(private val repository: HisaabRepository) : ViewModel() {
    private val _state = MutableStateFlow(SavingsState())
    val state: StateFlow<SavingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeSavingsPlans(),
                repository.invalidation("transactions", "savings_plans", "accounts"),
            ) { plans, _ -> plans }
                .collect { plans ->
                    val balances = repository.balances()
                    val rows = plans.map { SavingsRow(it, balances[it.accountId] ?: 0) }
                    _state.value = _state.value.copy(
                        loading = false,
                        plans = rows,
                        accounts = repository.activeAccounts(),
                        totalMinor = rows.sumOf { it.balanceMinor },
                    )
                }
        }
    }

    fun create(name: String, targetText: String, targetDate: String?, kind: SavingsKind, note: String?) {
        if (name.isBlank()) {
            _state.value = _state.value.copy(message = "সঞ্চয়ের নাম লিখুন")
            return
        }
        val target = if (targetText.isBlank()) null else Money.parsePositive(targetText)
        if (targetText.isNotBlank() && target == null) {
            _state.value = _state.value.copy(message = "লক্ষ্যমাত্রা ঠিক নেই")
            return
        }
        viewModelScope.launch {
            val result = repository.service.createSavingsPlan(
                SavingsPlanInput(name = name, kind = kind, targetAmountMinor = target, targetDate = targetDate, note = note?.takeIf { it.isNotBlank() }),
            )
            _state.value = _state.value.copy(
                message = when (result) {
                    is WriteResult.Success -> "সঞ্চয় পরিকল্পনা তৈরি হয়েছে"
                    is WriteResult.Invalid -> result.message
                    is WriteResult.Failed -> result.message
                },
            )
        }
    }

    fun move(planId: Long, deposit: Boolean, amountText: String, date: String, otherAccountId: Long?) {
        val amount = Money.parsePositive(amountText)
        if (amount == null) {
            _state.value = _state.value.copy(message = "টাকার পরিমাণ ঠিক নেই")
            return
        }
        viewModelScope.launch {
            val result = repository.service.savingsMove(
                planId = planId,
                deposit = deposit,
                amountMinor = amount,
                occurredAt = System.currentTimeMillis(),
                businessDate = date,
                otherAccountId = otherAccountId,
            )
            _state.value = _state.value.copy(
                message = when (result) {
                    is WriteResult.Success -> if (deposit) "জমা হয়েছে" else "উত্তোলন হয়েছে"
                    is WriteResult.Invalid -> result.message
                    is WriteResult.Failed -> result.message
                },
            )
        }
    }
}

@Composable
fun SavingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { SavingsViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var moveTarget by remember { mutableStateOf<Triple<Long, String, Boolean>?>(null) }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = Bn.savings,
            subtitle = "লক্ষ্য ও জমার হিসাব",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showCreate = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = Bn.newSavings, tint = Emerald)
                }
            },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            HisaabCard {
                Text("মোট সঞ্চয়", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                AmountText(minor = state.totalMinor, style = MaterialTheme.typography.headlineSmall, color = EmeraldDeep)
                Spacer(Modifier.height(4.dp))
                Text(
                    "সঞ্চয়ের টাকা সরানো মানে এক হিসাব থেকে অন্য হিসাবে যাওয়া — খরচ নয়।",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            if (state.message != null) {
                Spacer(Modifier.height(8.dp))
                InfoNote(text = state.message!!, tone = NoteTone.INFO)
            }
            Spacer(Modifier.height(14.dp))
            SectionHeader(title = "সঞ্চয় পরিকল্পনা")
            if (state.plans.isEmpty() && !state.loading) {
                EmptyState(
                    icon = Icons.Outlined.Savings,
                    title = Bn.noSavings,
                    message = "মাসিক সঞ্চয়, ঈদ ফান্ড বা জরুরি তহবিলের জন্য একটি লক্ষ্য ঠিক করুন।",
                    actionLabel = Bn.newSavings,
                    onAction = { showCreate = true },
                )
            }
            state.plans.forEach { row ->
                HisaabCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.plan.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                row.plan.targetAmountMinor?.let { "লক্ষ্য ${Money.format(it)}" } ?: "লক্ষ্য নির্ধারিত নেই",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        AmountText(minor = row.balanceMinor, style = MaterialTheme.typography.titleMedium, color = EmeraldDeep)
                    }
                    if (row.plan.targetAmountMinor != null) {
                        Spacer(Modifier.height(8.dp))
                        ProgressTrack(progress = row.progress / 100f)
                        Spacer(Modifier.height(4.dp))
                        Row {
                            Text("${row.progress}% সম্পন্ন", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(1f))
                            Text(
                                "বাকি ${Money.format((row.plan.targetAmountMinor - row.balanceMinor).coerceAtLeast(0))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            text = "জমা",
                            onClick = { moveTarget = Triple(row.plan.id, row.plan.name, true) },
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = "উত্তোলন",
                            onClick = { moveTarget = Triple(row.plan.id, row.plan.name, false) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(90.dp))
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("") }
        var targetDate by remember { mutableStateOf(BizDate.today()) }
        var kind by remember { mutableStateOf(SavingsKind.CASH) }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(Bn.newSavings) },
            text = {
                Column {
                    HisaabTextField(value = name, onValueChange = { name = it }, label = Bn.savingsTarget)
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = target, onValueChange = { target = it }, label = "লক্ষ্যমাত্রা (${Bn.optional})")
                    Spacer(Modifier.height(8.dp))
                    DateField("লক্ষ্যের তারিখ", targetDate, onSelect = { targetDate = it })
                    Spacer(Modifier.height(8.dp))
                    DropdownField("সঞ্চয়ের ধরন", SavingsKind.entries, kind, { savingsKindLabel(it) }, { kind = it })
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = note, onValueChange = { note = it }, label = Bn.noteOptional)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.create(name, target, targetDate, kind, note)
                    showCreate = false
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text(Bn.cancel) } },
        )
    }

    moveTarget?.let { (planId, name, deposit) ->
        var amount by remember { mutableStateOf("") }
        var date by remember { mutableStateOf(BizDate.today()) }
        var accountId by remember { mutableStateOf<Long?>(state.accounts.firstOrNull()?.id) }
        AlertDialog(
            onDismissRequest = { moveTarget = null },
            title = { Text(if (deposit) "সঞ্চয়ে জমা — $name" else "সঞ্চয় থেকে উত্তোলন — $name") },
            text = {
                Column {
                    AmountField(value = amount, onValueChange = { amount = it }, label = Bn.howMuch)
                    Spacer(Modifier.height(8.dp))
                    DropdownField(
                        label = if (deposit) Bn.fromWhichAccount else Bn.toWhichAccount,
                        options = state.accounts,
                        selected = state.accounts.firstOrNull { it.id == accountId },
                        optionLabel = { it.name },
                        onSelect = { accountId = it.id },
                    )
                    Spacer(Modifier.height(8.dp))
                    DateField(Bn.date, date, onSelect = { date = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.move(planId, deposit, amount, date, accountId)
                    moveTarget = null
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { moveTarget = null }) { Text(Bn.cancel) } },
        )
    }
}

private fun savingsKindLabel(kind: SavingsKind): String = when (kind) {
    SavingsKind.CASH -> "নগদ"
    SavingsKind.BANK -> "ব্যাংক"
    SavingsKind.NGO -> "এনজিও"
    SavingsKind.RECURRING -> "ডিপিএস"
    SavingsKind.GOAL -> "লক্ষ্য"
    SavingsKind.OTHER -> "অন্যান্য"
}
