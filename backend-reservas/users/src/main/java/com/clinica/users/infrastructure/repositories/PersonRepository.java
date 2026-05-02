package com.clinica.users.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.clinica.shared.domain.entities.Person;

import java.util.Optional;

/**
 * Repository interface for managing Person entities.
 */
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    /**
     * Finds a Person by their identification document.
     * @param identification The identification string.
     * @return An Optional containing the Person if found.
     */
    Optional<Person> findByIdentification(String identification);
}
