package com.shohankhan.hisaabos.domain.ledger

import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.LinkKind
import com.shohankhan.hisaabos.domain.model.PartyKind
import com.shohankhan.hisaabos.domain.model.TxType

/**
 * Everything needed to post one transaction. The UI builds drafts; only
 * [TransactionService] turns them into persisted rows and ledger postings.
 */
data class TxDraft(
    val type: TxType,
    val amountMinor: Long,
    val occurredAt: Long = System.currentTimeMillis(),
    val businessDate: String = BizDate.of(occurredAt),
    val accountId: Long? = null,
    val counterAccountId: Long? = null,
    val categoryId: Long? = null,
    val shopId: Long? = null,
    val personId: Long? = null,
    val partyKind: PartyKind = PartyKind.NONE,
    val partyName: String? = null,
    val linkKind: LinkKind = LinkKind.NONE,
    val linkId: Long? = null,
    val installmentIndex: Int? = null,
    val title: String? = null,
    val note: String? = null,
    val tags: String? = null,
    val attachmentUri: String? = null,
    val interestMinor: Long = 0,
    val feeMinor: Long = 0,
    val flowSign: Int = 1,
    val groupId: String? = null,
    val reversesTransactionId: Long? = null,
)

/** Result of a write. Validation errors are shown verbatim to the user in Bangla. */
sealed interface WriteResult {
    data class Success(val id: Long) : WriteResult
    data class Invalid(val message: String) : WriteResult
    data class Failed(val message: String) : WriteResult
}
