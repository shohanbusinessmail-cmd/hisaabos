package com.shohankhan.hisaabos.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.DebtEntity
import com.shohankhan.hisaabos.data.db.PersonEntity
import com.shohankhan.hisaabos.data.db.TxListItem
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.ledger.DebtInput
import com.shohankhan.hisaabos.domain.ledger.WriteResult
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.DebtDirection
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.TxType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class DebtRow(
    val debt: DebtEntity,
    val paidMinor: Long,
) {
    val outstandingMinor: Long
        get() = (debt.principalMinor + debt.interestMinor - paidMinor).coerceAtLeast(0)
    val isSettled: Boolean get() = debt.isClosed || outstandingMinor == 0L
}

data class PersonRow(
    val person: PersonEntity,
    val payableMinor: Long,
    val receivableMinor: Long,
    val openDebts: Int,
)

data class PeopleState(
    val loading: Boolean = true,
    val rows: List<PersonRow> = emptyList(),
    val totalPayableMinor: Long = 0,
    val totalReceivableMinor: Long = 0,
    val error: String? = null,
)

class PeopleViewModel(private val repository: HisaabRepository) : ViewModel() {

    private val _state = MutableStateFlow(PeopleState())
    val state: StateFlow<PeopleState> = _state.asStateFlow()
    private var debtsCache: List<DebtEntity> = emptyList()
    private var paidCache: Map<Long, Long> = emptyMap()

    init {
        viewModelScope.launch {
            combine(
                repository.observePeople(),
                repository.observeDebts(),
                repository.invalidation("transactions", "debts"),
            ) { people, debts, _ -> people to debts }
                .collect { (people, debts) ->
                    debtsCache = debts
                    paidCache = loadPaid()
                    val rows = people.map { person ->
                        val personDebts = debts.filter { it.personId == person.id && !it.isClosed }
                        val payable = personDebts
                            .filter { it.direction == DebtDirection.BORROWED }
                            .sumOf { debt -> (debt.principalMinor + debt.interestMinor - (paidCache[debt.id] ?: 0L)).coerceAtLeast(0) }
                        val receivable = personDebts
                            .filter { it.direction == DebtDirection.LENT }
                            .sumOf { debt -> (debt.principalMinor + debt.interestMinor - (paidCache[debt.id] ?: 0L)).coerceAtLeast(0) }
                        PersonRow(person, payable, receivable, personDebts.size)
                    }.sortedByDescending { it.payableMinor + it.receivableMinor }
                    _state.value = PeopleState(
                        loading = false,
                        rows = rows,
                        totalPayableMinor = rows.sumOf { it.payableMinor },
                        totalReceivableMinor = rows.sumOf { it.receivableMinor },
                    )
                }
        }
    }

    private suspend fun loadPaid(): Map<Long, Long> =
        repository.allForLinkKind(LinkKind.DEBT)
            .filter { it.linkId != null && (it.type == TxType.DEBT_REPAID || it.type == TxType.LOAN_COLLECTION) }
            .groupBy { it.linkId!! }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }

    fun paidFor(debtId: Long): Long = paidCache[debtId] ?: 0L

    fun createPerson(name: String, phone: String?, onCreated: (Long) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _state.value = _state.value.copy(error = "নাম লিখুন")
            return
        }
        viewModelScope.launch {
            val id = repository.dbInsertPerson(trimmed, phone)
            _state.value = _state.value.copy(error = null)
            onCreated(id)
        }
    }
}

data class PersonDetailState(
    val loading: Boolean = true,
    val person: PersonEntity? = null,
    val debts: List<DebtRow> = emptyList(),
    val transactions: List<TxListItem> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val payableMinor: Long = 0,
    val receivableMinor: Long = 0,
    val message: String? = null,
    val saving: Boolean = false,
)

