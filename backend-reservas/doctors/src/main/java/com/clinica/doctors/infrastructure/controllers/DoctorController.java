package com.clinica.doctors.infrastructure.controllers;



import com.clinica.doctors.application.services.DoctorService;
import com.clinica.doctors.application.services.DoctorScheduleService;
import com.clinica.shared.dto.DoctorRegistrationRequest;
import com.clinica.shared.dto.DoctorResponse;
import com.clinica.shared.dto.DoctorScheduleRequest;
import com.clinica.shared.dto.DoctorScheduleResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller exposing endpoints for Doctor management.
 */
@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final DoctorScheduleService doctorScheduleService;

    public DoctorController(DoctorService doctorService,
                            DoctorScheduleService doctorScheduleService) {
        this.doctorService = doctorService;
        this.doctorScheduleService = doctorScheduleService;
    }


    /**
     * POST /api/v1/doctors — Registrar un nuevo médico.
     */
    @PostMapping
    public ResponseEntity<DoctorResponse> registerDoctor(@Valid @RequestBody DoctorRegistrationRequest request) {
        DoctorResponse response = doctorService.registerDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/doctors — Listar todos los médicos.
     */
    @GetMapping
    public ResponseEntity<List<DoctorResponse>> listDoctors() {
        return ResponseEntity.ok(doctorService.listDoctors());
    }

    /**
     * GET /api/v1/doctors/me — Obtener el médico autenticado.
     * Lee el username directamente del JWT de Keycloak (preferred_username),
     * sin necesidad de userId en localStorage.
     */
    @GetMapping("/me")
    public ResponseEntity<DoctorResponse> getMyDoctorProfile(
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(doctorService.getDoctorByUsername(username));
    }

    /**
     * GET /api/v1/doctors/by-user/{userId} — Obtener médico por ID de usuario (legacy).
     */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<DoctorResponse> getDoctorByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(doctorService.getDoctorByUserId(userId));
    }

    /**
     * GET /api/v1/doctors/{id} — Obtener un médico por ID.
     */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<DoctorResponse> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

}
