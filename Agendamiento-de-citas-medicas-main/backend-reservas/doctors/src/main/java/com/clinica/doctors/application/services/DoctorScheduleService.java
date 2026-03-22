package com.clinica.doctors.application.services;



import com.clinica.shared.dto.DoctorScheduleRequest;
import com.clinica.shared.dto.DoctorScheduleResponse;

/**
 * Service interface for managing doctor schedules.
 * Used in RF4: admin configures doctor availability.
 */
public interface DoctorScheduleService {

    // RF4: Configurar o actualizar horario de un médico
    DoctorScheduleResponse saveSchedule(DoctorScheduleRequest request);

    // RF4: Obtener horario de un médico
    DoctorScheduleResponse getScheduleByDoctor(Long doctorId);
}
