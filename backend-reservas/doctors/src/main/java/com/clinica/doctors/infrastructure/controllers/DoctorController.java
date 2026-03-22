package com.clinica.doctors.infrastructure.controllers;

import com.clinica.doctors.application.services.DoctorService;
import com.clinica.shared.dto.DoctorRegistrationRequest;
import com.clinica.shared.dto.DoctorResponse;
import com.clinica.shared.dto.DoctorScheduleRequest;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing endpoints for Doctor management.
 */
@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    /**
     * Constructor injection for the DoctorService.
     *
     * @param doctorService The service handling doctor business logic.
     */
    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /**
     * Endpoint to register a new Doctor.
     *
     * @param request The registration payload, validated by Jakarta Validation.
     * @return A ResponseEntity containing the created DoctorResponse and HTTP 201 status.
     */
    @PostMapping
    public ResponseEntity<DoctorResponse> registerDoctor(@Valid @RequestBody DoctorRegistrationRequest request) {
        DoctorResponse response = doctorService.registerDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to add availability schedules to a specific doctor.
     * Restricted to ADMIN role in SecurityConfig.
     *
     * @param doctorId  The ID of the doctor.
     * @param schedules List of schedules to add.
     * @return 200 OK if successful.
     */
    @PostMapping("/{doctorId}/schedules")
    public ResponseEntity<Void> addSchedules(@PathVariable Long doctorId, @Valid @RequestBody List<DoctorScheduleRequest> schedules) {
        doctorService.addSchedulesToDoctor(doctorId, schedules);
        return ResponseEntity.ok().build();
    }
}
