package com.clinica.shared.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for rescheduling an existing appointment.
 */
public record RescheduleAppointmentRequest(

        @NotNull(message = "La nueva fecha es obligatoria")
        @FutureOrPresent(message = "La fecha no puede ser en el pasado")
        LocalDate newDate,

        @NotNull(message = "La nueva hora de inicio es obligatoria")
        LocalTime newStartTime,

        String reason
) {}
