package com.clinica.users.infrastructure.controllers;

import com.clinica.shared.dto.PatientDetailResponse;
import com.clinica.shared.dto.PatientRegistrationRequest;
import com.clinica.shared.dto.PatientResponse;
import com.clinica.users.application.services.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }


    /**
     * Busca un paciente por número de cédula para autocompletar
     * el formulario en el panel del agendador.
     * GET /api/v1/patients/by-identification?identification=123456
     */
    @GetMapping("/by-identification")
    public ResponseEntity<PatientDetailResponse> findByIdentification(
            @RequestParam String identification) {

        return patientService.findByIdentification(identification)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
