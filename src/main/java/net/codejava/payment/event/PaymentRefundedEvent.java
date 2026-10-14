package net.codejava.payment.event;

import java.math.BigDecimal;

/**
 * Published after a refund so Module 6 can update the order.
 *
 * @param fullyRefunded true if the whole payment has now been returned
 */
public record PaymentRefundedEvent(String paymentReference, BigDecimal amount, BigDecimal totalRefunded,
                                   boolean fullyRefunded) {
}
