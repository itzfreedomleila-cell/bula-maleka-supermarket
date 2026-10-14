package net.codejava.payment.exception;

/** Thrown when a refund is not possible (wrong status or amount). */
public class RefundNotAllowedException extends PaymentException {

    public RefundNotAllowedException(String message) {
        super(message);
    }
}
