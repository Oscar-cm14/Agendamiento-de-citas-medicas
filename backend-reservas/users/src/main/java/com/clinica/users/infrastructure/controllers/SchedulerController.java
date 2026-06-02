package com.clinica.users.infrastructure.controllers;

import com.clinica.shared.dto.SchedulerDetailResponse;
import com.clinica.shared.dto.SchedulerRegistrationRequest;
import com.clinica.shared.dto.SchedulerResponse;
import com.clinica.shared.dto.SchedulerUpdateRequest;
import com.clinica.users.application.services.SchedulerService;
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

@RestController
@RequestMapping("/api/v1/schedulers")
public class SchedulerController {

    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    // POST /register — Registrar agendador (público)
    @PostMapping("/register")
    public ResponseEntity<SchedulerResponse> registerScheduler(
            @Valid @RequestBody SchedulerRegistrationRequest request) {
        return new ResponseEntity<>(schedulerService.registerScheduler(request), HttpStatus.CREATED);
    }

    // GET / — Listar todos (solo ADMIN)
    @GetMapping
    public ResponseEntity<List<SchedulerDetailResponse>> listSchedulers() {
        return ResponseEntity.ok(schedulerService.listSchedulers());
    }

    // GET /me — Agendador autenticado ve su propio perfil
    @GetMapping("/me")
    public ResponseEntity<SchedulerDetailResponse> getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(schedulerService.getSchedulerByUsername(username));
    }

    // PUT /me — Agendador autenticado edita su propio perfil
    @PutMapping("/me")
    public ResponseEntity<SchedulerDetailResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody SchedulerUpdateRequest request) {
        String username = jwt.getClaimAsString("preferred_username");
        SchedulerDetailResponse current = schedulerService.getSchedulerByUsername(username);
        return ResponseEntity.ok(schedulerService.updateScheduler(current.id(), request));
    }

    // GET /{id} — Detalle por ID (solo ADMIN)
    @GetMapping("/{id}")
    public ResponseEntity<SchedulerDetailResponse> getSchedulerById(@PathVariable Long id) {
        return ResponseEntity.ok(schedulerService.getSchedulerDetailById(id));
    }

    // PUT /{id} — Actualizar por ID (solo ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<SchedulerDetailResponse> updateScheduler(
            @PathVariable Long id,
            @RequestBody SchedulerUpdateRequest request) {
        return ResponseEntity.ok(schedulerService.updateScheduler(id, request));
    }
}
