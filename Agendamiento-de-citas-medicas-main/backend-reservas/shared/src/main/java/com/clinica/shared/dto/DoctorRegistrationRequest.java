package com.clinica.shared.dto;

/**
 * Data Transfer Object (Record) containing all necessary information
 * to register a new Doctor and create their User account.
 * This record is immutable by default in Java.
 *
 * @param identification The identification document number.
 * @param firstName      The first name of the doctor.
 * @param lastName       The last name of the doctor.
 * @param email          The email address of the doctor.
 * @param phone          The contact phone number.
 * @param specialty      The medical specialty of the doctor.
 * @param licenseNumber  The professional medical license number.
 * @param username       The desired username for the account.
 * @param password       The plain text password (to be encrypted).
 */
public record DoctorRegistrationRequest(
        String identification,
        String firstName,
        String lastName,
        String email,
        String phone,
        String specialty,
        String licenseNumber,
        String username,
        String password
) {
}
