package com.clinica.users.application.services;


import com.clinica.shared.dto.PatientRegistrationRequest;
import com.clinica.shared.dto.PatientResponse;

/**
 * Service interface for managing patients.
 * Handles RF3: patient self-registration from the web.
 */
public interface PatientService {

    // RF3: Registrar un paciente desde la web
    PatientResponse registerPatient(PatientRegistrationRequest request);
}