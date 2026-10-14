package net.codejava.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Everything printed on a receipt (Receipt Creation). */
public record ReceiptDto(
        String storeName,
        String storeAddress,
        String storePhone,
        String receiptNumber,
        LocalDateTime issuedAt,
        String orderNumber,
        String customerName,
        List<OrderDto.Item> items,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal surcharge,
        BigDecimal total,
        BigDecimal refundedAmount,
        String paymentMethod,
        String paymentReference,
        String note) {
}
