package com.clinica.shared.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * DTO para actualizar información del paciente.
 */
public record PatientUpdateRequest(

        @NotBlank(message = "El nombre es obligatorio")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        String lastName,

        @Email(message = "Correo inválido")
        String email,

        String phone,

        String gender,

        LocalDate birthDate
) {
}