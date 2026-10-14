package net.codejava.cart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import net.codejava.cart.model.Cart;
import net.codejava.cart.model.CartStatus;

/**
 * What the client sees for the whole cart.
 */
public record CartDto(
        String customerId,
        CartStatus status,
        List<CartItemDto> items,
        int itemCount,
        BigDecimal total,
        LocalDateTime savedAt) {

    public static CartDto from(Cart cart) {
        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(CartItemDto::from)
                .toList();
        return new CartDto(
                cart.getCustomerId(),
                cart.getStatus(),
                itemDtos,
                cart.getItemCount(),
                cart.getTotal(),
                cart.getSavedAt());
    }
}
