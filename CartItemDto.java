package net.codejava.cart.dto;

import java.math.BigDecimal;
import net.codejava.cart.model.CartItem;

/**
 * What the client sees for one cart line. DTOs keep JPA entities out of the
 * JSON responses (avoids infinite Cart -> Item -> Cart loops).
 */
public record CartItemDto(
        Long productId,
        String productName,
        String imageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal) {

    public static CartItemDto from(CartItem item) {
        return new CartItemDto(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getImageUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal());
    }
}
