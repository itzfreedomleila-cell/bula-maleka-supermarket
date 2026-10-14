package net.codejava.payment.exception;

/** Base class for all payment errors (Payment Error Handling). */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }
}
