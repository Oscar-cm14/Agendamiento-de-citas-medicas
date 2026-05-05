package com.clinica.shared.dto;


import jakarta.validation.constraints.NotBlank;

public record SchedulerRegistrationRequest(
        @NotBlank String identification,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone,
        String email,
        @NotBlank String username,
        @NotBlank String password
) {}