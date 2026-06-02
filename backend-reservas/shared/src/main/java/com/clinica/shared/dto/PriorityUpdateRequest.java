package com.clinica.shared.dto;

/**
 * DTO para actualizar el nivel de prioridad de una cita.
 * Usado por PATCH /appointments/{id}/priority
 */
public record PriorityUpdateRequest(
        Boolean priority,
        String urgencyLevel,
        String priorityReason
) {}
