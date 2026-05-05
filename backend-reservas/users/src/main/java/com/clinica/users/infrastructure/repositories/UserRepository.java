package com.clinica.users.infrastructure.repositories;

import com.clinica.users.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    // FIX: query eficiente para buscar el username de una persona por su ID
    // Evita cargar todos los usuarios en memoria con findAll()
    @Query("SELECT u.username FROM User u WHERE u.person.id = :personId")
    Optional<String> findUsernameByPersonId(@Param("personId") Long personId);
}