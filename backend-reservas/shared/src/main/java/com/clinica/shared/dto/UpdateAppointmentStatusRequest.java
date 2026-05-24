package com.clinica.shared.dto;

import com.clinica.shared.domain.entities.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAppointmentStatusRequest(
    @NotNull(message = "El estado es obligatorio")
    AppointmentStatus status
) {
}
