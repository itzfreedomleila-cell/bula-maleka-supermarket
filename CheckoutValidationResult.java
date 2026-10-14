package net.codejava.cart.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of checking whether a cart is ready for checkout.
 *
 * @param valid        true if the cart can go to payment
 * @param errors       reasons the cart is not valid (empty when valid)
 * @param total        the cart total at the time of validation
 * @param minimumOrder the minimum online order value (from configuration)
 */
public record CheckoutValidationResult(
        boolean valid,
        List<String> errors,
        BigDecimal total,
        BigDecimal minimumOrder) {
}
