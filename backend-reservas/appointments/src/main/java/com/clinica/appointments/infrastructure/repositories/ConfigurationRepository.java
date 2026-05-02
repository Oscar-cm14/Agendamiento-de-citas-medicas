package com.clinica.appointments.infrastructure.repositories;

import com.clinica.appointments.domain.entities.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for accessing global system configuration.
 */
@Repository
public interface ConfigurationRepository extends JpaRepository<SystemConfiguration, Long> {
}
