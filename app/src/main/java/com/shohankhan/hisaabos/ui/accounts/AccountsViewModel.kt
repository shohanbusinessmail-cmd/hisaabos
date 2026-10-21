package com.shohankhan.hisaabos.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.TransactionEntity
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.ledger.WriteResult
import com.shohankhan.hisaabos.domain.ledger.TxDraft
import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.ui.text.Bn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AccountsState(
    val loading: Boolean = true,
    val accounts: List<AccountEntity> = emptyList(),
    val balances: Map<Long, Long> = emptyMap(),
    val showArchived: Boolean = false,
    val error: String? = null,
)

class AccountsViewModel(private val repository: HisaabRepository) : ViewModel() {

    private val _state = MutableStateFlow(AccountsState())
    val state: StateFlow<AccountsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.invalidation("accounts", "transactions").collect { reload() }
        }
    }

    suspend fun reload() {
        val accounts = repository.observeAccounts().first()
        val balances = repository.balances()
        _state.value = AccountsState(loading = false, accounts = accounts, balances = balances, showArchived = _state.value.showArchived)
    }

    fun toggleArchived() {
        _state.value = _state.value.copy(showArchived = !_state.value.showArchived)
    }

    fun saveAccount(
        existing: AccountEntity?,
        name: String,
        type: AccountType,
        institution: String,
        openingText: String,
        notes: String,
        isLiability: Boolean,
        onDone: (Long) -> Unit,
    ) {
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "হিসাবের নাম লিখুন")
            return
        }
        viewModelScope.launch {
            try {
                if (existing == null) {
                    val id = repository.createAccount(name, type, institution, isLiability)
                    val opening = Money.parse(openingText)
                    if (opening != null && opening > 0) {
                        repository.service.post(
                            TxDraft(
                                type = TxType.OPENING_BALANCE,
                                amountMinor = opening,
                                accountId = id,
                                flowSign = if (isLiability) -1 else 1,
                                title = "শুরুর ব্যালেন্স",
                                note = notes.takeIf { it.isNotBlank() },
                            )
                        )
                    }
                    onDone(id)
                } else {
                    val id = existing.id
                    repository.updateAccount(
                        existing.copy(
                            name = name.trim(),
                            type = type,
                            institution = institution.trim().takeIf { it.isNotEmpty() },
                            notes = notes.takeIf { it.isNotBlank() },
                            isLiability = isLiability,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                    onDone(id)
                }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(error = Bn.errorSave)
            }
        }
    }

    fun setArchived(id: Long, archived: Boolean) {
        viewModelScope.launch {
            repository.archiveAccount(id, archived)
            reload()
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

data class AccountDetailState(
    val loading: Boolean = true,
    val account: AccountEntity? = null,
    val balanceMinor: Long = 0,
    val transactions: List<TransactionEntity> = emptyList(),
)

class AccountDetailViewModel(
    private val repository: HisaabRepository,
    private val accountId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountDetailState())
    val state: StateFlow<AccountDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    suspend fun load() {
        val account = repository.account(accountId)
        val balance = repository.balances()[accountId] ?: 0L
        _state.value = AccountDetailState(
            loading = false,
            account = account,
            balanceMinor = balance,
            transactions = repository.accountTransactions(accountId, 100),
        )
    }

    fun reconcile(actualText: String, reason: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val actual = Money.parse(actualText)
            if (actual == null) {
                onDone("সঠিক ব্যালেন্স লিখুন")
                return@launch
            }
            val difference = actual - _state.value.balanceMinor
            if (difference == 0L) {
                onDone("ব্যালেন্স মিলে গেছে — কোনো সমন্বয় দরকার নেই")
                return@launch
            }
            val result = repository.service.adjustAccount(
                accountId = accountId,
                amountMinor = kotlin.math.abs(difference),
                increase = difference > 0,
                occurredAt = System.currentTimeMillis(),
                businessDate = BizDate.today(),
                reason = reason.ifBlank { "ব্যালেন্স মিলানো" },
            )
            when (result) {
                is WriteResult.Success -> {
                    load()
                    onDone("সমন্বয় সংরক্ষণ হয়েছে")
                }
                is WriteResult.Invalid -> onDone(result.message)
                is WriteResult.Failed -> onDone(Bn.errorSave)
            }
        }
    }
}
