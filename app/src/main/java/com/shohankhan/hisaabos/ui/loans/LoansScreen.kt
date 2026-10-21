package com.shohankhan.hisaabos.ui.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.InterestModel
import com.shohankhan.hisaabos.domain.model.LoanKind
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.RepaymentFrequency
import com.shohankhan.hisaabos.ui.components.AmountField
import com.shohankhan.hisaabos.ui.components.AmountText
import com.shohankhan.hisaabos.ui.components.DateField
import com.shohankhan.hisaabos.ui.components.DropdownField
import com.shohankhan.hisaabos.ui.components.EmptyState
import com.shohankhan.hisaabos.ui.components.HisaabCard
import com.shohankhan.hisaabos.ui.components.HisaabTextField
import com.shohankhan.hisaabos.ui.components.IconBadge
import com.shohankhan.hisaabos.ui.components.InfoNote
import com.shohankhan.hisaabos.ui.components.NoteTone
import com.shohankhan.hisaabos.ui.components.PageHeader
import com.shohankhan.hisaabos.ui.components.Pill
import com.shohankhan.hisaabos.ui.components.SectionHeader
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.AppBackground
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldDeep
import com.shohankhan.hisaabos.ui.theme.ExpenseRed
import com.shohankhan.hisaabos.ui.theme.TextPrimary
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import androidx.compose.runtime.LaunchedEffect

private fun loanKindLabel(kind: LoanKind) = when (kind) {
    LoanKind.NGO -> Bn.ngoLoan
    LoanKind.BANK -> Bn.bankLoan
}

private fun modelLabel(model: InterestModel) = when (model) {
    InterestModel.FLAT_TOTAL -> "নির্দিষ্ট মোট (এনজিও)"
    InterestModel.REDUCING_BALANCE -> "কমতিরোধী সুদ"
    InterestModel.ZERO_INTEREST -> "সুদমুক্ত"
    InterestModel.MANUAL -> "নিজের কিস্তি সূচি"
}

private fun frequencyLabel(frequency: RepaymentFrequency) = when (frequency) {
    RepaymentFrequency.WEEKLY -> "সাপ্তাহিক"
    RepaymentFrequency.BIWEEKLY -> "পাক্ষিক"
    RepaymentFrequency.MONTHLY -> "মাসিক"
    RepaymentFrequency.ONCE -> "একবারে"
}

