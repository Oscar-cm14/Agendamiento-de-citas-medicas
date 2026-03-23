package com.clinica.users.infrastructure.controllers;


import com.clinica.shared.dto.SchedulerRegistrationRequest;
import com.clinica.shared.dto.SchedulerResponse;
import com.clinica.users.application.services.SchedulerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for scheduler registration.
 * Only accessible by ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/schedulers")
public class SchedulerController {

    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    /**
     * POST /api/v1/schedulers/register
     */
    @PostMapping("/register")
    public ResponseEntity<SchedulerResponse> registerScheduler(
            @Valid @RequestBody SchedulerRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schedulerService.registerScheduler(request));
    }
}
