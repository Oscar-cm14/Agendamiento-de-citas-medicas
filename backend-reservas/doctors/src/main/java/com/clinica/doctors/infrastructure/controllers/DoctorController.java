package com.clinica.doctors.infrastructure.controllers;


import com.clinica.doctors.application.services.DoctorService;
import com.clinica.shared.dto.DoctorDetailResponse;
import com.clinica.shared.dto.DoctorRegistrationRequest;
import com.clinica.shared.dto.DoctorResponse;
import com.clinica.shared.dto.DoctorUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller para gestión de médicos.
 * Solo maneja datos del médico (registro, consulta, edición).
 * Los horarios están en DoctorScheduleController.
 */
@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    
    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    // POST /api/v1/doctors  — Registrar nuevo médico
    @PostMapping
    public ResponseEntity<DoctorResponse> registerDoctor(
            @Valid @RequestBody DoctorRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(doctorService.registerDoctor(request));
    }

    // GET /api/v1/doctors  — Listar todos
    @GetMapping
    public ResponseEntity<List<DoctorResponse>> listDoctors() {
        return ResponseEntity.ok(doctorService.listDoctors());
    }

    // GET /api/v1/doctors/me  — Médico autenticado
    @GetMapping("/me")
    public ResponseEntity<DoctorDetailResponse> getMyDoctorProfile(
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        // Retorna detalle completo (incluye skills) para el panel del médico
        DoctorResponse basic = doctorService.getDoctorByUsername(username);
        return ResponseEntity.ok(doctorService.getDoctorDetailById(basic.id()));
    }

    // GET /api/v1/doctors/by-user/{userId}  — Por userId
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<DoctorResponse> getDoctorByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(doctorService.getDoctorByUserId(userId));
    }

    // GET /api/v1/doctors/{id}  — Detalle completo (para formulario edición admin)
    // El patrón \d+ evita colisión con /me y /by-user
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<DoctorDetailResponse> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getDoctorDetailById(id));
    }

    // PUT /api/v1/doctors/{id}  — Actualizar datos del médico (admin)
    @PutMapping("/{id}")
    public ResponseEntity<DoctorDetailResponse> updateDoctor(
            @PathVariable Long id,
            @RequestBody DoctorUpdateRequest request) {
        return ResponseEntity.ok(doctorService.updateDoctor(id, request));
    }

    
}
