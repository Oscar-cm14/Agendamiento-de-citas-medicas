package com.clinica.appointments.application.services;

import com.clinica.appointments.domain.entities.SystemConfiguration;
import com.clinica.shared.dto.ConfigurationRequest;

/**
 * Service interface for managing the global system configuration.
 */
public interface ConfigurationService {

    /**
     * Retrieves the global system configuration. Creates a default one if it does not exist.
     * @return the current SystemConfiguration.
     */
    SystemConfiguration getGlobalConfiguration();

    /**
     * Updates the global system configuration with new values.
     * @param request the DTO containing the new configuration values.
     * @return the updated SystemConfiguration.
     */
    SystemConfiguration updateWindowWeeks(ConfigurationRequest request);

}
