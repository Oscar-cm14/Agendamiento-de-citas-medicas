package com.clinica.users.infrastructure.repositories;

import com.clinica.users.domain.entities.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Patient entities.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
}
