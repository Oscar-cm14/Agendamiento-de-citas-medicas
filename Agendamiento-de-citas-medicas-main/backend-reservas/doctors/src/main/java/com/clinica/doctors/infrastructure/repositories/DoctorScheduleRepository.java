package com.clinica.doctors.infrastructure.repositories;



import com.clinica.doctors.domain.entities.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing DoctorSchedule entities.
 */
@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    // Buscar el horario de un médico específico
    Optional<DoctorSchedule> findByDoctorId(Long doctorId);
}