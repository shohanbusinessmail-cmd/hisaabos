package com.shohankhan.hisaabos.ui.emi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.EmiEntity
import com.shohankhan.hisaabos.data.db.ScheduleItemEntity
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.domain.ledger.Settlement
import com.shohankhan.hisaabos.domain.ledger.WriteResult
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.ScheduleOwner
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.DateField
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.KeyValueRow
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.Pill
import com.shohankhan.hisaabos.ui.components.PrimaryButton
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

data class EmiDetailState(
    val loading: Boolean = true,
    val emi: EmiEntity? = null,
    val schedule: List<ScheduleItemEntity> = emptyList(),
    val paidIndexes: Set<Int> = emptySet(),
    val settlement: Settlement = Settlement(),
    val accounts: List<AccountEntity> = emptyList(),
    val next: ScheduleItemEntity? = null,
    val message: String? = null,
) {
    val outstandingMinor: Long
        get() = ((emi?.totalFinancedPriceMinor ?: 0) - settlement.amountMinor).coerceAtLeast(0)
}

class EmiDetailViewModel(
    private val repository: HisaabRepository,
    private val emiId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(EmiDetailState())
    val state: StateFlow<EmiDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val accounts = repository.activeAccounts()
            combine(
                repository.observeEmi(emiId),
                repository.observeSchedule(ScheduleOwner.EMI, emiId),
                repository.invalidation("transactions", "emi_purchases"),
            ) { emi, schedule, _ -> emi to schedule }
                .collect { (emi, schedule) ->
                    val paidIndexes = repository.forLink(LinkKind.EMI, emiId).mapNotNull { it.installmentIndex }.toSet()
                    val settlement = repository.service.settlementFor(LinkKind.EMI, emiId)
                    _state.value = _state.value.copy(
                        loading = false,
                        emi = emi,
                        schedule = schedule,
                        paidIndexes = paidIndexes,
                        settlement = settlement,
                        accounts = accounts,
                        next = schedule.firstOrNull { it.sequence !in paidIndexes },
                    )
                }
        }
    }

    fun pay(amountText: String, interestText: String, date: String, accountId: Long?, installmentIndex: Int?) {
        val amount = Money.parsePositive(amountText)
        if (amount == null) {
            _state.value = _state.value.copy(message = "কিস্তির পরিমাণ ঠিক নেই")
            return
        }
        val interest = if (interestText.isBlank()) 0L else Money.parse(interestText) ?: -1
        if (interest < 0 || interest > amount) {
            _state.value = _state.value.copy(message = "সুদের অংশ ঠিক নেই")
            return
        }
        viewModelScope.launch {
            val result = repository.service.payEmi(
                emiId = emiId,
                amountMinor = amount,
                principalMinor = amount - interest,
                interestMinor = interest,
                feeMinor = 0,
                occurredAt = System.currentTimeMillis(),
                businessDate = date,
                accountId = accountId,
                installmentIndex = installmentIndex,
            )
            _state.value = _state.value.copy(
                message = when (result) {
                    is WriteResult.Success -> "কিস্তি সংরক্ষণ হয়েছে"
                    is WriteResult.Invalid -> result.message
                    is WriteResult.Failed -> result.message
                },
            )
        }
    }
}

@Composable
fun EmiDetailScreen(emiId: Long, onBack: () -> Unit) {
    val vm = appViewModel(key = "emi-$emiId") { container -> EmiDetailViewModel(container.repository, emiId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showPay by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(title = state.emi?.productName ?: Bn.emiPurchase, subtitle = state.emi?.merchant, onBack = onBack)
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                HisaabCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("বাকি", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                        Pill(if (state.emi?.isSettled == true) "শেষ" else "চলছে", if (state.emi?.isSettled == true) EmeraldDeep else ExpenseRed)
                    }
                    AmountText(minor = state.outstandingMinor, style = MaterialTheme.typography.headlineMedium, color = ExpenseRed)
                    Spacer(Modifier.height(8.dp))
                    KeyValueRow("মোট মূল্য", Money.formatPlain(state.emi?.totalFinancedPriceMinor ?: 0))
                    KeyValueRow("নগদ মূল্য", Money.formatPlain(state.emi?.cashPriceMinor ?: 0))
                    KeyValueRow("ডাউন পেমেন্ট", Money.formatPlain(state.emi?.downPaymentMinor ?: 0))
                    KeyValueRow("সুদ", Money.formatPlain(state.emi?.interestMinor ?: 0))
                    KeyValueRow("কিস্তি", "${state.emi?.installmentCount ?: 0}টি × ${Money.format(state.emi?.installmentAmountMinor ?: 0)}")
                    KeyValueRow("পরিশোধিত", Money.formatPlain(state.settlement.amountMinor))
                    Spacer(Modifier.height(10.dp))
                    if (state.emi?.isSettled == false) {
                        PrimaryButton(
                            text = if (state.next != null) "কিস্তি পরিশোধ (${Money.format(state.next!!.amountMinor)})" else "কিস্তি পরিশোধ",
                            onClick = { showPay = true },
                        )
                    }
                }
                if (state.message != null) {
                    Spacer(Modifier.height(8.dp))
                    InfoNote(text = state.message!!, tone = NoteTone.SUCCESS)
                }
                Spacer(Modifier.height(14.dp))
                SectionHeader(title = "কিস্তির সূচি")
            }
            if (state.schedule.isEmpty()) {
                item { InfoNote(text = "এই চুক্তির জন্য আলাদা সূচি নেই।") }
            }
            items(state.schedule, key = { it.id }) { row ->
                val paid = row.sequence in state.paidIndexes
                HisaabCard(modifier = Modifier.padding(bottom = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${row.sequence} নম্বর কিস্তি", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(BizDate.formatBn(row.dueDate), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        AmountText(minor = row.amountMinor, style = MaterialTheme.typography.titleSmall, color = if (paid) EmeraldDeep else TextPrimary)
                    }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showPay) {
        val next = state.next
        var amount by remember { mutableStateOf(next?.amountMinor?.let { Money.formatAmountOnly(it).replace(",", "") } ?: (state.emi?.installmentAmountMinor ?: 0).let { Money.formatAmountOnly(it).replace(",", "") }) }
        var interest by remember { mutableStateOf("0") }
        var date by remember { mutableStateOf(BizDate.today()) }
        var accountId by remember { mutableStateOf<Long?>(state.accounts.firstOrNull()?.id) }
        AlertDialog(
            onDismissRequest = { showPay = false },
            title = { Text(Bn.payInstallment) },
            text = {
                Column {
                    AmountField(value = amount, onValueChange = { amount = it }, label = "কিস্তির পরিমাণ")
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = interest, onValueChange = { interest = it }, label = "এর মধ্যে সুদ")
                    Spacer(Modifier.height(8.dp))
                    DropdownField(Bn.fromWhichAccount, state.accounts, state.accounts.firstOrNull { it.id == accountId }, { it.name }, { accountId = it.id })
                    Spacer(Modifier.height(8.dp))
                    DateField(Bn.date, date) { date = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.pay(amount, interest, date, accountId, next?.sequence)
                    showPay = false
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { showPay = false }) { Text(Bn.cancel) } },
        )
    }
}
