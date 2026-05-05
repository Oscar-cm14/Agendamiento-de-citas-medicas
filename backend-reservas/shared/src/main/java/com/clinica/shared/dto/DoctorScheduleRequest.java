package com.clinica.shared.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/**
 * DTO for configuring a doctor's weekly schedule.
 * Used in RF4 by the administrator.
 */
public record DoctorScheduleRequest(

        @NotNull(message = "El ID del médico es obligatorio")
        Long doctorId,

        @NotEmpty(message = "Debe seleccionar al menos un día")
        Set<DayOfWeek> workingDays,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime startTime,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime endTime,

        @NotNull(message = "El intervalo es obligatorio")
        @Min(value = 10, message = "El intervalo mínimo es 10 minutos")
        Integer intervalMinutes
) {}
