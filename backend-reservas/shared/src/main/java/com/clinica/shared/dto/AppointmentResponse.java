package com.clinica.shared.dto;

import com.clinica.shared.domain.entities.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentResponse(
        Long id,
        Long doctorId,
        String doctorName,
        String specialty,
        Long patientId,
        String patientName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        AppointmentStatus status,
        String notes,
        String cancellationReason,

        // ── CAMPOS DE PRIORIDAD ─────────────────────────────────────
        boolean priority,
        String urgencyLevel,
        String priorityReason
) {}