package com.clinica.shared.dto;

import java.util.List;

/**
 * DTO resumen de usuario para la lista de gestión de roles.
 */
public record UserSummaryResponse(
        Long id,
        String username,
        String fullName,
        String email,
        List<String> roles
) {}