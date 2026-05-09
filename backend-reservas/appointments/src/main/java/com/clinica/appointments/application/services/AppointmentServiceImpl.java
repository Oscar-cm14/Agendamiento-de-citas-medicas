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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
}

