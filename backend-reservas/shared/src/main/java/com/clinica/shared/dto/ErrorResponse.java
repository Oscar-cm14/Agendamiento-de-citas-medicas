package com.clinica.shared.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing the standard error payload
 * returned to the client when an exception occurs.
 *
 * @param timestamp The exact time the error occurred.
 * @param status    The HTTP status code.
 * @param message   A human-readable error message.
 * @param details   Additional details, usually the requested URI.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String message,
        String details
) {
}
