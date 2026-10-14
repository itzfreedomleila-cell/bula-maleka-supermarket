package net.codejava.account.model;

/** Whether an account can still be used. */
public enum AccountStatus {
    ACTIVE,
    /** The customer deleted their account. Personal details have been removed. */
    DELETED
}
