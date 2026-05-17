package com.clinica.appointments.application.services;

import com.clinica.shared.dto.AppointmentRequest;
import com.clinica.shared.dto.AppointmentResponse;
import com.clinica.shared.dto.AvailableSlotResponse;
import com.clinica.shared.dto.CancelRequest;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    List<AppointmentResponse> listAppointmentsByDoctorAndDate(Long doctorId, LocalDate date);

    AppointmentResponse createAppointment(AppointmentRequest request);

    List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date);

    List<AppointmentResponse> listAppointmentsByPatient(Long patientId);

    // Búsqueda dinámica con Patrón Builder (Specification)
    List<AppointmentResponse> searchAppointments(Long doctorId, Long patientId, LocalDate exactDate, com.clinica.shared.domain.entities.AppointmentStatus status);
    
    // RF6: Re-agendamiento de cita
    AppointmentResponse rescheduleAppointment(Long appointmentId, com.clinica.shared.dto.RescheduleAppointmentRequest request);
    
    AppointmentResponse rescheduleAppointment(Long appointmentId, AppointmentRequest request);
    
    AppointmentResponse cancelAppointment(Long appointmentId, CancelRequest request);
}
