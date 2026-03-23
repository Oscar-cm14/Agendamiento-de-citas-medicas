package com.clinica.doctors.domain.entities;

import com.clinica.shared.domain.entities.Person; 
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
@Table(name = "doctors")
public class Doctor extends Person {

    private String specialty;
    private String licenseNumber;
    private Boolean active;

}

