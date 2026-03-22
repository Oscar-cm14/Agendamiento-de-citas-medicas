package com.clinica.shared.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO for registering a new patient from the web (RF3).
 */
public record PatientRegistrationRequest(

        @NotBlank(message = "El número de documento es obligatorio")
        String identification,

        @NotBlank(message = "El nombre es obligatorio")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        String lastName,

        @NotBlank(message = "El celular es obligatorio")
        String phone,

        @NotNull(message = "El género es obligatorio")
        String gender,
        
        LocalDate birthDate,

        @Email(message = "Correo inválido")
        String email,

        @NotBlank(message = "El username es obligatorio")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}