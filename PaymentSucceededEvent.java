package net.codejava.payment.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Published when a payment is approved. Module 6 listens for it to create
 * and confirm the order, so the payment module does not depend on orders.
 */
public record PaymentSucceededEvent(
        String paymentReference,
        String customerId,
        String methodLabel,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal surcharge,
        BigDecimal total,
        List<Line> lines) {

    /** One product bought. */
    public record Line(Long productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }
}
