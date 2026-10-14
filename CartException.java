package net.codejava.cart.exception;

/**
 * Base class for all shopping-cart errors. Subclasses describe specific
 * problems so the API can return the right HTTP status.
 */
public class CartException extends RuntimeException {

    public CartException(String message) {
        super(message);
    }
}
