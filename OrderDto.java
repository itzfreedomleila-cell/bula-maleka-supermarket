package net.codejava.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import net.codejava.order.model.CustomerOrder;
import net.codejava.order.model.OrderStatus;

/** An order as shown to the customer or staff. */
public record OrderDto(
        String orderNumber,
        String customerId,
        String customerName,
        OrderStatus status,
        String paymentReference,
        String paymentMethod,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal surcharge,
        BigDecimal total,
        BigDecimal refundedAmount,
        LocalDateTime createdAt,
        LocalDateTime confirmedAt,
        String receiptNumber,
        List<Item> items,
        String confirmationMessage) {

    public record Item(String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }

    public static OrderDto from(CustomerOrder o, String customerName) {
        return new OrderDto(o.getOrderNumber(), o.getCustomerId(), customerName, o.getStatus(),
                o.getPaymentReference(), o.getPaymentMethod(), o.getSubtotal(), o.getDeliveryFee(),
                o.getSurcharge(), o.getTotal(), o.getRefundedAmount(), o.getCreatedAt(), o.getConfirmedAt(),
                o.getReceiptNumber(),
                o.getItems().stream()
                        .map(i -> new Item(i.getProductName(), i.getQuantity(), i.getUnitPrice(), i.getSubtotal()))
                        .toList(),
                confirmationMessage(o));
    }

    private static String confirmationMessage(CustomerOrder o) {
        return switch (o.getStatus()) {
            case CREATED -> "Your order has been created.";
            case CONFIRMED -> "Order " + o.getOrderNumber() + " is confirmed. Book a delivery slot to receive it.";
            case DELIVERY_SCHEDULED -> "Order " + o.getOrderNumber() + " is confirmed and booked for delivery.";
            case DELIVERED -> "Order " + o.getOrderNumber() + " has been delivered.";
            case REFUNDED -> "Order " + o.getOrderNumber() + " was refunded.";
        };
    }
}
