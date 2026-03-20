package com.clinica.users.domain.entities;

import com.clinica.shared.domain.Gender;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a patient in the clinic system.
 * Inherits from Person.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patient")
public class Patient extends Person {

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate birthDate;
}
