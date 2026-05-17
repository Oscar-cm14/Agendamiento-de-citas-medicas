package com.clinica.appointments.infrastructure.repositories;

import com.clinica.appointments.domain.entities.AppointmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for AppointmentHistory entity.
 */
@Repository
public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, Long> {
}
