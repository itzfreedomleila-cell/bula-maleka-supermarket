package net.codejava.payment.exception;

/** Thrown when a payment method does not exist or belongs to another customer. */
public class PaymentMethodNotFoundException extends PaymentException {

    public PaymentMethodNotFoundException(Long id) {
        super("Payment method " + id + " was not found. Choose or add a payment method.");
    }
}
