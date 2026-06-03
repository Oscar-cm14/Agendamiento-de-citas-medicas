package com.clinica.shared.dto;

/**
 * DTO para actualizar los datos de un agendador existente.
 */
public record SchedulerUpdateRequest(
        String identification,
        String firstName,
        String lastName,
        String email,
        String phone
) {}