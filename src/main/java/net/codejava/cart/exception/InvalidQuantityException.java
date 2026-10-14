package net.codejava.cart.exception;

/** Thrown when a quantity is zero, negative or above the allowed maximum. */
public class InvalidQuantityException extends CartException {

    public InvalidQuantityException(String message) {
        super(message);
    }
}
