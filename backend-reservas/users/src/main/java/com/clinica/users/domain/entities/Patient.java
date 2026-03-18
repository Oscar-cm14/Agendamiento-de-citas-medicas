package com.clinica.users.domain.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
public final class Patient extends Person {

}
