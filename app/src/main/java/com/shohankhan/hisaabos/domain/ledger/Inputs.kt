package com.shohankhan.hisaabos.domain.ledger

import com.shohankhan.hisaabos.domain.loan.ScheduleRow
import com.shohankhan.hisaabos.domain.model.BizDate
import com.shohankhan.hisaabos.domain.model.DebtDirection
import com.shohankhan.hisaabos.domain.model.InterestModel
import com.shohankhan.hisaabos.domain.model.LoanKind
import com.shohankhan.hisaabos.domain.model.RepaymentFrequency
import com.shohankhan.hisaabos.domain.model.SavingsKind

data class PurchaseItemInput(
    val name: String,
    val quantityMilli: Long = 1000,
    val unit: String = "পিস",
    val unitPriceMinor: Long,
)

data class PurchaseInput(
    val shopId: Long? = null,
    val personId: Long? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val businessDate: String = BizDate.of(occurredAt),
    val items: List<PurchaseItemInput> = emptyList(),
    val discountMinor: Long = 0,
    val extraChargeMinor: Long = 0,
    val paidMinor: Long = 0,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val note: String? = null,
    /** When null the credit portion is derived as total − paid. */
    val creditMinor: Long? = null,
)

data class DebtInput(
    val direction: DebtDirection,
    val personId: Long,
    val principalMinor: Long,
    val interestMinor: Long = 0,
    val occurredAt: Long = System.currentTimeMillis(),
    val businessDate: String = BizDate.of(occurredAt),
    val dueDate: String? = null,
    val accountId: Long? = null,
    val note: String? = null,
)

data class LoanInput(
    val kind: LoanKind,
    val institution: String,
    val branch: String? = null,
    val reference: String? = null,
    val principalMinor: Long,
    val interestMinor: Long = 0,
    val feeMinor: Long = 0,
    val totalRepayableMinor: Long? = null,
    val model: InterestModel = InterestModel.MANUAL,
    val annualRateBp: Int = 0,
    val frequency: RepaymentFrequency = RepaymentFrequency.MONTHLY,
    val installmentCount: Int = 0,
    val installmentAmountMinor: Long = 0,
    val firstDueDate: String? = null,
    val disbursementDate: String = BizDate.today(),
    val accountId: Long? = null,
    val notes: String? = null,
    /** Manual schedule entered by the user (used when model == MANUAL). */
    val manualSchedule: List<ScheduleRow> = emptyList(),
)

data class EmiInput(
    val productName: String,
    val merchant: String? = null,
    val purchaseDate: String = BizDate.today(),
    val cashPriceMinor: Long? = null,
    val totalFinancedPriceMinor: Long,
    val downPaymentMinor: Long = 0,
    val interestMinor: Long = 0,
    val feeMinor: Long = 0,
    val frequency: RepaymentFrequency = RepaymentFrequency.MONTHLY,
    val installmentCount: Int,
    val firstDueDate: String,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val notes: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
)

data class SavingsPlanInput(
    val name: String,
    val institution: String? = null,
    val kind: SavingsKind = SavingsKind.CASH,
    val targetAmountMinor: Long? = null,
    val targetDate: String? = null,
    val contributionAmountMinor: Long? = null,
    val note: String? = null,
    val accountName: String? = null,
)
