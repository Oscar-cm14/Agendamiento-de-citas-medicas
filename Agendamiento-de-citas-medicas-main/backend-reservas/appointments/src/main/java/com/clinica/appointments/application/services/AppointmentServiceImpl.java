package com.clinica.appointments.application.services;

import com.clinica.appointments.domain.entities.Appointment;
import com.clinica.appointments.infrastructure.repositories.AppointmentRepository;
import com.clinica.shared.domain.entities.AppointmentStatus;                        
import com.clinica.shared.dto.AppointmentRequest;
import com.clinica.shared.dto.AppointmentResponse;
import com.clinica.shared.dto.AvailableSlotResponse;
import com.clinica.doctors.domain.entities.Doctor;                         
import com.clinica.doctors.infrastructure.repositories.DoctorRepository;  
import com.clinica.users.domain.entities.Patient;
import com.clinica.users.infrastructure.repositories.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of AppointmentService.
 * Handles RF1, RF2 and RF3 business logic.
 */
@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  DoctorRepository doctorRepository,
                                  PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * RF1: Lista todas las citas de un médico en una fecha determinada.
     */
    @Override
    public List<AppointmentResponse> listAppointmentsByDoctorAndDate(Long doctorId, LocalDate date) {

        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndDate(doctorId, date);

        return appointments.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * RF2 y RF3: Crea una nueva cita validando disponibilidad.
     */
    @Override
    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {

        // Verificar que el médico existe
        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));

        // Verificar que el paciente existe
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        // Verificar que no exista ya una cita en esa hora
        boolean ocupado = appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                request.doctorId(), request.date(), request.startTime());

        if (ocupado) {
            throw new RuntimeException("Ya existe una cita en esa franja horaria");
        }

        // Calcular hora de fin (intervalo fijo de 30 minutos por defecto)
        LocalTime endTime = request.startTime().plusMinutes(30);

        // Crear la cita
        Appointment appointment = new Appointment();
        appointment.setDoctorId(request.doctorId());
        appointment.setPatientId(request.patientId());
        appointment.setDate(request.date());
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setNotes(request.notes());

        Appointment saved = appointmentRepository.save(appointment);

        return toResponse(saved);
    }

    /**
     * RF3: Retorna las franjas horarias disponibles de un médico en una fecha.
     * Genera franjas de 8:00 a 17:00 cada 30 minutos.
     */
    @Override
    public List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {

        List<AvailableSlotResponse> slots = new ArrayList<>();

        LocalTime inicio = LocalTime.of(8, 0);
        LocalTime fin = LocalTime.of(17, 0);
        int intervalo = 30;

        LocalTime current = inicio;
        while (current.isBefore(fin)) {
            LocalTime next = current.plusMinutes(intervalo);
            boolean ocupado = appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                    doctorId, date, current);
            slots.add(new AvailableSlotResponse(current, next, !ocupado));
            current = next;
        }

        return slots;
    }

    /**
     * Convierte una entidad Appointment a AppointmentResponse.
     */
    private AppointmentResponse toResponse(Appointment appointment) {

        String doctorName = doctorRepository.findById(appointment.getDoctorId())
                .map(d -> d.getFirstName() + " " + d.getLastName())
                .orElse("Desconocido");

        String patientName = patientRepository.findById(appointment.getPatientId())
                .map(p -> p.getFirstName() + " " + p.getLastName())
                .orElse("Desconocido");

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getDoctorId(),
                doctorName,
                appointment.getPatientId(),
                patientName,
                appointment.getDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getNotes()
        );
    }
}