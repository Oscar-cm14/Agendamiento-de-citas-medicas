package com.clinica.shared.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Record representing the request to update the global system configuration.
 * DTO uses Java Record as per Java 25 features requirement.
 *
 * @param appointmentWindowWeeks the maximum allowed weeks in advance a patient can schedule an appointment.
 */
public record ConfigurationRequest(
        @NotNull(message = "Appointment window weeks cannot be null")
        @Min(value = 1, message = "Appointment window must be at least 1 week")
        Integer appointmentWindowWeeks
) {
}
