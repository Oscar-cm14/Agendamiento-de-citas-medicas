package com.clinica.shared.dto;

/**
 * DTO para actualizar los datos de un médico existente.
 * Todos los campos son opcionales; solo se actualizan los que vienen no nulos.
 */
public record DoctorUpdateRequest(
        String identification,
        String firstName,
        String lastName,
        String email,
        String phone,
        String specialty,
        String licenseNumber,
        String skills,
        String password
) {}