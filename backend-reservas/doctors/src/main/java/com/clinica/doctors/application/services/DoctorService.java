package com.clinica.doctors.application.services;

import com.clinica.shared.dto.DoctorRegistrationRequest;
import com.clinica.shared.dto.DoctorResponse;

/**
 * Interface defining the operations available for Doctor management.
 */
public interface DoctorService {

    /**
     * Registers a new Doctor in the system along with their User account.
     *
     * @param request Data transfer object containing the necessary fields.
     * @return DoctorResponse with basic created entity info.
     */
    DoctorResponse registerDoctor(DoctorRegistrationRequest request);
}
