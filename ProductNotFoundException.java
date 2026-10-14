package net.codejava.cart.exception;

/** Thrown when a product does not exist, is not for sale, or is not in the cart. */
public class ProductNotFoundException extends CartException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}
