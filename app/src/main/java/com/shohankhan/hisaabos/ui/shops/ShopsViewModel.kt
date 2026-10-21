package com.shohankhan.hisaabos.ui.shops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabos.data.db.PurchaseEntity
import com.shohankhan.hisaabos.data.db.ShopEntity
import com.shohankhan.hisaabos.data.db.TxListItem
import com.shohankhan.hisaabos.data.repo.HisaabRepository
import com.shohankhan.hisaabos.domain.ledger.TxDraft
import com.shohankhan.hisaabos.domain.ledger.WriteResult
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.PartyKind
import com.shohankhan.hisaabos.domain.model.TxType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ShopRow(
    val shop: ShopEntity,
    val creditMinor: Long = 0,
    val paidMinor: Long = 0,
) {
    val outstandingMinor: Long get() = (shop.openingBalanceMinor + creditMinor - paidMinor).coerceAtLeast(0)
}

data class ShopsState(
    val loading: Boolean = true,
    val shops: List<ShopRow> = emptyList(),
    val totalOutstandingMinor: Long = 0,
    val error: String? = null,
    val saving: Boolean = false,
)

class ShopsViewModel(private val repository: HisaabRepository) : ViewModel() {

    private val _state = MutableStateFlow(ShopsState())
    val state: StateFlow<ShopsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.observeShops(), repository.observeShopTotalsList()) { shops, totals ->
                shops.map { shop ->
                    val rows = totals.filter { it.shopId == shop.id }
                    ShopRow(
                        shop = shop,
                        creditMinor = rows.filter { it.type == TxType.CREDIT_PURCHASE }.sumOf { it.totalMinor },
                        paidMinor = rows.filter { it.type == TxType.SHOP_PAYMENT }.sumOf { it.totalMinor },
                    )
                }.sortedByDescending { it.outstandingMinor }
            }.collect { rows ->
                _state.value = _state.value.copy(
                    loading = false,
                    shops = rows,
                    totalOutstandingMinor = rows.sumOf { it.outstandingMinor },
                )
            }
        }
    }

    fun createShop(
        name: String,
        shopkeeper: String?,
        phone: String?,
        openingBalance: String,
        onCreated: (Long) -> Unit,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _state.value = _state.value.copy(error = "দোকানের নাম লিখুন")
            return
        }
        val opening = if (openingBalance.isBlank()) 0L else Money.parsePositive(openingBalance)
        if (opening == null) {
            _state.value = _state.value.copy(error = "বাকির পরিমাণ ঠিক নেই")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            val id = repository.createShop(
                name = trimmed,
                shopkeeperName = shopkeeper,
                phone = phone,
                openingBalanceMinor = opening,
            )
            _state.value = _state.value.copy(saving = false)
            onCreated(id)
        }
    }

    fun archiveShop(id: Long, archived: Boolean) {
        viewModelScope.launch { repository.archiveShop(id, archived) }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

data class ShopDetailState(
    val loading: Boolean = true,
    val shop: ShopEntity? = null,
    val transactions: List<TxListItem> = emptyList(),
    val purchases: List<PurchaseEntity> = emptyList(),
    val creditMinor: Long = 0,
    val paidMinor: Long = 0,
    val message: String? = null,
    val saving: Boolean = false,
) {
    val outstandingMinor: Long get() = ((shop?.openingBalanceMinor ?: 0) + creditMinor - paidMinor).coerceAtLeast(0)
}

class ShopDetailViewModel(
    private val repository: HisaabRepository,
    private val shopId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(ShopDetailState())
    val state: StateFlow<ShopDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeShop(shopId),
                repository.observeTransactionList(
                    from = null, to = null, types = emptyList(), categoryId = null, accountId = null,
                    shopId = shopId, personId = null, minAmount = null, maxAmount = null,
                    search = null, sort = "NEWEST", limit = 200, offset = 0,
                ),
                repository.observeShopPurchases(shopId),
            ) { shop, txs, purchases -> Triple(shop, txs, purchases) }
                .collect { (shop, txs, purchases) ->
                    val credit = txs
                        .filter { it.tx.type == TxType.CREDIT_PURCHASE }
                        .sumOf { it.tx.amountMinor }
                    val paid = txs
                        .filter { it.tx.type == TxType.SHOP_PAYMENT }
                        .sumOf { it.tx.amountMinor }
                    _state.value = _state.value.copy(
                        loading = false,
                        shop = shop,
                        transactions = txs,
                        purchases = purchases,
                        creditMinor = credit,
                        paidMinor = paid,
                    )
                }
        }
    }

    fun pay(amountText: String, accountId: Long?, date: String, note: String?) {
        val amount = Money.parsePositive(amountText)
        if (amount == null) {
            _state.value = _state.value.copy(message = "বিলের পরিমাণ ঠিক নেই")
            return
        }
        if (accountId == null) {
            _state.value = _state.value.copy(message = "কোন হিসাব থেকে টাকা দিচ্ছেন তা নির্বাচন করুন")
            return
        }
        val shop = _state.value.shop ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            val result = repository.service.post(
                TxDraft(
                    type = TxType.SHOP_PAYMENT,
                    amountMinor = amount,
                    businessDate = date,
                    accountId = accountId,
                    shopId = shopId,
                    partyKind = PartyKind.SHOP,
                    partyName = shop.shopkeeperName ?: shop.name,
                    linkKind = LinkKind.SHOP,
                    linkId = shopId,
                    title = "দোকান বিল পরিশোধ",
                    note = note?.takeIf { it.isNotBlank() },
                ),
            )
            _state.value = _state.value.copy(
                saving = false,
                message = when (result) {
                    is WriteResult.Success -> "পরিশোধ সংরক্ষণ হয়েছে"
                    is WriteResult.Invalid -> result.message
                    is WriteResult.Failed -> result.message
                },
            )
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun today(): String = BizDate.today()
}
