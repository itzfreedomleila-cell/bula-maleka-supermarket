package net.codejava.cart.exception;

/** Thrown when a customer has no cart yet. */
public class CartNotFoundException extends CartException {

    public CartNotFoundException(String customerId) {
        super("No cart found for customer '" + customerId + "'.");
    }
}
