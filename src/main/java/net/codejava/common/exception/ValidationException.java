package net.codejava.common.exception;

import java.util.List;

/** Input failed validation. Carries every problem so the user can fix them all at once (HTTP 400). */
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(errors.size() == 1 ? errors.get(0) : "Please fix the problems below.");
        this.errors = List.copyOf(errors);
    }

    public ValidationException(String error) {
        this(List.of(error));
    }

    public List<String> getErrors() {
        return errors;
    }
}
