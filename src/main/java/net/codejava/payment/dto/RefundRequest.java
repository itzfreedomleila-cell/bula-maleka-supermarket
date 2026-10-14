package net.codejava.payment.dto;

import java.math.BigDecimal;

/**
 * JSON body for a refund. Leave amount empty for a full refund.
 */
public record RefundRequest(BigDecimal amount, String reason) {
}
