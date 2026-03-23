package com.clinica.shared.dto;

/**
 * Data Transfer Object for the JWT authentication success response.
 * Utilizes Java 25 Records for immutability.
 *
 * @param token    The generated JSON Web Token string.
 * @param username The authenticated username.
 * @param role     The role assigned to the user.
 */
public record JwtResponse(
        String token,
        String username,
        String role
) {
}
