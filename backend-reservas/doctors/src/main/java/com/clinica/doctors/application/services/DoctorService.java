package com.clinica.doctors.application.services;

import com.clinica.shared.dto.DoctorRegistrationRequest;
import com.clinica.shared.dto.DoctorResponse;
import com.clinica.shared.dto.DoctorScheduleRequest;
import java.util.List;

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

    /**
     * Adds multiple availability schedules to a doctor.
     *
     * @param doctorId  The ID of the doctor to update.
     * @param schedules List of schedule requests to add.
     */
    void addSchedulesToDoctor(Long doctorId, List<DoctorScheduleRequest> schedules);
}
