package com.clinica.shared.domain.exceptions;

/**
 * Custom exception representing a domain validation error when a username
 * already exists in the system.
 */
public class UsernameAlreadyExistsException extends DuplicateResourceException {

    /**
     * Constructs a new exception with the specified message.
     * @param message The detail message.
     */
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
