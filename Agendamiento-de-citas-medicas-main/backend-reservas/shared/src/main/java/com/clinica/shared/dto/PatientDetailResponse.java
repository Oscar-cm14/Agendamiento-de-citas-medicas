package com.clinica.shared.dto;

import java.time.LocalDate;

/**
 * DTO con los datos completos del paciente.
 * Usado para autocompletar información en el panel del agendador
 * cuando se ingresa un número de cédula ya registrado.
 */
public record PatientDetailResponse(
        Long id,
        String identification,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        String gender,
        LocalDate birthDate,
        String username
) {}