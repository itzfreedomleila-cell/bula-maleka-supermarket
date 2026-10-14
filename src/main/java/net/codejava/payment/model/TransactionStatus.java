package net.codejava.payment.model;

/**
 * Stages a payment transaction moves through (Transaction Status Tracking).
 */
public enum TransactionStatus {
    /** Created, not yet sent to the payment provider. */
    PENDING,
    /** Sent to the payment provider, waiting for an answer. */
    PROCESSING,
    /** Approved: money moved to the store. */
    SUCCESS,
    /** Declined or an error happened. No money moved. */
    FAILED,
    /** Some of the money was given back. */
    PARTIALLY_REFUNDED,
    /** All of the money was given back. */
    REFUNDED
}
