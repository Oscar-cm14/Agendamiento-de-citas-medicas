package com.clinica.users.domain.entities;


import com.clinica.shared.domain.entities.Person;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Entity representing a patient in the clinic system.
 * Inherits from Person.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patients")
public class Patient extends Person {

    // RF2: género del paciente (Hombre, Mujer, Otro)
    private String gender;

    // RF2: fecha de nacimiento (opcional)
    private LocalDate birthDate;
}
