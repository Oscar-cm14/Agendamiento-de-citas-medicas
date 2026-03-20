package com.clinica.appointments.infrastructure.repositories;

import com.clinica.appointments.domain.entities.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for managing Appointment entities.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Finds appointments for a specific doctor within a given time range.
     * Useful for checking schedule conflicts (cruces de horarios).
     *
     * @param doctorId The ID of the doctor.
     * @param start    The start date and time.
     * @param end      The end date and time.
     * @return List of matched appointments.
     */
    List<Appointment> findByDoctorIdAndAppointmentDateTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);

    /**
     * Checks if a doctor already has an appointment at a specific date and time.
     *
     * @param doctorId            The ID of the doctor.
     * @param appointmentDateTime The exact date and time.
     * @return true if an appointment exists, false otherwise.
     */
    boolean existsByDoctorIdAndAppointmentDateTime(Long doctorId, LocalDateTime appointmentDateTime);
}
