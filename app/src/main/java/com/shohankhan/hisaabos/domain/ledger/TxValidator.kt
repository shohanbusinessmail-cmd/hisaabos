package com.shohankhan.hisaabos.domain.ledger

import com.shohankhan.hisaabos.domain.model.CategoryKind
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.Money
import com.shohankhan.hisaabos.domain.model.TxType

/**
 * Pure, unit-tested validation for every transaction draft. Returning a Bangla
 * message keeps error handling consistent across all forms.
 */
object TxValidator {

    fun validate(
        draft: TxDraft,
        categoryKind: CategoryKind? = null,
        sameAccountAllowed: Boolean = false,
    ): String? {
        if (draft.amountMinor <= 0) return "টাকার পরিমাণ শূন্যের বেশি হতে হবে"
        if (draft.amountMinor > MAX_AMOUNT) return "এত বড় অঙ্কের লেনদেন সমর্থিত নয়"
        if (draft.businessDate.isBlank()) return "তারিখ ঠিক নেই"

        when (draft.type) {
            TxType.INCOME, TxType.EXPENSE, TxType.REFUND, TxType.SHOP_PAYMENT,
            TxType.DEBT_BORROWED, TxType.DEBT_REPAID, TxType.LENDING, TxType.LOAN_COLLECTION,
            TxType.LOAN_DISBURSEMENT, TxType.LOAN_REPAYMENT, TxType.EMI_PAYMENT,
            TxType.OPENING_BALANCE, TxType.ADJUSTMENT,
            -> if (draft.accountId == null) return "কোন হিসাব থেকে লেনদেন হবে তা নির্বাচন করুন"

            TxType.TRANSFER, TxType.SAVINGS_DEPOSIT, TxType.SAVINGS_WITHDRAWAL -> {
                if (draft.accountId == null) return "উৎস হিসাব নির্বাচন করুন"
                if (draft.counterAccountId == null) return "গন্তব্য হিসাব নির্বাচন করুন"
                if (!sameAccountAllowed && draft.accountId == draft.counterAccountId) {
                    return "একই হিসাবে টাকা স্থানান্তর করা যাবে না"
                }
            }

            TxType.CREDIT_PURCHASE -> {
                if (draft.shopId == null && draft.personId == null) return "দোকান বা ব্যক্তি নির্বাচন করুন"
            }

            TxType.EMI_PURCHASE -> if (draft.linkId == null) return "কিস্তি চুক্তি সনাক্ত করা যায়নি"
        }

        if (draft.interestMinor < 0 || draft.feeMinor < 0) return "সুদ বা ফি ঋণাত্মক হতে পারবে না"
        if (draft.type.isLiabilitySettlement || draft.type == TxType.INCOME || draft.type == TxType.EXPENSE) {
            if (draft.interestMinor + draft.feeMinor > draft.amountMinor) {
                return "সুদ ও ফি মোট পরিশোধের চেয়ে বেশি হতে পারে না"
            }
        }
        if (draft.categoryId != null && categoryKind != null) {
            val mismatch = when (draft.type) {
                TxType.INCOME -> categoryKind == CategoryKind.EXPENSE
                TxType.EXPENSE, TxType.CREDIT_PURCHASE -> categoryKind == CategoryKind.INCOME
                else -> false
            }
            if (mismatch) return "এই লেনদেনের ধরনের সাথে ক্যাটাগরির ধরন মিলছে না"
        }
        if (draft.type == TxType.DEBT_REPAID && (draft.linkKind != LinkKind.DEBT || draft.linkId == null)) {
            return "কোন ধার পরিশোধ করছেন তা নির্বাচন করুন"
        }
        if (draft.type == TxType.LOAN_COLLECTION && (draft.linkKind != LinkKind.DEBT || draft.linkId == null)) {
            return "কোন পাওনা আদায় করছেন তা নির্বাচন করুন"
        }
        if (draft.type == TxType.LOAN_REPAYMENT && (draft.linkKind != LinkKind.LOAN || draft.linkId == null)) {
            return "কোন ঋণ পরিশোধ করছেন তা নির্বাচন করুন"
        }
        if (draft.type == TxType.EMI_PAYMENT && (draft.linkKind != LinkKind.EMI || draft.linkId == null)) {
            return "কোন কিস্তি পরিশোধ করছেন তা নির্বাচন করুন"
        }
        return null
    }

    fun describeAmount(minor: Long): String = Money.format(minor)

    private const val MAX_AMOUNT = 100_000_000_000_000L
}
