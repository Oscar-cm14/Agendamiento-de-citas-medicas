package com.clinica.shared.dto;

 
public record DoctorDetailResponse(
        Long   id,
        String fullName,
        String firstName,
        String lastName,
        String identification,
        String email,
        String phone,
        String specialty,
        String licenseNumber,
        String skills   // habilidades adicionales, separadas por coma
) {}