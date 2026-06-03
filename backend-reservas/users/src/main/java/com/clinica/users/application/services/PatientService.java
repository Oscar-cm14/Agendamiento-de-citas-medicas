package com.clinica.users.application.services;

import com.clinica.shared.dto.PatientDetailResponse;
import com.clinica.shared.dto.PatientRegistrationRequest;
import com.clinica.shared.dto.PatientResponse;
import com.clinica.shared.dto.PatientUpdateRequest;

import java.util.Optional;

/**
 * Service interface for managing patients.
 * Handles RF3: patient self-registration from the web.
 */
public interface PatientService {

    // RF3: Registrar un paciente desde la web
    PatientResponse registerPatient(PatientRegistrationRequest request);

    // Buscar paciente por cédula (identification) para autocompletar en el panel agendador
    Optional<PatientDetailResponse> findByIdentification(String identification);

    // Buscar paciente por username (usado en el login del paciente para obtener su ID)
    Optional<PatientDetailResponse> findByUsername(String username);

   PatientDetailResponse updatePatient(
        Long patientId,
        PatientUpdateRequest request
);

Optional<PatientDetailResponse> findById(Long id);
}
