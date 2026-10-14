package net.codejava.payment.exception;

import java.util.List;

/** Thrown when payment details or the cart fail validation. Carries every problem found. */
public class PaymentValidationException extends PaymentException {

    private final List<String> errors;

    public PaymentValidationException(List<String> errors) {
        super(errors.size() == 1 ? errors.get(0) : "Please fix the problems below.");
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
