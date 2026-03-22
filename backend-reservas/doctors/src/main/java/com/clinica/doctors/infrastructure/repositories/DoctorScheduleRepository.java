package com.clinica.doctors.infrastructure.repositories;

import com.clinica.doctors.domain.entities.DoctorSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for DoctorSchedule entity.
 */
@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    /**
     * Finds all schedules associated with a specific doctor.
     *
     * @param doctorId the ID of the doctor.
     * @return a list of DoctorSchedule entities.
     */
    List<DoctorSchedule> findByDoctorId(Long doctorId);
}
