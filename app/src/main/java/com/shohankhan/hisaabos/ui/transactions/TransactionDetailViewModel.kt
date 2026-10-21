package com.shohankhan.hisaabos.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.TransactionEntity
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.ledger.WriteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TxDetailState(
    val loading: Boolean = true,
    val transaction: TransactionEntity? = null,
    val accountName: String? = null,
    val counterAccountName: String? = null,
    val categoryName: String? = null,
    val shopName: String? = null,
    val personName: String? = null,
    val deleted: Boolean = false,
    val error: String? = null,
)

class TransactionDetailViewModel(
    private val repository: HisaabRepository,
    private val transactionId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(TxDetailState())
    val state: StateFlow<TxDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    suspend fun load() {
        val tx = repository.transaction(transactionId)
        if (tx == null) {
            _state.value = TxDetailState(loading = false, error = "লেনদেনটি পাওয়া যায়নি")
            return
        }
        _state.value = TxDetailState(
            loading = false,
            transaction = tx,
            accountName = tx.accountId?.let { repository.account(it)?.name },
            counterAccountName = tx.counterAccountId?.let { repository.account(it)?.name },
            categoryName = tx.categoryId?.let { repository.category(it)?.name },
            shopName = tx.shopId?.let { repository.shop(it)?.name },
            personName = tx.personId?.let { repository.person(it)?.name },
            deleted = tx.isDeleted,
        )
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = repository.service.delete(transactionId)) {
                is WriteResult.Success -> {
                    load()
                    onDone()
                }
                is WriteResult.Invalid -> _state.value = _state.value.copy(error = result.message)
                is WriteResult.Failed -> _state.value = _state.value.copy(error = "মুছে ফেলা যায়নি")
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            repository.service.restore(transactionId)
            load()
        }
    }
}