@Composable
fun LoansScreen(onBack: () -> Unit, onOpenLoan: (Long) -> Unit) {
    val vm = appViewModel { LoansViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val formVm = appViewModel { LoanFormViewModel(it.repository) }
    val formState by formVm.state.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }
    var newId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(newId) {
        newId?.let {
            showForm = false
            newId = null
            onOpenLoan(it)
        }
    }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        PageHeader(
            title = Bn.loansTitle,
            subtitle = "এনজিও ও ব্যাংক ঋণের হিসাব",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showForm = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = Bn.newLoan, tint = Emerald)
                }
            },
        )
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                HisaabCard {
                    Text("মোট বাকি ঋণ", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    AmountText(minor = state.totalOutstandingMinor, style = MaterialTheme.typography.headlineSmall, color = ExpenseRed)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "ঋণ নেওয়া আয় নয়; শুধু মূল টাকার কিস্তি পরিশোধ দায় কমায়, আর সুদ-ফি খরচ।",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                SectionHeader(title = "${state.loans.count { !it.loan.isClosed }}টি সক্রিয় ঋণ")
            }
            if (state.loans.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.AccountBalance,
                        title = Bn.noLoans,
                        message = "এনজিও বা ব্যাংকের ঋণ যোগ করুন। কিস্তির সূচি নিজে থেকেই তৈরি হবে, চাইলে হাতে সাজাতেও পারবেন।",
                        actionLabel = Bn.newLoan,
                        onAction = { showForm = true },
                    )
                }
            }
            items(state.loans, key = { it.loan.id }) { row ->
                HisaabCard(
                    modifier = Modifier.padding(bottom = 8.dp).clickable { onOpenLoan(row.loan.id) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Outlined.AccountBalance, size = 40)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(row.loan.institution, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                "${loanKindLabel(row.loan.kind)} · ${modelLabel(row.loan.model)}" +
                                    (row.nextDue?.let { " · পরের কিস্তি ${BizDate.formatBn(it.dueDate)}" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Pill(if (row.loan.isClosed) "বন্ধ" else "চলছে", if (row.loan.isClosed) EmeraldDeep else ExpenseRed)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("বাকি মূল", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = row.outstandingPrincipalMinor, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("মোট বাকি", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = row.outstandingTotalMinor, style = MaterialTheme.typography.bodyMedium, color = ExpenseRed)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("পরিশোধিত", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            AmountText(minor = row.paid.amountMinor, style = MaterialTheme.typography.bodyMedium, color = EmeraldDeep)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showForm) {
        var kind by remember { mutableStateOf(LoanKind.NGO) }
        var institution by remember { mutableStateOf("") }
        var principal by remember { mutableStateOf("") }
        var model by remember { mutableStateOf(InterestModel.FLAT_TOTAL) }
        var rate by remember { mutableStateOf("") }
        var totalRepayable by remember { mutableStateOf("") }
        var interest by remember { mutableStateOf("") }
        var fee by remember { mutableStateOf("") }
        var count by remember { mutableStateOf("10") }
        var frequency by remember { mutableStateOf(RepaymentFrequency.MONTHLY) }
        var firstDue by remember { mutableStateOf(BizDate.today()) }
        var disbursement by remember { mutableStateOf(BizDate.today()) }
        var accountId by remember { mutableStateOf<Long?>(formState.accounts.firstOrNull()?.id) }
        var notes by remember { mutableStateOf("") }
        LaunchedEffect(formState.accounts) {
            if (accountId == null) accountId = formState.accounts.firstOrNull()?.id
        }

        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(Bn.newLoan) },
            text = {
                Column(Modifier.height(460.dp).verticalScroll(rememberScrollState())) {
                    DropdownField("ঋণের ধরন", LoanKind.entries, kind, { loanKindLabel(it) }, { kind = it })
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = institution, onValueChange = { institution = it }, label = "প্রতিষ্ঠানের নাম")
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = principal, onValueChange = { principal = it }, label = "ঋণের পরিমাণ")
                    Spacer(Modifier.height(8.dp))
                    DropdownField("সুদের ধরন", InterestModel.entries, model, { modelLabel(it) }, { model = it })
                    Spacer(Modifier.height(8.dp))
                    DropdownField("কিস্তির ধরন", RepaymentFrequency.entries, frequency, { frequencyLabel(it) }, { frequency = it })
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = count, onValueChange = { count = it }, label = "কিস্তির সংখ্যা")
                    Spacer(Modifier.height(8.dp))
                    if (model == InterestModel.REDUCING_BALANCE) {
                        HisaabTextField(value = rate, onValueChange = { rate = it }, label = "বার্ষিক সুদের হার (%)")
                        Spacer(Modifier.height(8.dp))
                    }
                    if (model == InterestModel.FLAT_TOTAL) {
                        AmountField(value = totalRepayable, onValueChange = { totalRepayable = it }, label = "মোট পরিশোধযোগ্য (${Bn.optional})")
                        Spacer(Modifier.height(8.dp))
                    }
                    AmountField(value = interest, onValueChange = { interest = it }, label = "মোট সুদ (${Bn.optional})")
                    Spacer(Modifier.height(8.dp))
                    AmountField(value = fee, onValueChange = { fee = it }, label = "ফি (${Bn.optional})")
                    Spacer(Modifier.height(8.dp))
                    DateField("প্রথম কিস্তির তারিখ", firstDue, { firstDue = it })
                    Spacer(Modifier.height(8.dp))
                    DateField("ঋণ নেওয়ার তারিখ", disbursement, { disbursement = it })
                    Spacer(Modifier.height(8.dp))
                    DropdownField(Bn.toWhichAccount, formState.accounts, formState.accounts.firstOrNull { it.id == accountId }, { it.name }, { accountId = it.id })
                    Spacer(Modifier.height(8.dp))
                    HisaabTextField(value = notes, onValueChange = { notes = it }, label = Bn.noteOptional)
                    if (formState.message != null) {
                        Spacer(Modifier.height(8.dp))
                        InfoNote(
                            text = formState.message!!,
                            tone = if (formState.createdId != null) NoteTone.SUCCESS else NoteTone.ERROR,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    formVm.create(
                        kind = kind,
                        institution = institution,
                        principalText = principal,
                        model = model,
                        annualRateText = rate,
                        totalRepayableText = totalRepayable,
                        interestText = interest,
                        feeText = fee,
                        installmentCountText = count,
                        frequency = frequency,
                        firstDueDate = firstDue,
                        disbursementDate = disbursement,
                        accountId = accountId,
                        notes = notes,
                    )
                }) { Text(Bn.save, color = Emerald) }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text(Bn.cancel) } },
        )
        LaunchedEffect(formState.createdId) {
            formState.createdId?.let { newId = it }
        }
    }
}
