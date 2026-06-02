package com.clinica.appointments.infrastructure.controllers;

import com.clinica.appointments.application.services.AppointmentService;
import com.clinica.shared.dto.AppointmentRequest;
import com.clinica.shared.dto.AppointmentResponse;
import com.clinica.shared.dto.AvailableSlotResponse;
import com.clinica.shared.dto.CancelRequest;
import com.clinica.shared.dto.PriorityUpdateRequest;
import com.clinica.shared.dto.RescheduleAppointmentRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // ── Listar citas por médico y fecha ──────────────────────────────────────
    @GetMapping(params = {"doctorId", "date"})
    public ResponseEntity<List<AppointmentResponse>> listByDoctorAndDate(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.listAppointmentsByDoctorAndDate(doctorId, date));
    }

    // ── Listar citas por paciente ─────────────────────────────────────────────
    @GetMapping(params = "patientId")
    public ResponseEntity<List<AppointmentResponse>> listByPatient(@RequestParam Long patientId) {
        return ResponseEntity.ok(appointmentService.listAppointmentsByPatient(patientId));
    }

    // ── Crear cita ────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(request));
    }

    // ── Reagendar (legacy) ────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse> reschedule(
            @PathVariable Long id, @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(id, request));
    }

    // ── Reagendar (RF6) ───────────────────────────────────────────────────────
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(id, request));
    }

    // ── Cancelar cita ─────────────────────────────────────────────────────────
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable Long id, @RequestBody CancelRequest request) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, request));
    }

    // ── Actualizar estado ─────────────────────────────────────────────────────
    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody com.clinica.shared.dto.UpdateAppointmentStatusRequest request) {
        return ResponseEntity.ok(appointmentService.updateAppointmentStatus(id, request));
    }

    // ── Franjas disponibles ───────────────────────────────────────────────────
    @GetMapping("/slots")
    public ResponseEntity<List<AvailableSlotResponse>> slots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getAvailableSlots(doctorId, date));
    }

    // ── NUEVO: Marcar cita como atendida/completada ───────────────────────────
    @PatchMapping("/{id}/complete")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.completeAppointment(id));
    }

    // ── NUEVO: Actualizar nivel de prioridad ──────────────────────────────────
    @PatchMapping("/{id}/priority")
    public ResponseEntity<AppointmentResponse> updatePriority(
            @PathVariable Long id,
            @RequestBody PriorityUpdateRequest request) {
        return ResponseEntity.ok(appointmentService.updatePriority(id, request));
    }

    // ── NUEVO: Listar citas prioritarias por rango de fechas ─────────────────
    @GetMapping("/priority")
    public ResponseEntity<List<AppointmentResponse>> listPriority(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Long doctorId) {
        return ResponseEntity.ok(appointmentService.listPriorityAppointments(doctorId, dateFrom, dateTo));
    }
}