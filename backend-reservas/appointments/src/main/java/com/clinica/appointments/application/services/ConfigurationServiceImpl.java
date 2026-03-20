package com.clinica.appointments.application.services;

import com.clinica.appointments.domain.entities.SystemConfiguration;
import com.clinica.appointments.infrastructure.repositories.ConfigurationRepository;
import com.clinica.shared.dto.ConfigurationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the global system configuration service.
 */
@Service
@RequiredArgsConstructor
public class ConfigurationServiceImpl implements ConfigurationService {

    private final ConfigurationRepository configurationRepository;

    @Override
    @Transactional
    public SystemConfiguration getGlobalConfiguration() {
        return configurationRepository.findAll().stream().findFirst().orElseGet(() -> {
            SystemConfiguration defaultConfig = new SystemConfiguration();
            defaultConfig.setAppointmentWindowWeeks(4);
            return configurationRepository.save(defaultConfig);
        });
    }

    @Override
    @Transactional
    public SystemConfiguration updateWindowWeeks(ConfigurationRequest request) {
        SystemConfiguration config = getGlobalConfiguration();
        config.setAppointmentWindowWeeks(request.appointmentWindowWeeks());
        return configurationRepository.save(config);
    }
}
