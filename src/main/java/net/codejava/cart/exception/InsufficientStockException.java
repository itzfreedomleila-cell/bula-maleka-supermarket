package net.codejava.cart.exception;

/** Thrown when the customer asks for more units than are in stock. */
public class InsufficientStockException extends CartException {

    public InsufficientStockException(String productName, int available) {
        super("Only " + available + " of '" + productName + "' in stock.");
    }
}
