package com.clinica.shared.dto;

/**
 * DTO returned after a patient is successfully registered or re-synchronized.
 *
 * @param _sincronizado true cuando el paciente ya existía en H2 y se re-sincronizó
 *                      con Keycloak (actualización de contraseña).
 */
public record PatientResponse(

        Long id,
        String fullName,
        String username,
        String email,
        boolean _sincronizado
) {}