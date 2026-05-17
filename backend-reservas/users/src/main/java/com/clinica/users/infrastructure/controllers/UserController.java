package com.clinica.users.infrastructure.controllers;

import com.clinica.shared.domain.entities.Person;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.PersonRepository;
import com.clinica.users.infrastructure.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;

    public UserController(UserRepository userRepository, PersonRepository personRepository) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        String preferredUsername = "";
        String email = "";

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            if (jwt.hasClaim("preferred_username")) {
                preferredUsername = jwt.getClaimAsString("preferred_username");
            }
            if (jwt.hasClaim("email")) {
                email = jwt.getClaimAsString("email");
            }
        }

        final String u1 = username != null ? username.trim() : "";
        final String u2 = preferredUsername != null ? preferredUsername.trim() : "";
        final String u3 = email != null ? email.trim() : "";
        final String u1NoDomain = u1.contains("@") ? u1.substring(0, u1.indexOf('@')) : u1;
        final String u2NoDomain = u2.contains("@") ? u2.substring(0, u2.indexOf('@')) : u2;
        final String u3NoDomain = u3.contains("@") ? u3.substring(0, u3.indexOf('@')) : u3;

        // 1. Buscar en UserRepository
        Optional<User> userOpt = userRepository.findAll().stream()
                .filter(u -> {
                    String dbUser = u.getUsername() != null ? u.getUsername().trim() : "";
                    String dbEmail = u.getPerson() != null && u.getPerson().getEmail() != null ? u.getPerson().getEmail().trim() : "";
                    String dbUserNoDomain = dbUser.contains("@") ? dbUser.substring(0, dbUser.indexOf('@')) : dbUser;
                    String dbEmailNoDomain = dbEmail.contains("@") ? dbEmail.substring(0, dbEmail.indexOf('@')) : dbEmail;

                    return dbUser.equalsIgnoreCase(u1) || dbUser.equalsIgnoreCase(u2) || dbUser.equalsIgnoreCase(u3) ||
                           dbEmail.equalsIgnoreCase(u1) || dbEmail.equalsIgnoreCase(u2) || dbEmail.equalsIgnoreCase(u3) ||
                           (!dbUserNoDomain.isEmpty() && (dbUserNoDomain.equalsIgnoreCase(u1NoDomain) || dbUserNoDomain.equalsIgnoreCase(u2NoDomain) || dbUserNoDomain.equalsIgnoreCase(u3NoDomain))) ||
                           (!dbEmailNoDomain.isEmpty() && (dbEmailNoDomain.equalsIgnoreCase(u1NoDomain) || dbEmailNoDomain.equalsIgnoreCase(u2NoDomain) || dbEmailNoDomain.equalsIgnoreCase(u3NoDomain)));
                })
                .findFirst();

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("role", user.getRole().name());
            response.put("personId", user.getPerson() != null ? user.getPerson().getId() : user.getId());
            return ResponseEntity.ok(response);
        }

        // 2. Si no está en User, buscar en PersonRepository (tabla person / patients / doctors)
        Optional<Person> personOpt = personRepository.findAll().stream()
                .filter(p -> {
                    String pEmail = p.getEmail() != null ? p.getEmail().trim() : "";
                    String pId = p.getIdentification() != null ? p.getIdentification().trim() : "";
                    String pEmailNoDomain = pEmail.contains("@") ? pEmail.substring(0, pEmail.indexOf('@')) : pEmail;

                    return pEmail.equalsIgnoreCase(u1) || pEmail.equalsIgnoreCase(u2) || pEmail.equalsIgnoreCase(u3) ||
                           pId.equalsIgnoreCase(u1) || pId.equalsIgnoreCase(u2) || pId.equalsIgnoreCase(u3) ||
                           (!pEmailNoDomain.isEmpty() && (pEmailNoDomain.equalsIgnoreCase(u1NoDomain) || pEmailNoDomain.equalsIgnoreCase(u2NoDomain) || pEmailNoDomain.equalsIgnoreCase(u3NoDomain)));
                })
                .findFirst();

        if (personOpt.isPresent()) {
            Person person = personOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("userId", person.getId());
            response.put("username", u1.isEmpty() ? (u2.isEmpty() ? u3 : u2) : u1);
            response.put("role", "PATIENT");
            response.put("personId", person.getId());
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado en la base de datos local para: " + username + " / " + email));
    }
}
