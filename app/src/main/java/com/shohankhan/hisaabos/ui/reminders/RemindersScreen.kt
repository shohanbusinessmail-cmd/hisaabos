package com.shohankhan.hisaabos.ui.reminders

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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import com.shohankhan.hisaabos.data.db.RecurringRuleEntity
import com.shohankhan.hisaabos.data.db.ReminderEntity
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.RecurrenceFrequency
import com.shohankhan.hisaabos.domain.model.TxType
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
import com.shohankhan.hisaabos.ui.components.Pill
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

data class RemindersState(
    val loading: Boolean = true,
    val reminders: List<ReminderEntity> = emptyList(),
    val recurring: List<RecurringRuleEntity> = emptyList(),
    val message: String? = null,
)

class RemindersViewModel(private val repository: HisaabRepository) : ViewModel() {
    private val _state = MutableStateFlow(RemindersState())
    val state: StateFlow<RemindersState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeReminders(),
                repository.observeRecurring(),
                repository.invalidation("reminders", "recurring_rules"),
            ) { reminders, recurring, _ -> reminders to recurring }
                .collect { (reminders, recurring) ->
                    _state.value = _state.value.copy(
                        loading = false,
                        reminders = reminders.sortedWith(compareBy({ it.isDone }, { it.dueDate })),
                        recurring = recurring,
                    )
                }
        }
    }

    fun addReminder(title: String, date: String, amountText: String?) {
        if (title.isBlank()) {
            _state.value = _state.value.copy(message = "কী মনে করাতে হবে লিখুন")
            return
        }
        val amount = amountText?.takeIf { it.isNotBlank() }?.let { Money.parsePositive(it) }
        viewModelScope.launch {
            repository.createReminder(
                ReminderEntity(
                    title = title.trim(),
                    dueDate = date,
                    amountMinor = amount,
                ),
            )
            _state.value = _state.value.copy(message = "মনে করানো যোগ হয়েছে")
        }
    }

    fun setDone(id: Long, done: Boolean) {
        viewModelScope.launch { repository.setReminderDone(id, done) }
    }

    fun addRecurring(title: String, amountText: String, type: TxType, frequency: RecurrenceFrequency, nextRun: String) {
        val amount = Money.parsePositive(amountText)
        if (title.isBlank() || amount == null) {
            _state.value = _state.value.copy(message = "নিয়মিত লেনদেনের নাম ও টাকার পরিমাণ দিন")
            return
        }
        viewModelScope.launch {
            repository.createRecurring(
                RecurringRuleEntity(
                    title = title.trim(),
                    type = type,
                    amountMinor = amount,
                    frequency = frequency,
                    nextRunDate = nextRun,
                ),
            )
            _state.value = _state.value.copy(message = "নিয়মিত লেনদেন তৈরি হয়েছে — অ্যাপ নিজে থেকে টাকা লিখবে না, শুধু মনে করাবে।")
        }
    }

    fun toggleRecurring(id: Long, active: Boolean) {
        viewModelScope.launch { repository.setRecurringActive(id, active) }
    }
}

