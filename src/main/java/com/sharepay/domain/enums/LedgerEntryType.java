package com.sharepay.domain.enums;

/**
 * Double-entry ledger posting types. Credits increase a member's balance
 * (they are owed money); debits decrease it (they owe money). Every transaction
 * posts balanced credits and debits, so the sum of all balances in an event is zero.
 */
public enum LedgerEntryType {
    PAYMENT_CREDIT(true),
    SHARE_DEBIT(false),
    SPONSOR_DEBIT(false),
    ADJUSTMENT_CREDIT(true),
    ADJUSTMENT_DEBIT(false),
    REFUND_CREDIT(true),
    REFUND_DEBIT(false);

    private final boolean credit;

    LedgerEntryType(boolean credit) {
        this.credit = credit;
    }

    public boolean isCredit() {
        return credit;
    }

    public boolean isDebit() {
        return !credit;
    }
}
