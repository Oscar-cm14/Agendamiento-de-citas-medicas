package com.clinica.users.infrastructure.controllers;

import com.clinica.shared.dto.SchedulerDetailResponse;
import com.clinica.shared.dto.SchedulerRegistrationRequest;
import com.clinica.shared.dto.SchedulerResponse;
import com.clinica.shared.dto.SchedulerUpdateRequest;
import com.clinica.users.application.services.SchedulerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller para gestión de agendadores.
 *
 * CAMBIOS respecto a la versión original (solo tenía POST /register):
 *  - GET /            → NUEVO: lista todos los agendadores con detalle completo.
 *  - GET /{id}        → NUEVO: detalle de un agendador por ID.
 *  - PUT /{id}        → NUEVO: actualiza los datos de un agendador existente.
 *
 * El endpoint POST /register se conserva sin cambios.
 */
@RestController
@RequestMapping("/api/v1/schedulers")
public class SchedulerController {

    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/v1/schedulers/register  — Registrar (sin cambios)
    // ─────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<SchedulerResponse> registerScheduler(
            @Valid @RequestBody SchedulerRegistrationRequest request) {
        return new ResponseEntity<>(schedulerService.registerScheduler(request), HttpStatus.CREATED);
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/schedulers  — NUEVO: lista todos los agendadores
    // Solo ADMIN (agregar hasRole("ADMIN") en SecurityConfig).
    // ─────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<SchedulerDetailResponse>> listSchedulers() {
        return ResponseEntity.ok(schedulerService.listSchedulers());
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/schedulers/{id}  — NUEVO: detalle de un agendador
    // ─────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<SchedulerDetailResponse> getSchedulerById(@PathVariable Long id) {
        return ResponseEntity.ok(schedulerService.getSchedulerDetailById(id));
    }

    // ─────────────────────────────────────────────────────────
    // PUT /api/v1/schedulers/{id}  — NUEVO: actualizar agendador
    // Partial update: solo se modifican los campos no-nulos.
    // Requiere rol ADMIN (agregar en SecurityConfig).
    // ─────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<SchedulerDetailResponse> updateScheduler(
            @PathVariable Long id,
            @RequestBody SchedulerUpdateRequest request) {
        return ResponseEntity.ok(schedulerService.updateScheduler(id, request));
    }
}
