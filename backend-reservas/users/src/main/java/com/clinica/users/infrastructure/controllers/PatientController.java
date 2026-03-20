package com.clinica.users.infrastructure.controllers;

import com.clinica.shared.dto.PatientRegistrationRequest;
import com.clinica.users.application.services.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing endpoints for Patient management.
 */
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    /**
     * Constructor injection for the PatientService.
     *
     * @param patientService The service handling patient business logic.
     */
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    /**
     * Endpoint to register a new Patient.
     * Restricted to ADMIN and SCHEDULER roles in SecurityConfig.
     *
     * @param request The registration payload.
     * @return 201 Created status if successful.
     */
    @PostMapping
    public ResponseEntity<Void> registerPatient(@Valid @RequestBody PatientRegistrationRequest request) {
        patientService.registerPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
