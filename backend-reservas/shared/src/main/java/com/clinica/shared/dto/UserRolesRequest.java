package com.clinica.shared.dto;

import java.util.List;

/**
 * DTO para asignar/reemplazar los roles de un usuario en Keycloak.
 */
public record UserRolesRequest(
        List<String> roles
) {}