package com.clinica.appointments.infrastructure.repositories;


import com.clinica.appointments.domain.entities.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repository for managing Appointment entities.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // RF1: Listar citas de un médico en una fecha determinada
    List<Appointment> findByDoctorIdAndDate(Long doctorId, LocalDate date);

    // Verificar si ya existe una cita en esa hora para ese médico
    boolean existsByDoctorIdAndDateAndStartTime(Long doctorId, LocalDate date, LocalTime startTime);

    // Panel paciente: listar citas propias
    List<Appointment> findByPatientId(Long patientId);
}