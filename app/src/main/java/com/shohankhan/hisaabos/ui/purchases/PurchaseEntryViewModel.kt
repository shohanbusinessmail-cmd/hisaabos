package com.shohankhan.hisaabos.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.db.CategoryEntity
import com.shohankhan.hisaabos.data.db.ShopEntity
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.ledger.PurchaseInput
import com.shohankhan.hisaabos.domain.ledger.PurchaseItemInput
import com.shohankhan.hisaabos.domain.ledger.PurchaseMath
import com.shohankhan.hisaabos.domain.ledger.QuantityMath
import com.shohankhan.hisaabos.domain.ledger.WriteResult
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ItemDraft(
    val key: Long,
    val name: String = "",
    val quantity: String = "1",
    val unit: String = "কেজি",
    val price: String = "",
) {
    val lineTotalMinor: Long
        get() {
            val qty = QuantityMath.parseMilli(quantity) ?: return 0
            val priceMinor = Money.parsePositive(price) ?: return 0
            return PurchaseMath.lineTotal(qty, priceMinor)
        }
}

data class PurchaseEntryState(
    val loading: Boolean = true,
    val shops: List<ShopEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val shopId: Long? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val date: String = BizDate.today(),
    val items: List<ItemDraft> = listOf(ItemDraft(key = 1)),
    val discount: String = "",
    val extraCharge: String = "",
    val paid: String = "",
    val note: String = "",
    val saving: Boolean = false,
    val message: String? = null,
    val savedId: Long? = null,
) {
    val subtotalMinor: Long get() = items.sumOf { it.lineTotalMinor }
    val discountMinor: Long get() = Money.parsePositive(discount) ?: 0
    val extraChargeMinor: Long get() = Money.parsePositive(extraCharge) ?: 0
    val totalMinor: Long get() = (subtotalMinor - discountMinor + extraChargeMinor).coerceAtLeast(0)
    val paidMinor: Long get() = Money.parsePositive(paid) ?: 0
    val creditMinor: Long get() = (totalMinor - paidMinor).coerceAtLeast(0)
    val hasItems: Boolean get() = items.any { it.name.isNotBlank() && it.lineTotalMinor > 0 }
}

class PurchaseEntryViewModel(
    private val repository: HisaabRepository,
    private val presetShopId: Long?,
    private val presetPersonId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(PurchaseEntryState(shopId = presetShopId))
    val state: StateFlow<PurchaseEntryState> = _state.asStateFlow()
    private var nextKey = 2L

    init {
        viewModelScope.launch {
            val shops = repository.allShops()
            val accounts = repository.activeAccounts()
            val categories = repository.allCategories().filter { it.kind != com.shohankhan.hisaabos.domain.model.CategoryKind.INCOME }
            _state.value = _state.value.copy(
                loading = false,
                shops = shops,
                accounts = accounts,
                categories = categories,
                shopId = presetShopId ?: shops.firstOrNull()?.id,
                accountId = accounts.firstOrNull()?.id,
                categoryId = categories.firstOrNull()?.id,
            )
        }
    }

    fun createShop(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _state.value = _state.value.copy(message = "দোকানের নাম লিখুন")
            return
        }
        viewModelScope.launch {
            val id = repository.createShop(trimmed)
            val shops = repository.allShops()
            _state.value = _state.value.copy(shops = shops, shopId = id, message = null)
        }
    }

    fun addItem() {
        _state.value = _state.value.copy(items = _state.value.items + ItemDraft(key = nextKey++))
    }

    fun removeItem(key: Long) {
        val remaining = _state.value.items.filterNot { it.key == key }
        _state.value = _state.value.copy(items = remaining.ifEmpty { listOf(ItemDraft(key = nextKey++)) })
    }

    fun updateItem(key: Long, transform: (ItemDraft) -> ItemDraft) {
        _state.value = _state.value.copy(
            items = _state.value.items.map { if (it.key == key) transform(it) else it },
        )
    }

    fun setShop(id: Long?) = update { it.copy(shopId = id) }
    fun setAccount(id: Long?) = update { it.copy(accountId = id) }
    fun setCategory(id: Long?) = update { it.copy(categoryId = id) }
    fun setDate(date: String) = update { it.copy(date = date) }
    fun setDiscount(text: String) = update { it.copy(discount = text) }
    fun setExtraCharge(text: String) = update { it.copy(extraCharge = text) }
    fun setPaid(text: String) = update { it.copy(paid = text) }
    fun setNote(text: String) = update { it.copy(note = text) }

    private fun update(transform: (PurchaseEntryState) -> PurchaseEntryState) {
        _state.value = transform(_state.value)
    }

    fun save() {
        val current = _state.value
        if (!current.hasItems) {
            _state.value = current.copy(message = "অন্তত একটি পণ্য যোগ করুন")
            return
        }
        if (current.paidMinor > current.totalMinor) {
            _state.value = current.copy(message = "পরিশোধ মোট মূল্যের চেয়ে বেশি হতে পারে না")
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(saving = true, message = null)
            val items = current.items
                .filter { it.name.isNotBlank() && it.lineTotalMinor > 0 }
                .map {
                    PurchaseItemInput(
                        name = it.name.trim(),
                        quantityMilli = QuantityMath.parseMilli(it.quantity) ?: 1000,
                        unit = it.unit.ifBlank { "পিস" },
                        unitPriceMinor = Money.parsePositive(it.price) ?: 0,
                    )
                }
            val result = repository.service.createPurchase(
                PurchaseInput(
                    shopId = current.shopId,
                    personId = presetPersonId,
                    businessDate = current.date,
                    items = items,
                    discountMinor = current.discountMinor,
                    extraChargeMinor = current.extraChargeMinor,
                    paidMinor = current.paidMinor,
                    accountId = if (current.paidMinor > 0) current.accountId else null,
                    categoryId = current.categoryId,
                    note = current.note.takeIf { it.isNotBlank() },
                ),
            )
            _state.value = when (result) {
                is WriteResult.Success -> current.copy(saving = false, savedId = result.id, message = "বাজার সংরক্ষণ হয়েছে")
                is WriteResult.Invalid -> current.copy(saving = false, message = result.message)
                is WriteResult.Failed -> current.copy(saving = false, message = result.message)
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
