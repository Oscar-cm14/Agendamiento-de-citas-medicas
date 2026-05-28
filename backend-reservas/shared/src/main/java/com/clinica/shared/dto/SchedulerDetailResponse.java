package com.clinica.shared.dto;

/**
 * DTO completo del agendador para el panel de edición del administrador.
 */
public record SchedulerDetailResponse(
        Long id,
        String fullName,
        String firstName,
        String lastName,
        String identification,
        String email,
        String phone,
        String username
) {}