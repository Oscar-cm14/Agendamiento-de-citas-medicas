package com.clinica.appointments.application.services;

import com.clinica.appointments.domain.entities.Appointment;
import com.clinica.appointments.infrastructure.repositories.AppointmentRepository;
import com.clinica.shared.domain.entities.AppointmentStatus;
import com.clinica.shared.dto.AppointmentRequest;
import com.clinica.shared.dto.AppointmentResponse;
import com.clinica.shared.dto.AvailableSlotResponse;
import com.clinica.shared.dto.CancelRequest;
import com.clinica.doctors.domain.entities.Doctor;
import com.clinica.doctors.infrastructure.repositories.DoctorRepository;
import com.clinica.users.infrastructure.repositories.PatientRepository;
import com.clinica.doctors.domain.entities.DoctorSchedule;
import com.clinica.doctors.infrastructure.repositories.DoctorScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.clinica.appointments.domain.entities.AppointmentHistory;
import com.clinica.appointments.infrastructure.repositories.AppointmentHistoryRepository;
import com.clinica.shared.dto.RescheduleAppointmentRequest;

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
    private final AppointmentHistoryRepository appointmentHistoryRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            DoctorScheduleRepository doctorScheduleRepository,
            AppointmentHistoryRepository appointmentHistoryRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.appointmentHistoryRepository = appointmentHistoryRepository;
    }

    @Override
    public List<AppointmentResponse> listAppointmentsByDoctorAndDate(Long doctorId, LocalDate date) {
        return appointmentRepository.findByDoctorIdAndDate(doctorId, date)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));
        patientRepository.findById(request.patientId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        boolean ocupado = appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                request.doctorId(), request.date(), request.startTime());
        if (ocupado) throw new RuntimeException("Ya existe una cita en esa franja horaria");

        // ── REGLAS DE NEGOCIO: VALIDACIÓN DE HISTORIAL DEL PACIENTE ──
        List<Appointment> patientAppts = appointmentRepository.findByPatientId(request.patientId());
        boolean hasScheduled = false;
        boolean hasCompletedGeneral = false;

        for (Appointment app : patientAppts) {
            if (app.getStatus() == AppointmentStatus.SCHEDULED) {
                hasScheduled = true;
            }
            if (app.getStatus() == AppointmentStatus.COMPLETED) {
                Doctor doc = doctorRepository.findById(app.getDoctorId()).orElse(null);
                if (doc != null && doc.getSpecialty() != null && doc.getSpecialty().equalsIgnoreCase("Consulta General")) {
                    hasCompletedGeneral = true;
                }
            }
        }

        // Regla 1: No puede tener citas agendadas/pendientes simultáneas
        if (hasScheduled) {
            throw new RuntimeException("No puede agendar nuevas citas mientras tenga una cita pendiente o agendada");
        }

        // Regla 3: Si no es Consulta General, debe tener una completada antes
        Doctor targetDoctor = doctorRepository.findById(request.doctorId()).get();
        if (targetDoctor.getSpecialty() != null && !targetDoctor.getSpecialty().equalsIgnoreCase("Consulta General") && !hasCompletedGeneral) {
            throw new RuntimeException("Debe tener una Consulta General completada antes de agendar esta especialidad");
        }
        // ─────────────────────────────────────────────────────────────

        int intervalMinutes = doctorScheduleRepository.findByDoctorId(request.doctorId())
                .map(DoctorSchedule::getIntervalMinutes)
                .filter(i -> i != null && i > 0).orElse(30);

        Appointment appointment = new Appointment();
        appointment.setDoctorId(request.doctorId());
        appointment.setPatientId(request.patientId());
        appointment.setDate(request.date());
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.startTime().plusMinutes(intervalMinutes));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setNotes(request.notes());
        return toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse rescheduleAppointment(Long appointmentId, AppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        boolean ocupado = appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                request.doctorId(), request.date(), request.startTime());
        if (ocupado) throw new RuntimeException("La franja seleccionada ya está ocupada");

        int intervalMinutes = doctorScheduleRepository.findByDoctorId(request.doctorId())
                .map(DoctorSchedule::getIntervalMinutes)
                .filter(i -> i != null && i > 0).orElse(30);

        appointment.setDate(request.date());
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.startTime().plusMinutes(intervalMinutes));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        if (request.notes() != null) appointment.setNotes(request.notes());
        return toResponse(appointmentRepository.save(appointment));
    }

    // ── NUEVO: Cancelar cita con motivo ──────────────────────────────────────
    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId, CancelRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new RuntimeException("La cita ya está cancelada");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new RuntimeException("No se puede cancelar una cita ya completada");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new RuntimeException("Debe indicar el motivo de cancelación");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request.reason());
        return toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        List<AvailableSlotResponse> slots = new ArrayList<>();
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        Optional<DoctorSchedule> scheduleOpt = doctorScheduleRepository.findByDoctorId(doctorId);

        LocalTime inicio; LocalTime fin; int intervalo;

        if (scheduleOpt.isPresent()) {
            DoctorSchedule schedule = scheduleOpt.get();
            if (schedule.getWorkingDays() == null
                    || !schedule.getWorkingDays().contains(dayOfWeek)) return slots;
            inicio    = schedule.getStartTime()       != null ? schedule.getStartTime()       : LocalTime.of(8, 0);
            fin       = schedule.getEndTime()          != null ? schedule.getEndTime()          : LocalTime.of(17, 0);
            intervalo = (schedule.getIntervalMinutes() != null && schedule.getIntervalMinutes() > 0)
                        ? schedule.getIntervalMinutes() : 30;
        } else {
            if (dayOfWeek == java.time.DayOfWeek.SATURDAY
                    || dayOfWeek == java.time.DayOfWeek.SUNDAY) return slots;
            inicio = LocalTime.of(8, 0); fin = LocalTime.of(17, 0); intervalo = 30;
        }

        LocalTime current = inicio;
        while (current.isBefore(fin)) {
            LocalTime next = current.plusMinutes(intervalo);
            if (next.isAfter(fin)) break;
            boolean ocupado = appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                    doctorId, date, current);
            slots.add(new AvailableSlotResponse(current, next, !ocupado));
            current = next;
        }
        return slots;
    }

    @Override
    public List<AppointmentResponse> listAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId)
                .stream().map(this::toResponse).toList();
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        Doctor doctor = doctorRepository.findById(appointment.getDoctorId()).orElse(null);
        String doctorName = (doctor != null)
                ? doctor.getFirstName() + " " + doctor.getLastName() : "Desconocido";
        String specialty = (doctor != null) ? doctor.getSpecialty() : null;
        String patientName = patientRepository.findById(appointment.getPatientId())
                .map(p -> p.getFirstName() + " " + p.getLastName()).orElse("Desconocido");

        return new AppointmentResponse(
                appointment.getId(), appointment.getDoctorId(), doctorName, specialty,
                appointment.getPatientId(), patientName, appointment.getDate(),
                appointment.getStartTime(), appointment.getEndTime(), appointment.getStatus(),
                appointment.getNotes(), appointment.getCancellationReason());
    }

    /**
     * Búsqueda dinámica de reservas aplicando el patrón de diseño Builder
     * mediante la clase AppointmentSpecificationBuilder.
     */
    @Override
    public List<AppointmentResponse> searchAppointments(Long doctorId, Long patientId, LocalDate exactDate,
            AppointmentStatus status) {
        // Uso del Patrón Builder para construir de forma programática las consultas
        org.springframework.data.jpa.domain.Specification<Appointment> spec = new com.clinica.appointments.infrastructure.specifications.AppointmentSpecificationBuilder()
                .withDoctorId(doctorId)
                .withPatientId(patientId)
                .withExactDate(exactDate)
                .withStatus(status)
                .build();

        return appointmentRepository.findAll(spec)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * RF6: Re-agendamiento de citas existentes para evitar que el paciente que requiere seguimiento
     * haga una cita nueva. Se conserva el historial de cambios.
     */
    @Override
    @Transactional
    public AppointmentResponse rescheduleAppointment(Long appointmentId, RescheduleAppointmentRequest request) {
        // Buscar la cita original
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        // Validar que la nueva franja no esté ocupada por otra cita
        // (excluyendo la cita actual en caso de que mantenga la hora y solo cambie fecha, etc.)
        boolean ocupado = appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                appointment.getDoctorId(), request.newDate(), request.newStartTime());

        if (ocupado && !(appointment.getDate().equals(request.newDate()) && appointment.getStartTime().equals(request.newStartTime()))) {
            throw new RuntimeException("Ya existe una cita en esa franja horaria para el médico");
        }

        // Calcular nuevo endTime usando el intervalo del médico
        int intervalMinutes = doctorScheduleRepository.findByDoctorId(appointment.getDoctorId())
                .map(DoctorSchedule::getIntervalMinutes)
                .filter(i -> i != null && i > 0)
                .orElse(30);
        LocalTime newEndTime = request.newStartTime().plusMinutes(intervalMinutes);

        // Extraer usuario responsable del SecurityContext
        String changedBy = "system"; // fallback
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            changedBy = authentication.getName();
        }

        // Crear registro en el historial
        AppointmentHistory history = new AppointmentHistory();
        history.setAppointment(appointment);
        history.setPreviousDate(appointment.getDate());
        history.setPreviousStartTime(appointment.getStartTime());
        history.setPreviousEndTime(appointment.getEndTime());
        history.setNewDate(request.newDate());
        history.setNewStartTime(request.newStartTime());
        history.setNewEndTime(newEndTime);
        history.setChangedAt(LocalDateTime.now());
        history.setChangedBy(changedBy);
        history.setReason(request.reason());

        appointmentHistoryRepository.save(history);

        // Actualizar la cita principal
        appointment.setDate(request.newDate());
        appointment.setStartTime(request.newStartTime());
        appointment.setEndTime(newEndTime);
        // Podríamos actualizar el estado si fuera necesario, por ahora lo mantenemos o forzamos a SCHEDULED
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        Appointment saved = appointmentRepository.save(appointment);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse updateAppointmentStatus(Long appointmentId, com.clinica.shared.dto.UpdateAppointmentStatusRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        
        appointment.setStatus(request.status());
        return toResponse(appointmentRepository.save(appointment));
    }
}