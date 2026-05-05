package com.clinica.users.domain.entities;

import com.clinica.shared.domain.entities.Person;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a scheduler in the clinic system.
 * Inherits from Person.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "scheduler")
public class Scheduler extends Person {

}
