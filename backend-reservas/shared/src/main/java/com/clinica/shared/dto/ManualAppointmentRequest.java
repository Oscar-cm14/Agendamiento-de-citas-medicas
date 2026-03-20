package com.clinica.shared.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for manually scheduling an appointment.
 *
 * @param patientId   The ID of the patient.
 * @param doctorId    The ID of the doctor.
 * @param dateTime    The required date and time of the appointment.
 * @param observation Any additional observations.
 */
public record ManualAppointmentRequest(
        @NotNull(message = "Patient ID is required") Long patientId,
        @NotNull(message = "Doctor ID is required") Long doctorId,
        @NotNull(message = "Appointment date and time is required") LocalDateTime dateTime,
        String observation
) {
}
