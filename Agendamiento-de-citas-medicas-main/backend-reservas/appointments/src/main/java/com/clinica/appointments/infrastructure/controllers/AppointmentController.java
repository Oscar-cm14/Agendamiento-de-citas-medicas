package com.clinica.appointments.infrastructure.controllers;

import com.clinica.appointments.application.services.AppointmentService;
import com.clinica.shared.dto.AppointmentRequest;
import com.clinica.shared.dto.AppointmentResponse;
import com.clinica.shared.dto.AvailableSlotResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for managing appointments.
 * Handles RF1, RF2 and RF3.
 */
@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * RF1: Lista las citas de un médico en una fecha determinada.
     * También soporta listar las citas de un paciente (panel paciente).
     * GET /api/v1/appointments?doctorId=1&date=2026-03-21
     * GET /api/v1/appointments?patientId=5
     */
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> listAppointments(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long patientId) {

        // Panel paciente: listar sus propias citas
        if (patientId != null) {
            return ResponseEntity.ok(appointmentService.listAppointmentsByPatient(patientId));
        }

        // Panel agendador / médico: filtrar por doctor y fecha
        List<AppointmentResponse> appointments =
                appointmentService.listAppointmentsByDoctorAndDate(doctorId, date);

        return ResponseEntity.ok(appointments);
    }

    /**
     * RF2 y RF3: Crea una nueva cita.
     * POST /api/v1/appointments
     */
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentRequest request) {

        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * RF3: Retorna las franjas horarias disponibles de un médico en una fecha.
     * GET /api/v1/appointments/slots?doctorId=1&date=2026-03-21
     */
    @GetMapping("/slots")
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<AvailableSlotResponse> slots =
                appointmentService.getAvailableSlots(doctorId, date);

        return ResponseEntity.ok(slots);
    }
}