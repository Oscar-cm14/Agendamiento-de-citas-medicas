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
     * Finds a user by their username.
     * @param username The string representing the username.
     * @return An Optional containing the User if found.
     */
    Optional<User> findByUsername(String username);
}
