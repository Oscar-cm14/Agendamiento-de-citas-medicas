package com.clinica.shared.domain.exceptions;

/**
 * Custom exception representing a domain validation error when a resource
 * already exists (e.g. duplicated username or identification).
 */
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructs a new DuplicateResourceException with the specified message.
     * @param message The detail message.
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}
