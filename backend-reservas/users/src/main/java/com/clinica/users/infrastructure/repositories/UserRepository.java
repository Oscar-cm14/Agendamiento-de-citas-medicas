package com.clinica.users.infrastructure.repositories;

import com.clinica.users.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Busca un usuario por el id de la persona asociada
     * (Doctor, Patient, Admin, etc.).
     */
    Optional<User> findByPersonId(Long personId);
}
