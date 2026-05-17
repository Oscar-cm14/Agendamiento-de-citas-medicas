package com.clinica.appointments.infrastructure.controllers;

import com.clinica.appointments.application.services.AppointmentService;
import com.clinica.shared.dto.AppointmentRequest;
import com.clinica.shared.dto.AppointmentResponse;
import com.clinica.shared.dto.AvailableSlotResponse;
import com.clinica.shared.dto.CancelRequest;
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

    @GetMapping(params = {"doctorId", "date"})
    public ResponseEntity<List<AppointmentResponse>> listByDoctorAndDate(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.listAppointmentsByDoctorAndDate(doctorId, date));
    }

    @GetMapping(params = "patientId")
    public ResponseEntity<List<AppointmentResponse>> listByPatient(@RequestParam Long patientId) {
        return ResponseEntity.ok(appointmentService.listAppointmentsByPatient(patientId));
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse> reschedule(
            @PathVariable Long id, @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(id, request));
    }

    // ── NUEVO: Cancelar cita con motivo ──────────────────────────────────────
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable Long id, @RequestBody CancelRequest request) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, request));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<AvailableSlotResponse>> slots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getAvailableSlots(doctorId, date));
    }

    /**
     * RF6: Re-agendamiento de citas existentes.
     * PUT /api/v1/appointments/{id}/reschedule
     */
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleAppointmentRequest request) {

        AppointmentResponse response = appointmentService.rescheduleAppointment(id, request);
        return ResponseEntity.ok(response);
    }
}