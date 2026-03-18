package com.clinica.users.infrastructure.repositories;

import com.clinica.users.domain.entities.Scheduler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Scheduler entities.
 */
@Repository
public interface SchedulerRepository extends JpaRepository<Scheduler, Long> {
}
