package com.shohankhan.hisaabos.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.EmiEntity
import com.shohankhan.hisaabos.data.db.LoanEntity
import com.shohankhan.hisaabos.data.db.ScheduleItemEntity
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.ledger.EmiInput
import com.shohankhan.hisaabos.domain.ledger.LoanInput
import com.shohankhan.hisaabos.domain.ledger.Settlement
import com.shohankhan.hisaabos.domain.ledger.WriteResult
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.InterestModel
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.LoanKind
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.RepaymentFrequency
import com.shohankhan.hisaabos.domain.model.ScheduleOwner
import com.shohankhan.hisaabos.domain.model.TxType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class LoanRow(
    val loan: LoanEntity,
    val paid: Settlement,
    val nextDue: ScheduleItemEntity?,
) {
    val outstandingPrincipalMinor: Long
        get() = (loan.principalMinor - paid.principalMinor).coerceAtLeast(0)
    val outstandingTotalMinor: Long
        get() = ((loan.totalRepayableMinor ?: loan.principalMinor) - paid.amountMinor).coerceAtLeast(0)
}

data class LoansState(
    val loading: Boolean = true,
    val loans: List<LoanRow> = emptyList(),
    val totalOutstandingMinor: Long = 0,
)

class LoansViewModel(private val repository: HisaabRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoansState())
    val state: StateFlow<LoansState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeLoans(),
                repository.invalidation("transactions", "loans", "schedule_items"),
            ) { loans, _ -> loans }
                .collect { loans ->
                    val rows = loans.map { loan ->
                        val paid = repository.service.settlementFor(LinkKind.LOAN, loan.id)
                        val schedule = repository.schedule(ScheduleOwner.LOAN, loan.id)
                        val paidIndexes = repository.forLink(LinkKind.LOAN, loan.id)
                            .mapNotNull { it.installmentIndex }
                            .toSet()
                        LoanRow(
                            loan = loan,
                            paid = paid,
                            nextDue = schedule.firstOrNull { it.sequence !in paidIndexes && it.dueDate >= BizDate.today() }
                                ?: schedule.firstOrNull { it.sequence !in paidIndexes },
                        )
                    }.sortedByDescending { !it.loan.isClosed }
                    _state.value = LoansState(
                        loading = false,
                        loans = rows,
                        totalOutstandingMinor = rows.filter { !it.loan.isClosed }.sumOf { it.outstandingTotalMinor },
                    )
                }
        }
    }
}

data class LoanFormState(
    val accounts: List<AccountEntity> = emptyList(),
    val saving: Boolean = false,
    val message: String? = null,
    val createdId: Long? = null,
)

class LoanFormViewModel(private val repository: HisaabRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoanFormState())
    val state: StateFlow<LoanFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(accounts = repository.activeAccounts())
        }
    }

    fun create(
        kind: LoanKind,
        institution: String,
        principalText: String,
        model: InterestModel,
        annualRateText: String,
        totalRepayableText: String,
        interestText: String,
        feeText: String,
        installmentCountText: String,
        frequency: RepaymentFrequency,
        firstDueDate: String,
        disbursementDate: String,
        accountId: Long?,
        notes: String?,
    ) {
        val principal = Money.parsePositive(principalText)
        if (institution.isBlank()) {
            _state.value = _state.value.copy(message = "প্রতিষ্ঠানের নাম লিখুন")
            return
        }
        if (principal == null) {
            _state.value = _state.value.copy(message = "ঋণের পরিমাণ ঠিক নেই")
            return
        }
        val interest = if (interestText.isBlank()) 0L else Money.parse(interestText)
        val fee = if (feeText.isBlank()) 0L else Money.parse(feeText)
        val total = if (totalRepayableText.isBlank()) null else Money.parsePositive(totalRepayableText)
        if (interest == null || fee == null) {
            _state.value = _state.value.copy(message = "সুদ বা ফি ঠিক নেই")
            return
        }
        if (accountId == null) {
            _state.value = _state.value.copy(message = "টাকা কোন হিসাবে এসেছে তা নির্বাচন করুন")
            return
        }
        val count = installmentCountText.trim().let { if (it.isEmpty()) 0 else Money.toLatinDigits(it).toIntOrNull() ?: 0 }
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            val result = repository.service.createLoan(
                LoanInput(
                    kind = kind,
                    institution = institution,
                    principalMinor = principal,
                    interestMinor = interest,
                    feeMinor = fee,
                    totalRepayableMinor = total,
                    model = model,
                    annualRateBp = (Money.toLatinDigits(annualRateText).toDoubleOrNull()?.times(100))?.toInt() ?: 0,
                    frequency = frequency,
                    installmentCount = count,
                    firstDueDate = if (model == InterestModel.MANUAL) null else firstDueDate,
                    disbursementDate = disbursementDate,
                    accountId = accountId,
                    notes = notes?.takeIf { it.isNotBlank() },
                ),
            )
            _state.value = when (result) {
                is WriteResult.Success -> _state.value.copy(saving = false, createdId = result.id, message = "ঋণ সংরক্ষণ হয়েছে")
                is WriteResult.Invalid -> _state.value.copy(saving = false, message = result.message)
                is WriteResult.Failed -> _state.value.copy(saving = false, message = result.message)
            }
        }
    }
}

