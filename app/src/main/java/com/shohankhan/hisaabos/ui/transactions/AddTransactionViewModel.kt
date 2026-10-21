package com.shohankhan.hisaabos.ui.transactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.data.db.PersonEntity
import com.shohankhan.hisaabos.data.db.ShopEntity
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.ledger.BusinessDateMillis
import com.shohankhan.hisaabos.domain.ledger.TxDraft
import com.shohankhan.hisaabos.domain.ledger.WriteResult
import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.CategoryKind
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.PartyKind
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.ui.text.Bn
import kotlinx.coroutines.launch

data class AddTxState(
    val type: TxType,
    val amountText: String = "",
    val date: String = BizDate.today(),
    val accountId: Long? = null,
    val counterAccountId: Long? = null,
    val categoryId: Long? = null,
    val shopId: Long? = null,
    val personId: Long? = null,
    val note: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    val duplicateWarning: Boolean = false,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val shops: List<ShopEntity> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
)

class AddTransactionViewModel(
    private val repository: HisaabRepository,
    private val initialType: TxType,
    private val presetShopId: Long? = null,
    private val presetPersonId: Long? = null,
    private val presetAccountId: Long? = null,
    private val existingId: Long? = null,
) : ViewModel() {

    val isEditing: Boolean get() = existingId != null

    var state by mutableStateOf(AddTxState(type = initialType))
        private set

    val categoryKind: CategoryKind = when (initialType) {
        TxType.INCOME -> CategoryKind.INCOME
        else -> CategoryKind.EXPENSE
    }

    init {
        viewModelScope.launch {
            loadMasters()
            existingId?.let { loadExisting(it) }
        }
    }

    private suspend fun loadExisting(id: Long) {
        val existing = repository.transaction(id) ?: return
        state = state.copy(
            type = existing.type,
            amountText = Money.formatAmountOnly(existing.amountMinor).replace(",", ""),
            date = existing.businessDate,
            accountId = existing.accountId,
            counterAccountId = existing.counterAccountId,
            categoryId = existing.categoryId,
            shopId = existing.shopId,
            personId = existing.personId,
            note = existing.note.orEmpty(),
        )
    }

    private suspend fun loadMasters() {
        val accounts = repository.activeAccounts()
        val categories = repository.activeCategories().filter { it.kind == categoryKind || it.kind == CategoryKind.BOTH }
        val shops = repository.allShops()
        val people = repository.allPeople()
        state = state.copy(
            accounts = accounts,
            categories = categories,
            shops = shops,
            people = people,
            accountId = state.accountId ?: presetAccountId ?: accounts.firstOrNull()?.id,
            counterAccountId = state.counterAccountId ?: accounts.getOrNull(1)?.id,
            shopId = state.shopId ?: presetShopId,
            personId = state.personId ?: presetPersonId,
        )
    }

    fun update(transform: (AddTxState) -> AddTxState) {
        state = transform(state).copy(error = null, duplicateWarning = false)
    }

    fun setType(type: TxType) {
        state = state.copy(type = type, categoryId = null, error = null)
        viewModelScope.launch { loadMasters() }
    }

    private var duplicateConfirmed = false

    fun save(onSuccess: (Long) -> Unit) {
        if (state.saving) return
        val amount = Money.parsePositive(state.amountText)
        if (amount == null) {
            state = state.copy(error = Bn.errorAmount)
            return
        }
        val draft = buildDraft(amount) ?: return
        if (!duplicateConfirmed) {
            viewModelScope.launch {
                val duplicate = repository.service.isPotentialDuplicate(draft)
                if (duplicate) {
                    state = state.copy(duplicateWarning = true)
                } else {
                    persist(draft, onSuccess)
                }
            }
        } else {
            viewModelScope.launch { persist(draft, onSuccess) }
        }
    }

    fun saveDespiteDuplicate(onSuccess: (Long) -> Unit) {
        duplicateConfirmed = true
        state = state.copy(duplicateWarning = false)
        save(onSuccess)
    }

    fun dismissDuplicateWarning() {
        state = state.copy(duplicateWarning = false)
    }

    private suspend fun persist(draft: TxDraft, onSuccess: (Long) -> Unit) {
        state = state.copy(saving = true, error = null)
        val result = if (existingId != null) repository.service.update(existingId, draft) else repository.service.post(draft)
        when (result) {
            is WriteResult.Success -> {
                state = state.copy(saving = false)
                onSuccess(result.id)
            }
            is WriteResult.Invalid -> state = state.copy(saving = false, error = result.message)
            is WriteResult.Failed -> state = state.copy(saving = false, error = Bn.errorSave)
        }
    }

    private fun buildDraft(amount: Long): TxDraft? {
        val occurredAt = if (state.date == BizDate.today()) System.currentTimeMillis() else BusinessDateMillis.of(state.date)
        val type = state.type
        if (type == TxType.SHOP_PAYMENT && state.shopId == null) {
            state = state.copy(error = "কোন দোকানের বিল পরিশোধ করছেন তা নির্বাচন করুন")
            return null
        }
        if (type == TxType.CREDIT_PURCHASE && state.shopId == null) {
            state = state.copy(error = "কোন দোকানে বাকি হচ্ছে তা নির্বাচন করুন")
            return null
        }
        return TxDraft(
            type = type,
            amountMinor = amount,
            occurredAt = occurredAt,
            businessDate = state.date,
            accountId = state.accountId,
            counterAccountId = if (type == TxType.TRANSFER) state.counterAccountId else null,
            categoryId = state.categoryId,
            shopId = if (type == TxType.INCOME || type == TxType.EXPENSE || type == TxType.CREDIT_PURCHASE || type == TxType.SHOP_PAYMENT) state.shopId else null,
            personId = state.personId,
            partyKind = when {
                state.shopId != null -> PartyKind.SHOP
                state.personId != null -> PartyKind.PERSON
                else -> PartyKind.NONE
            },
            note = state.note.takeIf { it.isNotBlank() },
        )
    }

    fun createAccount(name: String, type: AccountType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createAccount(name, type)
            state = state.copy(
                accounts = repository.activeAccounts(),
                accountId = if (state.accountId == null) id else state.accountId,
            )
        }
    }

    fun createCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createCategory(name, categoryKind)
            state = state.copy(
                categories = repository.activeCategories().filter { it.kind == categoryKind || it.kind == CategoryKind.BOTH },
                categoryId = id,
            )
        }
    }

    fun createShop(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createShop(name)
            state = state.copy(shops = repository.allShops(), shopId = id)
        }
    }

    fun afterShopsChanged() {
        viewModelScope.launch { state = state.copy(shops = repository.allShops()) }
    }

    fun afterPeopleChanged() {
        viewModelScope.launch { state = state.copy(people = repository.allPeople()) }
    }
}
