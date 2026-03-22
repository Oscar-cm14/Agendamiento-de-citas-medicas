package com.clinica.appointments.infrastructure.controllers;

import com.clinica.appointments.application.services.AppointmentService;
import com.clinica.shared.dto.ManualAppointmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for managing medical appointments.
 */
@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Constructor injection.
     *
     * @param appointmentService The appointment service.
     */
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Endpoint for manual appointment scheduling.
     * Restricted to ADMIN and SCHEDULER in SecurityConfig.
     *
     * @param request Data required for the appointment.
     * @return 201 Created status.
     */
    @PostMapping("/manual")
    public ResponseEntity<Void> scheduleManualAppointment(@Valid @RequestBody ManualAppointmentRequest request) {
        appointmentService.scheduleManualAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
