package com.clinica.shared.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for creating a new appointment.
 * Used by both scheduler (RF2) and patient (RF3).
 */
public record AppointmentRequest(

        @NotNull(message = "El ID del médico es obligatorio")
        Long doctorId,

        @NotNull(message = "El ID del paciente es obligatorio")
        Long patientId,

        @NotNull(message = "La fecha es obligatoria")
        @FutureOrPresent(message = "La fecha no puede ser en el pasado")
        LocalDate date,

        @NotNull(message = "La hora es obligatoria")
        LocalTime startTime,

        String notes
) {}