data class LoanDetailState(
    val loading: Boolean = true,
    val loan: LoanEntity? = null,
    val schedule: List<ScheduleItemEntity> = emptyList(),
    val paidIndexes: Set<Int> = emptySet(),
    val settlement: Settlement = Settlement(),
    val accounts: List<AccountEntity> = emptyList(),
    val next: ScheduleItemEntity? = null,
    val message: String? = null,
    val saving: Boolean = false,
) {
    val outstandingPrincipalMinor: Long
        get() = ((loan?.principalMinor ?: 0) - settlement.principalMinor).coerceAtLeast(0)
    val outstandingTotalMinor: Long
        get() = ((loan?.totalRepayableMinor ?: loan?.principalMinor ?: 0) - settlement.amountMinor).coerceAtLeast(0)
}

class LoanDetailViewModel(
    private val repository: HisaabRepository,
    private val loanId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(LoanDetailState())
    val state: StateFlow<LoanDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val accounts = repository.activeAccounts()
            combine(
                repository.observeLoan(loanId),
                repository.observeSchedule(ScheduleOwner.LOAN, loanId),
                repository.invalidation("transactions", "loans"),
            ) { loan, schedule, _ -> loan to schedule }
                .collect { (loan, schedule) ->
                    val links = repository.forLink(LinkKind.LOAN, loanId)
                    val paidIndexes = links.mapNotNull { it.installmentIndex }.toSet()
                    val settlement = repository.service.settlementFor(LinkKind.LOAN, loanId)
                    val next = schedule.firstOrNull { it.sequence !in paidIndexes }
                    _state.value = _state.value.copy(
                        loading = false,
                        loan = loan,
                        schedule = schedule,
                        paidIndexes = paidIndexes,
                        settlement = settlement,
                        accounts = accounts,
                        next = next,
                    )
                }
        }
    }

    fun repay(
        amountText: String,
        interestText: String,
        feeText: String,
        date: String,
        accountId: Long?,
        installmentIndex: Int?,
    ) {
        val amount = Money.parsePositive(amountText)
        if (amount == null) {
            _state.value = _state.value.copy(message = "পরিশোধের পরিমাণ ঠিক নেই")
            return
        }
        val interest = if (interestText.isBlank()) 0L else Money.parse(interestText) ?: -1
        val fee = if (feeText.isBlank()) 0L else Money.parse(feeText) ?: -1
        if (interest < 0 || fee < 0 || interest + fee > amount) {
            _state.value = _state.value.copy(message = "সুদ ও ফি মোট পরিশোধের চেয়ে বেশি হতে পারে না")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            val result = repository.service.repayLoan(
                loanId = loanId,
                amountMinor = amount,
                principalMinor = amount - interest - fee,
                interestMinor = interest,
                feeMinor = fee,
                occurredAt = System.currentTimeMillis(),
                businessDate = date,
                accountId = accountId,
                installmentIndex = installmentIndex,
            )
            _state.value = _state.value.copy(
                saving = false,
                message = when (result) {
                    is WriteResult.Success -> "কিস্তি পরিশোধ সংরক্ষণ হয়েছে"
                    is WriteResult.Invalid -> result.message
                    is WriteResult.Failed -> result.message
                },
            )
        }
    }

    fun close() {
        viewModelScope.launch {
            repository.closeLoan(loanId)
        }
    }
}

