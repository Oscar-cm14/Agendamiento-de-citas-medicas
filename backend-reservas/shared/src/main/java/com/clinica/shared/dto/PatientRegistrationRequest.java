package com.clinica.shared.dto;

import com.clinica.shared.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Data Transfer Object for registering a new Patient.
 * Email and birthDate are optional.
 *
 * @param identification Unique identification document number.
 * @param firstName      Patient's first name.
 * @param lastName       Patient's last name.
 * @param phone          Patient's phone number.
 * @param email          Patient's email address (optional).
 * @param gender         Patient's gender.
 * @param birthDate      Patient's birth date (optional).
 */
public record PatientRegistrationRequest(
        @NotBlank(message = "Identification cannot be blank")
        String identification,

        @NotBlank(message = "First name cannot be blank")
        String firstName,

        @NotBlank(message = "Last name cannot be blank")
        String lastName,

        @NotBlank(message = "Phone number cannot be blank")
        String phone,

        String email, // Optional

        @NotNull(message = "Gender is required")
        Gender gender,

        LocalDate birthDate // Optional
) {
}
