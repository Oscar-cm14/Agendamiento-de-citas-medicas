package com.clinica.shared.dto;

import com.clinica.shared.domain.entities.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for returning appointment data to the client.
 * Used in RF1 (list appointments) and RF2/RF3 (create appointment).
 */
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
        String notes
) {}