data class EmiRow(
    val emi: EmiEntity,
    val paid: Settlement,
    val nextDue: ScheduleItemEntity?,
) {
    val outstandingTotalMinor: Long
        get() = (emi.totalFinancedPriceMinor - paid.amountMinor).coerceAtLeast(0)
}

data class EmiState(
    val loading: Boolean = true,
    val emis: List<EmiRow> = emptyList(),
    val outstandingMinor: Long = 0,
)

class EmiViewModel(private val repository: HisaabRepository) : ViewModel() {

    private val _state = MutableStateFlow(EmiState())
    val state: StateFlow<EmiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeEmis(),
                repository.invalidation("transactions", "emi_purchases", "schedule_items"),
            ) { emis, _ -> emis }
                .collect { emis ->
                    val rows = emis.map { emi ->
                        val paid = repository.service.settlementFor(LinkKind.EMI, emi.id)
                        val schedule = repository.schedule(ScheduleOwner.EMI, emi.id)
                        val paidIndexes = repository.forLink(LinkKind.EMI, emi.id).mapNotNull { it.installmentIndex }.toSet()
                        EmiRow(emi, paid, schedule.firstOrNull { it.sequence !in paidIndexes })
                    }
                    _state.value = EmiState(
                        loading = false,
                        emis = rows,
                        outstandingMinor = rows.filter { !it.emi.isSettled }.sumOf { it.outstandingTotalMinor },
                    )
                }
        }
    }
}

class EmiFormViewModel(private val repository: HisaabRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoanFormState())
    val state: StateFlow<LoanFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.value = _state.value.copy(accounts = repository.activeAccounts()) }
    }

    fun create(
        product: String,
        merchant: String,
        totalPriceText: String,
        cashPriceText: String,
        downPaymentText: String,
        interestText: String,
        countText: String,
        firstDueDate: String,
        accountId: Long?,
        notes: String?,
    ) {
        val total = Money.parsePositive(totalPriceText)
        if (product.isBlank()) {
            _state.value = _state.value.copy(message = "পণ্যের নাম লিখুন")
            return
        }
        if (total == null) {
            _state.value = _state.value.copy(message = "মোট মূল্য ঠিক নেই")
            return
        }
        val down = if (downPaymentText.isBlank()) 0L else Money.parse(downPaymentText) ?: -1
        val interest = if (interestText.isBlank()) 0L else Money.parse(interestText) ?: -1
        val cash = if (cashPriceText.isBlank()) null else Money.parsePositive(cashPriceText)
        if (down < 0 || interest < 0 || down >= total) {
            _state.value = _state.value.copy(message = "ডাউন পেমেন্ট ও সুদ ঠিক নেই")
            return
        }
        val count = Money.toLatinDigits(countText.trim()).toIntOrNull() ?: 0
        if (count <= 0) {
            _state.value = _state.value.copy(message = "কিস্তির সংখ্যা দিন")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            val result = repository.service.createEmi(
                EmiInput(
                    productName = product,
                    merchant = merchant.takeIf { it.isNotBlank() },
                    totalFinancedPriceMinor = total,
                    cashPriceMinor = cash,
                    downPaymentMinor = down,
                    interestMinor = interest,
                    installmentCount = count,
                    firstDueDate = firstDueDate,
                    accountId = accountId,
                    notes = notes?.takeIf { it.isNotBlank() },
                ),
            )
            _state.value = when (result) {
                is WriteResult.Success -> _state.value.copy(saving = false, createdId = result.id, message = "কিস্তি চুক্তি সংরক্ষণ হয়েছে")
                is WriteResult.Invalid -> _state.value.copy(saving = false, message = result.message)
                is WriteResult.Failed -> _state.value.copy(saving = false, message = result.message)
            }
        }
    }
}
