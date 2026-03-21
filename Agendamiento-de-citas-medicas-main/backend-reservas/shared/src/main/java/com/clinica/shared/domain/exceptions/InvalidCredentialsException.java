package com.clinica.shared.domain.exceptions;

/**
 * Exception thrown when authentication fails due to incorrect username or password.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
