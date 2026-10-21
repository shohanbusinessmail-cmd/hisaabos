package com.shohankhan.hisaabos.domain.model

/** Every kind of money movement HisaabOS understands. Persisted by name(). */
enum class TxType {
    INCOME,
    EXPENSE,
    TRANSFER,
    CREDIT_PURCHASE,
    SHOP_PAYMENT,
    DEBT_BORROWED,
    DEBT_REPAID,
    LENDING,
    LOAN_COLLECTION,
    LOAN_DISBURSEMENT,
    LOAN_REPAYMENT,
    EMI_PURCHASE,
    EMI_PAYMENT,
    SAVINGS_DEPOSIT,
    SAVINGS_WITHDRAWAL,
    OPENING_BALANCE,
    ADJUSTMENT,
    REFUND;

    /** Cash actually received that is genuinely earnings. */
    val isEarnedIncome: Boolean get() = this == INCOME

    /** Cash actually spent on goods/services. */
    val isCashExpense: Boolean get() = this == EXPENSE

    /** Spending recognised without a cash movement (payable/liability created). */
    val isAccrualExpense: Boolean get() = this == CREDIT_PURCHASE || this == EMI_PURCHASE

    /** Moves money between two accounts the user owns. */
    val isInternalTransfer: Boolean
        get() = this == TRANSFER || this == SAVINGS_DEPOSIT || this == SAVINGS_WITHDRAWAL

    /** Reduces an outstanding payable / liability principal. */
    val isLiabilitySettlement: Boolean
        get() = this == SHOP_PAYMENT || this == DEBT_REPAID || this == LOAN_REPAYMENT || this == EMI_PAYMENT
}

enum class AccountType(val code: String) {
    CASH("CASH"),
    BANK("BANK"),
    MFS("MFS"),
    WALLET("WALLET"),
    SAVINGS("SAVINGS"),
    CREDIT_CARD("CREDIT_CARD"),
    OTHER("OTHER");

    companion object {
        fun from(code: String?): AccountType = entries.firstOrNull { it.code == code } ?: OTHER
    }
}

enum class CategoryKind { INCOME, EXPENSE, BOTH }

enum class PartyKind { NONE, PERSON, SHOP, INSTITUTION }

enum class LinkKind { NONE, PERSON, SHOP, DEBT, LOAN, EMI, SAVINGS, PURCHASE, BUDGET }

enum class DebtDirection { BORROWED, LENT }

enum class LoanKind { NGO, BANK }

/**
 * How a lender's repayment arrangement is expressed. HisaabOS never invents an
 * interest rate: the user picks what the institution actually agreed to.
 */
enum class InterestModel(val code: String) {
    /** Fixed total repayable, principal + flat charge (very common for NGO microfinance). */
    FLAT_TOTAL("FLAT_TOTAL"),

    /** Classic amortising loan with an annual rate. */
    REDUCING_BALANCE("REDUCING_BALANCE"),

    /** Zero-interest instalment plan. */
    ZERO_INTEREST("ZERO_INTEREST"),

    /** User enters the schedule by hand because terms are unknown/irregular. */
    MANUAL("MANUAL");

    companion object {
        fun from(code: String?): InterestModel = entries.firstOrNull { it.code == code } ?: MANUAL
    }
}

enum class RepaymentFrequency(val code: String) {
    WEEKLY("WEEKLY"),
    BIWEEKLY("BIWEEKLY"),
    MONTHLY("MONTHLY"),
    ONCE("ONCE");

    companion object {
        fun from(code: String?): RepaymentFrequency = entries.firstOrNull { it.code == code } ?: MONTHLY
    }
}

enum class ScheduleOwner { LOAN, EMI }

enum class BudgetPeriod(val code: String) {
    WEEKLY("WEEKLY"),
    MONTHLY("MONTHLY"),
    CUSTOM("CUSTOM");

    companion object {
        fun from(code: String?): BudgetPeriod = entries.firstOrNull { it.code == code } ?: MONTHLY
    }
}

enum class SavingsKind(val code: String) {
    CASH("CASH"),
    BANK("BANK"),
    NGO("NGO"),
    RECURRING("RECURRING"),
    GOAL("GOAL"),
    OTHER("OTHER");

    companion object {
        fun from(code: String?): SavingsKind = entries.firstOrNull { it.code == code } ?: OTHER
    }
}

enum class ReminderKind(val code: String) {
    DEBT("DEBT"),
    LOAN("LOAN"),
    EMI("EMI"),
    SAVINGS("SAVINGS"),
    CUSTOM("CUSTOM");

    companion object {
        fun from(code: String?): ReminderKind = entries.firstOrNull { it.code == code } ?: CUSTOM
    }
}

enum class RecurrenceFrequency(val code: String) {
    DAILY("DAILY"),
    WEEKLY("WEEKLY"),
    MONTHLY("MONTHLY");

    companion object {
        fun from(code: String?): RecurrenceFrequency = entries.firstOrNull { it.code == code } ?: MONTHLY
    }
}