@Composable
fun RemindersScreen(onBack: () -> Unit) {
    val vm = appViewModel { RemindersViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showReminder by remember { mutableStateOf(false) }
    var showRecurring by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = Bn.reminders,
            subtitle = "সব মনে করানো ফোনেই থাকে",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showReminder = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = Bn.newReminder, tint = Emerald)
                }
            },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            InfoNote(text = "অ্যাপ বন্ধ থাকলেও প্রতিদিন একবার ফোনে মনে করানো দেখানো হয়। কোনো টাকা নিজে থেকে কাটা বা জমা হয় না।")
            if (state.message != null) {
                Spacer(Modifier.height(8.dp))
                InfoNote(text = state.message!!, tone = NoteTone.SUCCESS)
            }
            Spacer(Modifier.height(12.dp))
            SectionHeader(title = "মনে করানো")
            if (state.reminders.isEmpty() && !state.loading) {
                EmptyState(
                    icon = Icons.Outlined.Notifications,
                    title = Bn.noReminders,
                    message = "কিস্তি, বিল বা জমার তারিখ যোগ করুন — সময় হলে ফোনে জানানো হবে।",
                    actionLabel = Bn.newReminder,
                    onAction = { showReminder = true },
                )
            }
            state.reminders.forEach { reminder ->
                HisaabCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = reminder.isDone, onCheckedChange = { vm.setDone(reminder.id, it) })
                        Column(Modifier.weight(1f)) {
                            Text(reminder.title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(
                                BizDate.formatBn(reminder.dueDate, withWeekday = true),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        reminder.amountMinor?.let {
                            AmountText(minor = it, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            SectionHeader(title = Bn.recurring)
            InfoNote(text = "নিয়মিত লেনদেনের টেমপ্লেট শুধু মনে করায়; টাকা যোগ বা বিয়োগ আপনি নিজে নিশ্চিত করবেন।")
            Spacer(Modifier.height(8.dp))
            state.recurring.forEach { rule ->
                HisaabCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(rule.title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(
                                "${Bn.txType(rule.type)} · ${frequencyLabel(rule.frequency)} · পরবর্তী ${BizDate.formatBn(rule.nextRunDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        AmountText(minor = rule.amountMinor, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Pill(if (rule.isActive) "চালু" else "বন্ধ", if (rule.isActive) EmeraldDeep else ExpenseRed)
                    }
                    Spacer(Modifier.height(6.dp))
                    SecondaryButton(
                        text = if (rule.isActive) "বন্ধ করুন" else "চালু করুন",
                        onClick = { vm.toggleRecurring(rule.id, !rule.isActive) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = "নতুন নিয়মিত লেনদেন", icon = Icons.Outlined.Repeat, onClick = { showRecurring = true })
            Spacer(Modifier.height(90.dp))
        }
    }

    if (showReminder) {
        var title by remember { mutableStateOf("") }
        var date by remember { mutableStateOf(BizDate.today()) }
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReminder = false },
            title = { Text(Bn.newReminder) },
            text = {
                Column {
                    HisaabTextField(value = title, onValueChange = { title = it }, label = Bn.reminderTitle)
                    Spacer(Modifier.height(8.dp))
                    DateField(Bn.reminderDate, date) { date = it }
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = amount, onValueChange = { amount = it }, label = "টাকার পরিমাণ (${Bn.optional})")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addReminder(title, date, amount)
                    showReminder = false
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { showReminder = false }) { Text(Bn.cancel) } },
        )
    }

    if (showRecurring) {
        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(TxType.EXPENSE) }
        var frequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }
        var next by remember { mutableStateOf(BizDate.today()) }
        AlertDialog(
            onDismissRequest = { showRecurring = false },
            title = { Text("নতুন নিয়মিত লেনদেন") },
            text = {
                Column {
                    HisaabTextField(value = title, onValueChange = { title = it }, label = "নাম (যেমন: বাসা ভাড়া)")
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = amount, onValueChange = { amount = it }, label = Bn.howMuch)
                    Spacer(Modifier.height(8.dp))
                    DropdownField(Bn.typeLabel, listOf(TxType.EXPENSE, TxType.INCOME), type, { Bn.txType(it) }, { type = it })
                    Spacer(Modifier.height(8.dp))
                    DropdownField("কতবার", RecurrenceFrequency.entries, frequency, { frequencyLabel(it) }, { frequency = it })
                    Spacer(Modifier.height(8.dp))
                    DateField("পরবর্তী তারিখ", next) { next = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addRecurring(title, amount, type, frequency, next)
                    showRecurring = false
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { showRecurring = false }) { Text(Bn.cancel) } },
        )
    }
}

private fun frequencyLabel(frequency: RecurrenceFrequency): String = when (frequency) {
    RecurrenceFrequency.DAILY -> "প্রতিদিন"
    RecurrenceFrequency.WEEKLY -> "প্রতি সপ্তাহ"
    RecurrenceFrequency.MONTHLY -> "প্রতি মাস"
}
