package net.codejava.common.exception;

/** The user is logged in but not allowed to do this (HTTP 403). */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
