package com.clinica.shared.dto;

/**
 * DTO returning doctor information.
 */
public record DoctorResponse(
        Long id,
        String fullName,
        String specialty
) {}
