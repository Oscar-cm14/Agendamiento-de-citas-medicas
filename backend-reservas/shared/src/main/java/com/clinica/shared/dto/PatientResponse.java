package com.clinica.shared.dto;

/**
 * DTO returned after a patient is successfully registered.
 */
public record PatientResponse(

        Long id,
        String fullName,
        String username,
        String email
) {}