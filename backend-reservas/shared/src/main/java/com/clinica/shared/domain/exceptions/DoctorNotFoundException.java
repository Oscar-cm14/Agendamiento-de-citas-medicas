package com.clinica.shared.domain.exceptions;

/**
 * Exception thrown when a Doctor entity is not found in the system.
 */
public class DoctorNotFoundException extends RuntimeException {
    public DoctorNotFoundException(String message) {
        super(message);
    }
}
