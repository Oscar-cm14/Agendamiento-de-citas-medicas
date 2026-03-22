package com.clinica.doctors.infrastructure.repositories;


import com.clinica.doctors.domain.entities.Doctor;  
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Doctor entities.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
}