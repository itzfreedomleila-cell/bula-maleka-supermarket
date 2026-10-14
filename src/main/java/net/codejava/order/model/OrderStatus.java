package net.codejava.order.model;

/** Stages of an order. */
public enum OrderStatus {
    /** Order Creation: made from a successful payment. */
    CREATED,
    /** Order Confirmation: accepted by the store; the customer can book delivery. */
    CONFIRMED,
    /** A delivery slot has been booked (Module 9). */
    DELIVERY_SCHEDULED,
    /** Handed to the customer. */
    DELIVERED,
    /** The payment was fully refunded. */
    REFUNDED
}
