package net.codejava.payment.dto;

import java.math.BigDecimal;
import net.codejava.payment.model.PaymentMethodType;

/**
 * How the amount to pay was worked out (Payment Amount Calculation).
 * total = subtotal + deliveryFee + surcharge
 */
public record AmountBreakdown(
        PaymentMethodType methodType,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal surcharge,
        BigDecimal total,
        BigDecimal freeDeliveryThreshold) {
}
