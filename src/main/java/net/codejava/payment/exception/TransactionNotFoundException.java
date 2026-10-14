package net.codejava.payment.exception;

/** Thrown when a transaction reference is unknown. */
public class TransactionNotFoundException extends PaymentException {

    public TransactionNotFoundException(String reference) {
        super("No transaction found with reference '" + reference + "'.");
    }
}
