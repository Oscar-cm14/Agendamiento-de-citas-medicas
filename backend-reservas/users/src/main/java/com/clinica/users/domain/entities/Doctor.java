package com.clinica.users.domain.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a doctor in the clinic system.
 * Inherits from Person.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "doctor")
public final class Doctor extends Person {

    private String specialty;
    private String licenseNumber;
    private Boolean active;

}
