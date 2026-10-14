package net.codejava.cart.model;

/**
 * Lifecycle states of a shopping cart.
 */
public enum CartStatus {
    /** The customer is currently adding/removing items. */
    ACTIVE,
    /** The customer saved the cart so it can be restored later. */
    SAVED
}
