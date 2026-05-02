package com.clinica.appointments.infrastructure.controllers;

import com.clinica.appointments.application.services.ConfigurationService;
import com.clinica.appointments.domain.entities.SystemConfiguration;
import com.clinica.shared.dto.ConfigurationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for managing global system configurations.
 * Handles RF4 requirements.
 */
@RestController
@RequestMapping("/api/v1/configurations")
@RequiredArgsConstructor
public class ConfigurationController {

    private final ConfigurationService configurationService;

    /**
     * Retrieves the current global system configuration.
     * @return 200 OK with the SystemConfiguration.
     */
    @GetMapping
    public ResponseEntity<SystemConfiguration> getGlobalConfiguration() {
        return ResponseEntity.ok(configurationService.getGlobalConfiguration());
    }

    /**
     * Updates the global system configuration appointment window.
     * Accessible only by ADMIN role (secured in SecurityConfig).
     * @param request the new configuration values.
     * @return 200 OK with the updated SystemConfiguration.
     */
    @PutMapping
    public ResponseEntity<SystemConfiguration> updateConfiguration(
            @Valid @RequestBody ConfigurationRequest request) {
        return ResponseEntity.ok(configurationService.updateWindowWeeks(request));
    }

}
