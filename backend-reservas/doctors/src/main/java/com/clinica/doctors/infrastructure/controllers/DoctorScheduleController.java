package com.clinica.doctors.infrastructure.controllers;


import com.clinica.doctors.application.services.DoctorScheduleService;
import com.clinica.shared.dto.DoctorScheduleRequest;
import com.clinica.shared.dto.DoctorScheduleResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for managing doctor schedules.
 * Handles RF4: admin configures doctor availability.
 */
@RestController
@RequestMapping("/api/v1/doctors/schedules")
public class DoctorScheduleController {

    private final DoctorScheduleService scheduleService;

    public DoctorScheduleController(DoctorScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * RF4: Configura o actualiza el horario de un médico.
     * PUT /api/v1/doctors/schedules
     */
    @PutMapping
    public ResponseEntity<DoctorScheduleResponse> saveSchedule(
            @Valid @RequestBody DoctorScheduleRequest request) {

        return ResponseEntity.ok(scheduleService.saveSchedule(request));
    }

    /**
     * RF4: Obtiene el horario configurado de un médico.
     * GET /api/v1/doctors/schedules/{doctorId}
     */
    @GetMapping("/{doctorId}")
    public ResponseEntity<DoctorScheduleResponse> getSchedule(
            @PathVariable Long doctorId) {

        return ResponseEntity.ok(scheduleService.getScheduleByDoctorId(doctorId));
    }
}
