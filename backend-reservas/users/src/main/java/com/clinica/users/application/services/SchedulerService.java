package com.clinica.users.application.services;



import com.clinica.shared.dto.SchedulerRegistrationRequest;
import com.clinica.shared.dto.SchedulerResponse;

public interface SchedulerService {
    SchedulerResponse registerScheduler(SchedulerRegistrationRequest request);
}
