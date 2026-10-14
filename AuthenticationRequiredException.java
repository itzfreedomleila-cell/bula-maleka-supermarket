package net.codejava.common.exception;

/** The user must log in first (HTTP 401). */
public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException() {
        super("Please log in to continue.");
    }
}
