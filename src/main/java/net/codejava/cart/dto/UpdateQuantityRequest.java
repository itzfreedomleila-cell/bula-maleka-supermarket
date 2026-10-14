package net.codejava.cart.dto;

/**
 * JSON body for changing an item's quantity, e.g. {"quantity": 3}.
 * A quantity of 0 removes the item.
 */
public record UpdateQuantityRequest(int quantity) {
}
