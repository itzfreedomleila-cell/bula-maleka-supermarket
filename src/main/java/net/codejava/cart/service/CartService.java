package net.codejava.cart.service;

import java.math.BigDecimal;
import net.codejava.cart.dto.CartDto;
import net.codejava.cart.dto.CheckoutValidationResult;

/**
 * Module 4 - Shopping Cart operations.
 * <p>
 * Controllers depend on this interface, not on the implementation, which
 * keeps coupling low and makes the service easy to test or replace.
 */
public interface CartService {

    /** Returns the customer's cart, creating an empty one if needed. */
    CartDto getCart(String customerId);

    /** Add Product: adds a product, or increases its quantity if already in the cart. */
    CartDto addProduct(String customerId, Long productId, int quantity);

    /** Remove Product: removes a product line from the cart. */
    CartDto removeProduct(String customerId, Long productId);

    /** Update Product Quantity: sets a new quantity (0 removes the item). */
    CartDto updateQuantity(String customerId, Long productId, int quantity);

    /** Calculate Subtotal: unit price x quantity for one product in the cart. */
    BigDecimal calculateSubtotal(String customerId, Long productId);

    /** Calculate Cart Total: sum of all subtotals. */
    BigDecimal calculateCartTotal(String customerId);

    /** Validate Cart for Checkout: checks the cart is ready to pay for. */
    CheckoutValidationResult validateForCheckout(String customerId);

    /** Save Cart: marks the cart as saved so it can be restored later. */
    CartDto saveCart(String customerId);

    /** Restore Cart: brings a saved cart back into active use. */
    CartDto restoreCart(String customerId);

    /**
     * Called by Module 5 after a successful payment: takes the purchased
     * quantities out of stock and empties the cart.
     */
    void completeCheckout(String customerId);
}
