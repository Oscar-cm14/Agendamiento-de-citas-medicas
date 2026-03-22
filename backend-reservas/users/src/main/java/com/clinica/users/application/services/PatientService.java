package com.clinica.users.application.services;

import com.clinica.shared.dto.PatientRegistrationRequest;

/**
 * Interface defining the operations available for Patient management.
 */
public interface PatientService {

    /**
     * Registers a new Patient. If an email is provided, a User account is also created.
     *
     * @param request Data transfer object containing the necessary fields.
     */
    void registerPatient(PatientRegistrationRequest request);
}
