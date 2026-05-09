package com.clinica.shared.dto;

/**
 * DTO para cancelar una cita con justificación.
 */
public record CancelRequest(
        String reason  // motivo de cancelación (obligatorio)
) {}