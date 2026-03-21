package com.clinica.shared.domain.exceptions;

/**
 * Custom exception representing a domain validation error when an identification
 * Document already exists in the system.
 */
public class IdentificationAlreadyExistsException extends DuplicateResourceException {

    /**
     * Constructs a new exception with the specified message.
     * @param message The detail message.
     */
    public IdentificationAlreadyExistsException(String message) {
        super(message);
    }
}
