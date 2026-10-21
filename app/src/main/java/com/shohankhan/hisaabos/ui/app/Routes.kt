package com.shohankhan.hisaabos.ui.app

import com.shohankhan.hisaabos.domain.model.TxType

/** Every destination in the app. Ids use "-1" for "not set" because nav args must be primitives. */
object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val BOOKS = "books"
    const val REPORTS = "reports"
    const val MORE = "more"

    const val ADD_TX = "add_tx?type={type}&shopId={shopId}&personId={personId}&accountId={accountId}"
    const val EDIT_TX = "edit_tx/{id}"
    const val TX_DETAIL = "tx/{id}"

    const val ACCOUNTS = "accounts"
    const val ACCOUNT_DETAIL = "account/{id}"
    const val ACCOUNT_FORM = "account_form?accountId={accountId}"
    const val CATEGORIES = "categories"

    const val SHOPS = "shops"
    const val SHOP_DETAIL = "shop/{id}"
    const val PURCHASE = "purchase?shopId={shopId}&personId={personId}"

    const val PEOPLE = "people"
    const val PERSON_DETAIL = "person/{id}"

    const val LOANS = "loans"
    const val LOAN_DETAIL = "loan/{id}"
    const val EMI = "emi"
    const val EMI_DETAIL = "emi/{id}"

    const val SAVINGS = "savings"
    const val BUDGETS = "budgets"
    const val REMINDERS = "reminders"
    const val SEARCH = "search"
    const val BACKUP = "backup"
    const val SECURITY = "security"
    const val APPEARANCE = "appearance"
    const val ABOUT = "about"
    const val VERIFY = "verify"

    fun addTx(type: TxType, shopId: Long? = null, personId: Long? = null, accountId: Long? = null): String =
        "add_tx?type=${type.name}&shopId=${shopId ?: -1}&personId=${personId ?: -1}&accountId=${accountId ?: -1}"

    fun editTx(id: Long) = "edit_tx/$id"
    fun txDetail(id: Long) = "tx/$id"
    fun accountDetail(id: Long) = "account/$id"
    fun accountForm(id: Long? = null) = "account_form?accountId=${id ?: -1}"
    fun shopDetail(id: Long) = "shop/$id"
    fun personDetail(id: Long) = "person/$id"
    fun loanDetail(id: Long) = "loan/$id"
    fun emiDetail(id: Long) = "emi/$id"
    fun purchase(shopId: Long? = null, personId: Long? = null) =
        "purchase?shopId=${shopId ?: -1}&personId=${personId ?: -1}"
}
