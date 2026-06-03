package com.clinica.appointments.application.services;

import com.clinica.appointments.domain.entities.Appointment;
import com.clinica.appointments.infrastructure.repositories.AppointmentRepository;
import com.clinica.shared.domain.entities.AppointmentStatus;
import com.clinica.shared.dto.AppointmentRequest;
import com.clinica.shared.dto.AppointmentResponse;
import com.clinica.shared.dto.AvailableSlotResponse;
import com.clinica.shared.dto.CancelRequest;
import com.clinica.shared.dto.PriorityUpdateRequest;
import com.clinica.doctors.domain.entities.Doctor;
import com.clinica.doctors.infrastructure.repositories.DoctorRepository;
import com.clinica.users.domain.entities.Patient;
import com.clinica.users.infrastructure.repositories.PatientRepository;
import com.clinica.doctors.domain.entities.DoctorSchedule;
import com.clinica.doctors.infrastructure.repositories.DoctorScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.clinica.shared.domain.exceptions.BusinessRuleException;

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

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final AppointmentHistoryRepository appointmentHistoryRepository;
    private final NotificationService notificationService;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            DoctorScheduleRepository doctorScheduleRepository,
            AppointmentHistoryRepository appointmentHistoryRepository,
            NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.appointmentHistoryRepository = appointmentHistoryRepository;
        this.notificationService = notificationService;
    }

    // =========================================================================
    // RF1 - LISTAR CITAS
    // =========================================================================

    @Override
    public List<AppointmentResponse> listAppointmentsByDoctorAndDate(Long doctorId, LocalDate date) {
        return appointmentRepository.findByDoctorIdAndDate(doctorId, date)
                .stream().map(this::toResponse).toList();
    }

    // =========================================================================
    // RF2/RF3 - CREAR CITA
    // =========================================================================

    @Override
    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        boolean ocupado = appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                request.doctorId(), request.date(), request.startTime());
        if (ocupado) throw new BusinessRuleException("Ya existe una cita en esa franja horaria");

        // ── REGLAS DE NEGOCIO ────────────────────────────────────
        List<Appointment> patientAppts = appointmentRepository.findByPatientId(request.patientId());
        boolean hasScheduled = false;
        boolean hasCompletedGeneral = false;

        for (Appointment app : patientAppts) {
            if (app.getStatus() == AppointmentStatus.SCHEDULED) {
                hasScheduled = true;
            }
            if (app.getStatus() == AppointmentStatus.COMPLETED) {
                Doctor doc = doctorRepository.findById(app.getDoctorId()).orElse(null);
                if (doc != null && doc.getSpecialty() != null
                        && doc.getSpecialty().equalsIgnoreCase("Consulta General")) {
                    hasCompletedGeneral = true;
                }
            }
        }

        if (hasScheduled) {
            throw new BusinessRuleException(
                    "No puede agendar nuevas citas mientras tenga una cita pendiente o agendada");
        }

        Doctor targetDoctor = doctorRepository.findById(request.doctorId()).get();
        if (targetDoctor.getSpecialty() != null
                && !targetDoctor.getSpecialty().equalsIgnoreCase("Consulta General")
                && !hasCompletedGeneral) {
            throw new BusinessRuleException(
                    "Debe tener una Consulta General completada antes de agendar esta especialidad");
        }

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

        // ── CAMPOS DE PRIORIDAD ───────────────────────────────────
        if (request.priority() != null) {
            appointment.setPriority(request.priority());
        }
        if (request.priorityReason() != null) {
            appointment.setPriorityReason(request.priorityReason());
        }
        if (request.urgencyLevel() != null) {
            appointment.setUrgencyLevel(request.urgencyLevel());
        }
        // ─────────────────────────────────────────────────────────

        AppointmentResponse response = toResponse(appointmentRepository.save(appointment));

        // ── NOTIFICACIÓN (no bloquea si falla) ──────────────────
        try {
            notificationService.notificarCitaCreada(
                    response,
                    patient.getEmail(),
                    patient.getPhone()
            );
        } catch (Exception ignored) {}

        return response;
    }

    // =========================================================================
    // CANCELAR CITA
    // =========================================================================

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId, CancelRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessRuleException("La cita ya está cancelada");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("No se puede cancelar una cita ya completada");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new BusinessRuleException("Debe indicar el motivo de cancelación");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request.reason());

        AppointmentResponse response = toResponse(appointmentRepository.save(appointment));

        try {
            Patient patient = patientRepository.findById(appointment.getPatientId()).orElse(null);
            if (patient != null) {
                notificationService.notificarCitaCancelada(response, patient.getEmail(), patient.getPhone());
            }
        } catch (Exception ignored) {}

        return response;
    }

    // =========================================================================
    // REAGENDAR CITA (RF6)
    // =========================================================================

    @Override
    @Transactional
    public AppointmentResponse rescheduleAppointment(Long appointmentId, RescheduleAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        boolean ocupado = appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                appointment.getDoctorId(), request.newDate(), request.newStartTime());

        if (ocupado && !(appointment.getDate().equals(request.newDate())
                && appointment.getStartTime().equals(request.newStartTime()))) {
            throw new RuntimeException("Ya existe una cita en esa franja horaria para el médico");
        }

        int intervalMinutes = doctorScheduleRepository.findByDoctorId(appointment.getDoctorId())
                .map(DoctorSchedule::getIntervalMinutes)
                .filter(i -> i != null && i > 0)
                .orElse(30);
        LocalTime newEndTime = request.newStartTime().plusMinutes(intervalMinutes);

        String changedBy = "system";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {
            changedBy = authentication.getName();
        }

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

        appointment.setDate(request.newDate());
        appointment.setStartTime(request.newStartTime());
        appointment.setEndTime(newEndTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        AppointmentResponse response = toResponse(appointmentRepository.save(appointment));

        try {
            Patient patient = patientRepository.findById(appointment.getPatientId()).orElse(null);
            if (patient != null) {
                notificationService.notificarCitaReagendada(response, patient.getEmail(), patient.getPhone());
            }
        } catch (Exception ignored) {}

        return response;
    }

    @Override
    @Transactional
    public AppointmentResponse rescheduleAppointment(Long appointmentId, AppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        boolean ocupado = appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                request.doctorId(), request.date(), request.startTime());
        if (ocupado) throw new BusinessRuleException("La franja seleccionada ya está ocupada");

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

    // =========================================================================
    // FRANJAS DISPONIBLES
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        List<AvailableSlotResponse> slots = new ArrayList<>();
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        Optional<DoctorSchedule> scheduleOpt = doctorScheduleRepository.findByDoctorId(doctorId);
        if (scheduleOpt.isEmpty()) return slots;
        DoctorSchedule schedule = scheduleOpt.get();
        if (schedule.getWorkingDays() == null || !schedule.getWorkingDays().contains(dayOfWeek)) return slots;
        LocalTime inicio = schedule.getStartTime();
        LocalTime fin    = schedule.getEndTime();
        int intervalo = (schedule.getIntervalMinutes() != null && schedule.getIntervalMinutes() > 0)
                ? schedule.getIntervalMinutes() : 30;
        if (inicio == null || fin == null) return slots;
        LocalTime current = inicio;
        while (current.isBefore(fin)) {
            LocalTime next = current.plusMinutes(intervalo);
            if (next.isAfter(fin)) break;
            boolean ocupado = appointmentRepository.existsByDoctorIdAndDateAndStartTime(doctorId, date, current);
            slots.add(new AvailableSlotResponse(current, next, !ocupado));
            current = next;
        }
        return slots;
    }

    // =========================================================================
    // LISTAR POR PACIENTE
    // =========================================================================

    @Override
    public List<AppointmentResponse> listAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId).stream().map(this::toResponse).toList();
    }

    // =========================================================================
    // BÚSQUEDA DINÁMICA
    // =========================================================================

    @Override
    public List<AppointmentResponse> searchAppointments(Long doctorId, Long patientId, LocalDate exactDate,
            AppointmentStatus status) {
        org.springframework.data.jpa.domain.Specification<Appointment> spec =
                new com.clinica.appointments.infrastructure.specifications.AppointmentSpecificationBuilder()
                .withDoctorId(doctorId)
                .withPatientId(patientId)
                .withExactDate(exactDate)
                .withStatus(status)
                .build();
        return appointmentRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    // =========================================================================
    // ACTUALIZAR ESTADO
    // =========================================================================

    @Override
    @Transactional
    public AppointmentResponse updateAppointmentStatus(Long appointmentId,
            com.clinica.shared.dto.UpdateAppointmentStatusRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        appointment.setStatus(request.status());
        return toResponse(appointmentRepository.save(appointment));
    }

    // =========================================================================
    // NUEVO: LISTAR CITAS PRIORITARIAS
    // =========================================================================

    @Override
    public List<AppointmentResponse> listPriorityAppointments(Long doctorId, LocalDate dateFrom, LocalDate dateTo) {
        List<Appointment> appointments;
        if (doctorId != null && doctorId > 0) {
            appointments = appointmentRepository
                    .findByPriorityTrueAndDoctorIdAndDateBetween(doctorId, dateFrom, dateTo);
        } else {
            appointments = appointmentRepository
                    .findByPriorityTrueAndDateBetween(dateFrom, dateTo);
        }
        return appointments.stream()
                .sorted((a, b) -> {
                    int oa = urgencyOrder(a.getUrgencyLevel());
                    int ob = urgencyOrder(b.getUrgencyLevel());
                    if (oa != ob) return oa - ob;
                    return a.getDate().compareTo(b.getDate());
                })
                .map(this::toResponse)
                .toList();
    }

    // =========================================================================
    // NUEVO: MARCAR CITA COMO COMPLETADA
    // =========================================================================

    @Override
    @Transactional
    public AppointmentResponse completeAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessRuleException("No se puede completar una cita cancelada");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("La cita ya fue marcada como atendida");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return toResponse(appointmentRepository.save(appointment));
    }

    // =========================================================================
    // NUEVO: ACTUALIZAR PRIORIDAD
    // =========================================================================

    @Override
    @Transactional
    public AppointmentResponse updatePriority(Long appointmentId, PriorityUpdateRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        if (request.priority() != null) {
            appointment.setPriority(request.priority());
        }
        if (request.urgencyLevel() != null) {
            appointment.setUrgencyLevel(request.urgencyLevel());
        }
        // Permitir string vacío para limpiar urgencyLevel
        if (request.urgencyLevel() != null && request.urgencyLevel().isBlank()) {
            appointment.setUrgencyLevel(null);
        }
        if (request.priorityReason() != null) {
            appointment.setPriorityReason(request.priorityReason());
        }

        return toResponse(appointmentRepository.save(appointment));
    }

    // =========================================================================
    // UTILIDADES PRIVADAS
    // =========================================================================

    private AppointmentResponse toResponse(Appointment appointment) {
        Doctor doctor = doctorRepository.findById(appointment.getDoctorId()).orElse(null);
        String doctorName  = (doctor != null) ? doctor.getFirstName() + " " + doctor.getLastName() : "Desconocido";
        String specialty   = (doctor != null) ? doctor.getSpecialty() : null;
        String patientName = patientRepository.findById(appointment.getPatientId())
                .map(p -> p.getFirstName() + " " + p.getLastName()).orElse("Desconocido");

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
                appointment.getNotes(),
                appointment.getCancellationReason(),
                // ── PRIORIDAD ──────────────────────────────────────
                appointment.isPriority(),
                appointment.getUrgencyLevel(),
                appointment.getPriorityReason()
        );
    }

    private int urgencyOrder(String level) {
        if ("HIGH".equals(level))   return 0;
        if ("MEDIUM".equals(level)) return 1;
        if ("LOW".equals(level))    return 2;
        return 3;
    }
}