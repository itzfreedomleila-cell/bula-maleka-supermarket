package net.codejava.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import net.codejava.payment.model.PaymentTransaction;
import net.codejava.payment.model.TransactionStatus;

/**
 * A transaction as returned to the client: receipt, status history and refunds.
 *
 * @param duplicate true if this response is for a repeated request that was
 *                  NOT charged again (Duplicate Payment Prevention)
 * @param message   a plain-language confirmation or error message
 */
public record TransactionDto(
        String reference,
        String customerId,
        TransactionStatus status,
        String methodLabel,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal surcharge,
        BigDecimal totalAmount,
        BigDecimal refundedAmount,
        BigDecimal refundableAmount,
        boolean refundable,
        String failureReason,
        String gatewayReference,
        LocalDateTime createdAt,
        List<LineDto> lines,
        List<EventDto> events,
        List<RefundDto> refunds,
        boolean duplicate,
        String message) {

    public record LineDto(String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }

    public record EventDto(TransactionStatus status, String message, LocalDateTime at) {
    }

    public record RefundDto(BigDecimal amount, String reason, LocalDateTime at) {
    }

    public static TransactionDto from(PaymentTransaction t, boolean duplicate) {
        return new TransactionDto(
                t.getReference(),
                t.getCustomerId(),
                t.getStatus(),
                t.getMethodLabel(),
                t.getSubtotal(),
                t.getDeliveryFee(),
                t.getSurcharge(),
                t.getTotalAmount(),
                t.getRefundedAmount(),
                t.getRefundableAmount(),
                t.canBeRefunded(),
                t.getFailureReason(),
                t.getGatewayReference(),
                t.getCreatedAt(),
                t.getLines().stream()
                        .map(l -> new LineDto(l.getProductName(), l.getQuantity(), l.getUnitPrice(), l.getSubtotal()))
                        .toList(),
                t.getEvents().stream()
                        .map(e -> new EventDto(e.getStatus(), e.getMessage(), e.getCreatedAt()))
                        .toList(),
                t.getRefunds().stream()
                        .map(r -> new RefundDto(r.getAmount(), r.getReason(), r.getCreatedAt()))
                        .toList(),
                duplicate,
                messageFor(t, duplicate));
    }

    private static String messageFor(PaymentTransaction t, boolean duplicate) {
        String prefix = duplicate ? "This payment was already submitted, so you were not charged again. " : "";
        return prefix + switch (t.getStatus()) {
            case SUCCESS -> "Payment successful. Your order reference is " + t.getReference() + ".";
            case FAILED -> "Payment failed: " + t.getFailureReason();
            case REFUNDED -> "This payment has been fully refunded.";
            case PARTIALLY_REFUNDED -> "Part of this payment has been refunded.";
            case PENDING, PROCESSING -> "Payment is being processed.";
        };
    }
}
