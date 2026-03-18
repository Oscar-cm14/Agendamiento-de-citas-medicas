package com.clinica.users.domain.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Abstract sealed entity representing a general person in the clinic system.
 * Uses JOINED inheritance meaning each subclass maps to its own table.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "person")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract sealed class Person permits Admin, Patient, Doctor, Scheduler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String identification;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}
