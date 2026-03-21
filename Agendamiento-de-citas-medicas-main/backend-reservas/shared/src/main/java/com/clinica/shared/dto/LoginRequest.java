package com.clinica.shared.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for login credentials.
 * Utilizes Java 25 Records for immutability and Jakarta Validation.
 *
 * @param username The exact username. Cannot be blank.
 * @param password The raw password. Cannot be blank.
 */
public record LoginRequest(
        @NotBlank(message = "Username cannot be blank")
        String username,
        
        @NotBlank(message = "Password cannot be blank")
        String password
) {
}
