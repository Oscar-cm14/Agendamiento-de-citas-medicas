package com.clinica.shared.dto;

/**
 * Data Transfer Object (Record) returning the basic information
 * of a newly registered Doctor.
 * This record is immutable by default in Java.
 *
 * @param id       The generated ID of the doctor.
 * @param fullName The full name (first + last).
 */
public record DoctorResponse(
        Long id,
        String fullName
) {
}
