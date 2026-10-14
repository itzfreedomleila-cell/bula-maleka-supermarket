package net.codejava.payment.model;

/** Direction of a money movement recorded in the ledger. */
public enum LedgerEntryType {
    /** Customer to store. */
    PAYMENT,
    /** Store back to customer. */
    REFUND
}
