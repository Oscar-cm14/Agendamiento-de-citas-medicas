package com.clinica.appointments.application.services;

import com.clinica.shared.dto.AppointmentRequest;
import com.clinica.shared.dto.AppointmentResponse;
import com.clinica.shared.dto.AvailableSlotResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing appointments.
 */
public interface AppointmentService {

    // RF1: Listar citas de un médico en una fecha
    List<AppointmentResponse> listAppointmentsByDoctorAndDate(Long doctorId, LocalDate date);

    // RF2 y RF3: Crear una nueva cita
    AppointmentResponse createAppointment(AppointmentRequest request);

    // RF3: Obtener franjas disponibles de un médico en una fecha
    List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date);
    
   // Panel paciente: listar sus propias citas
    List<AppointmentResponse> listAppointmentsByPatient(Long patientId);
    
    // RF: Reagendar una cita existente
    AppointmentResponse rescheduleAppointment(Long appointmentId, AppointmentRequest request);
}
