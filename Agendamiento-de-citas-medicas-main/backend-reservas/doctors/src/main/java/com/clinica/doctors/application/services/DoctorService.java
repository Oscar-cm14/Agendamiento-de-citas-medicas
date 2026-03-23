package com.clinica.doctors.application.services;


import com.clinica.shared.dto.DoctorRegistrationRequest;
import com.clinica.shared.dto.DoctorResponse;

import java.util.List;

/**
 * Interface defining the operations available for Doctor management.
 */
public interface DoctorService {

    DoctorResponse registerDoctor(DoctorRegistrationRequest request);

   
    List<DoctorResponse> listDoctors();

  
    DoctorResponse getDoctorById(Long id);
}
