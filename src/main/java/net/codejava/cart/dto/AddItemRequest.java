package net.codejava.cart.dto;

/**
 * JSON body for adding a product to the cart, e.g. {"productId": 1, "quantity": 2}.
 */
public record AddItemRequest(Long productId, int quantity) {
}
