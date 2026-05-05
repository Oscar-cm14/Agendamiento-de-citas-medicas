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
import com.clinica.doctors.domain.entities.DoctorSchedule;
import com.clinica.doctors.infrastructure.repositories.DoctorScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de AppointmentService.
 * Maneja la lógica de negocio de RF1, RF2 y RF3.
 */
@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            DoctorScheduleRepository doctorScheduleRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
    }

    /**
     * RF1: Lista todas las citas de un médico en una fecha determinada.
     */
    @Override
    public List<AppointmentResponse> listAppointmentsByDoctorAndDate(Long doctorId, LocalDate date) {
        return appointmentRepository.findByDoctorIdAndDate(doctorId, date)
                .stream()
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

        // Verificar que la franja no esté ya ocupada
        boolean ocupado = appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                request.doctorId(), request.date(), request.startTime());

        if (ocupado) {
            throw new RuntimeException("Ya existe una cita en esa franja horaria");
        }

        // ── MEJORA RF2/RF4 ────────────────────────────────────────────────────
        // En lugar de usar 30 minutos fijo, se consulta el intervalo real
        // configurado por el administrador para este médico (RF4).
        // Si el médico no tiene horario configurado, se usan 30 min como fallback.
        int intervalMinutes = doctorScheduleRepository.findByDoctorId(request.doctorId())
                .map(DoctorSchedule::getIntervalMinutes)
                .filter(i -> i != null && i > 0)
                .orElse(30);
        LocalTime endTime = request.startTime().plusMinutes(intervalMinutes);

        // Construir y guardar la cita
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
     * Usa el horario configurado del médico; si no tiene, usa valores por defecto
     * (lunes a viernes, 8:00-17:00, intervalos de 30 minutos).
     */
    @Override
    public List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {

        List<AvailableSlotResponse> slots = new ArrayList<>();
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();

        Optional<DoctorSchedule> scheduleOpt = doctorScheduleRepository.findByDoctorId(doctorId);

        LocalTime inicio;
        LocalTime fin;
        int intervalo;

        if (scheduleOpt.isPresent()) {
            DoctorSchedule schedule = scheduleOpt.get();
            // Si el día no está en los días laborables del médico, no hay franjas
            if (schedule.getWorkingDays() == null
                    || !schedule.getWorkingDays().contains(dayOfWeek)) {
                return slots;
            }
            inicio = schedule.getStartTime();
            fin = schedule.getEndTime();
            intervalo = schedule.getIntervalMinutes();
        } else {
            // Valores por defecto cuando el médico no tiene horario configurado
            if (dayOfWeek == java.time.DayOfWeek.SATURDAY
                    || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                return slots; // No hay atención los fines de semana
            }
            inicio = LocalTime.of(8, 0);
            fin = LocalTime.of(17, 0);
            intervalo = 30;
        }

        // Generar todas las franjas y marcar cuáles están disponibles
        LocalTime current = inicio;
        while (current.isBefore(fin)) {
            LocalTime next = current.plusMinutes(intervalo);
            if (next.isAfter(fin))
                break;

            boolean ocupado = appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                    doctorId, date, current);

            slots.add(new AvailableSlotResponse(current, next, !ocupado));
            current = next;
        }

        return slots;
    }

    /**
     * Panel paciente: lista todas las citas de un paciente por su ID.
     */
    @Override
    public List<AppointmentResponse> listAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Convierte una entidad Appointment en AppointmentResponse (DTO).
     */
    private AppointmentResponse toResponse(Appointment appointment) {

        // Buscar médico para obtener nombre y especialidad
        Doctor doctor = doctorRepository.findById(appointment.getDoctorId())
                .orElse(null);

        String doctorName = (doctor != null)
                ? doctor.getFirstName() + " " + doctor.getLastName()
                : "Desconocido";

        // extraer specialty del médico
        String specialty = (doctor != null) ? doctor.getSpecialty() : null;

        // Buscar paciente para obtener nombre
        String patientName = patientRepository.findById(appointment.getPatientId())
                .map(p -> p.getFirstName() + " " + p.getLastName())
                .orElse("Desconocido");

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getDoctorId(),
                doctorName,
                specialty,
                appointment.getPatientId(),
                patientName,
                appointment.getDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getNotes());
    }
}