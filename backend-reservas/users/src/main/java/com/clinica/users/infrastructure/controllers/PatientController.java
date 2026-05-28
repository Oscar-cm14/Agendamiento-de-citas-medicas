package com.clinica.users.infrastructure.controllers;

import com.clinica.shared.dto.PatientDetailResponse;
import com.clinica.shared.dto.PatientRegistrationRequest;
import com.clinica.shared.dto.PatientResponse;
import com.clinica.users.application.services.PatientService;
import com.clinica.users.domain.entities.Patient;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.clinica.shared.dto.PatientUpdateRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;


/**
 * REST Controller para gestión de pacientes.
 *
 * Endpoints disponibles:
 *   POST /api/v1/patients/register              → RF3: registrar paciente
 *   GET  /api/v1/patients/by-identification     → buscar por cédula (agendador)
 *   GET  /api/v1/patients/by-username           → buscar por username (login paciente)
 *   GET  /api/v1/patients/by-id/{id}            → buscar por ID numérico
 *   PUT  /api/v1/patients/{id}                  → actualizar perfil del paciente
 */
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // =========================================================
    // RF3: Registrar paciente desde la web
    // =========================================================

    /**
     * POST /api/v1/patients/register
     * Registra un nuevo paciente (auto-registro desde formulario web).
     * Si la cédula ya existe devuelve los datos del existente (HTTP 200).
     */
    @PostMapping("/register")
    public ResponseEntity<PatientResponse> register(
            @Valid @RequestBody PatientRegistrationRequest request) {

        PatientResponse response = patientService.registerPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================
    // Buscar por cédula
    // =========================================================

    /**
     * GET /api/v1/patients/by-identification?identification=CEDULA
     *
     * Busca un paciente por su número de cédula.
     * Usado por el panel del agendador para autocompletar los datos
     * del paciente al crear una nueva cita.
     *
     * Respuestas:
     *   200 OK        → paciente encontrado con todos sus datos
     *   404 Not Found → no existe paciente con esa cédula
     */
    @GetMapping("/by-identification")
    public ResponseEntity<PatientDetailResponse> findByIdentification(
            @RequestParam String identification) {

        return patientService.findByIdentification(identification)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================================================
    // ✅ CORRECCIÓN: Buscar por username (panel del paciente)
    // =========================================================

    /**
     * GET /api/v1/patients/by-username?username=...
     *
     * Busca un paciente por su username de login (Keycloak).
     * Este endpoint es llamado por agendar-cita.ts en resolverPatientId()
     * cuando el userId no está en localStorage.
     *
     * Respuestas:
     *   200 OK        → paciente encontrado con todos sus datos
     *   404 Not Found → no existe paciente con ese username
     */
    @GetMapping("/by-username")
    public ResponseEntity<PatientDetailResponse> findByUsername(
            @RequestParam String username) {

        return patientService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================================================
    // ACTUALIZAR INFORMACIÓN DEL PACIENTE
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<PatientDetailResponse> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientUpdateRequest request
    ) {

        PatientDetailResponse response =
                patientService.updatePatient(id, request);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // BUSCAR PACIENTE POR ID
    // =========================================================

    @GetMapping("/by-id/{id}")
    public ResponseEntity<PatientDetailResponse> findById(
            @PathVariable Long id
    ) {

        return patientService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