class PersonDetailViewModel(
    private val repository: HisaabRepository,
    private val personId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(PersonDetailState())
    val state: StateFlow<PersonDetailState> = _state.asStateFlow()
    private var paidCache: Map<Long, Long> = emptyMap()

    init {
        viewModelScope.launch {
            val accounts = repository.activeAccounts()
            combine(
                repository.observePerson(personId),
                repository.observePersonDebts(personId),
                repository.observeTransactionList(
                    from = null, to = null, types = emptyList(), categoryId = null, accountId = null,
                    shopId = null, personId = personId, minAmount = null, maxAmount = null,
                    search = null, sort = "NEWEST", limit = 200, offset = 0,
                ),
                repository.invalidation("transactions"),
            ) { person, debts, txs, _ -> Triple(person, debts, txs) }
                .collect { (person, debts, txs) ->
                    paidCache = repository.allForLinkKind(LinkKind.DEBT)
                        .filter { it.linkId != null && (it.type == TxType.DEBT_REPAID || it.type == TxType.LOAN_COLLECTION) }
                        .groupBy { it.linkId!! }
                        .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
                    val rows = debts.map { DebtRow(it, paidCache[it.id] ?: 0L) }
                        .sortedByDescending { it.debt.businessDate }
                    _state.value = _state.value.copy(
                        loading = false,
                        person = person,
                        debts = rows,
                        transactions = txs,
                        accounts = accounts,
                        payableMinor = rows.filter { it.debt.direction == DebtDirection.BORROWED && !it.isSettled }
                            .sumOf { it.outstandingMinor },
                        receivableMinor = rows.filter { it.debt.direction == DebtDirection.LENT && !it.isSettled }
                            .sumOf { it.outstandingMinor },
                    )
                }
        }
    }

    fun addDebt(
        direction: DebtDirection,
        amountText: String,
        interestText: String,
        date: String,
        dueDate: String?,
        accountId: Long?,
        note: String?,
    ) {
        val principal = Money.parsePositive(amountText)
        if (principal == null) {
            _state.value = _state.value.copy(message = "টাকার পরিমাণ ঠিক নেই")
            return
        }
        val interest = if (interestText.isBlank()) 0L else Money.parse(interestText)
        if (interest == null || interest < 0) {
            _state.value = _state.value.copy(message = "সুদের পরিমাণ ঠিক নেই")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            val result = repository.service.createDebt(
                DebtInput(
                    direction = direction,
                    personId = personId,
                    principalMinor = principal,
                    interestMinor = interest,
                    businessDate = date,
                    dueDate = dueDate,
                    accountId = accountId,
                    note = note?.takeIf { it.isNotBlank() },
                ),
            )
            _state.value = _state.value.copy(saving = false, message = result.message())
        }
    }

    fun settle(debtId: Long, debt: DebtEntity, amountText: String, interestText: String, date: String, accountId: Long?, note: String?) {
        val amount = Money.parsePositive(amountText)
        if (amount == null) {
            _state.value = _state.value.copy(message = "পরিশোধের পরিমাণ ঠিক নেই")
            return
        }
        val interest = if (interestText.isBlank()) 0L else Money.parse(interestText)
        if (interest == null || interest < 0 || interest > amount) {
            _state.value = _state.value.copy(message = "সুদের অংশ ঠিক নেই")
            return
        }
        val principal = amount - interest
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            val result = repository.service.settleDebt(
                debtId = debtId,
                amountMinor = amount,
                principalMinor = principal,
                interestMinor = interest,
                feeMinor = 0,
                occurredAt = System.currentTimeMillis(),
                businessDate = date,
                accountId = accountId,
                note = note?.takeIf { it.isNotBlank() },
            )
            _state.value = _state.value.copy(saving = false, message = result.message())
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun today(): String = BizDate.today()
}

private fun WriteResult.message(): String = when (this) {
    is WriteResult.Success -> "সংরক্ষণ হয়েছে"
    is WriteResult.Invalid -> message
    is WriteResult.Failed -> message
}
