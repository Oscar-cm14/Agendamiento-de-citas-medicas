

package com.clinica.users.infrastructure.controllers;

import com.clinica.shared.domain.entities.Person;
import com.clinica.shared.dto.UserRolesRequest;
import com.clinica.shared.dto.UserSummaryResponse;
import com.clinica.shared.infrastructure.keycloak.KeycloakAdminService;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.PersonRepository;
import com.clinica.users.infrastructure.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller para gestión de usuarios.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository       userRepository;
    private final PersonRepository     personRepository;
    private final KeycloakAdminService keycloakAdminService;

    public UserController(UserRepository userRepository,
                          PersonRepository personRepository,
                          KeycloakAdminService keycloakAdminService) {
        this.userRepository       = userRepository;
        this.personRepository     = personRepository;
        this.keycloakAdminService = keycloakAdminService;
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/users/me  — Usuario autenticado 
    // ─────────────────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username          = authentication.getName();
        String preferredUsername = "";
        String email             = "";

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            if (jwt.hasClaim("preferred_username")) preferredUsername = jwt.getClaimAsString("preferred_username");
            if (jwt.hasClaim("email"))              email             = jwt.getClaimAsString("email");
        }

        final String u1   = username          != null ? username.trim()          : "";
        final String u2   = preferredUsername != null ? preferredUsername.trim() : "";
        final String u3   = email             != null ? email.trim()             : "";
        final String u1ND = stripDomain(u1), u2ND = stripDomain(u2), u3ND = stripDomain(u3);

        Optional<User> userOpt = userRepository.findAll().stream()
                .filter(u -> {
                    String dbUser  = nvl(u.getUsername());
                    String dbEmail = u.getPerson() != null ? nvl(u.getPerson().getEmail()) : "";
                    return matches(dbUser, u1, u2, u3, u1ND, u2ND, u3ND)
                        || matches(dbEmail, u1, u2, u3, u1ND, u2ND, u3ND);
                })
                .findFirst();

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> resp = new HashMap<>();
            resp.put("userId",   user.getId());
            resp.put("username", user.getUsername());
            resp.put("role",     user.getRole().name());
            resp.put("personId", user.getPerson() != null ? user.getPerson().getId() : user.getId());
            return ResponseEntity.ok(resp);
        }

        Optional<Person> personOpt = personRepository.findAll().stream()
                .filter(p -> {
                    String pEmail = nvl(p.getEmail());
                    String pId    = nvl(p.getIdentification());
                    return matches(pEmail, u1, u2, u3, u1ND, u2ND, u3ND)
                        || matches(pId, u1, u2, u3, u1ND, u2ND, u3ND);
                })
                .findFirst();

        if (personOpt.isPresent()) {
            Person person = personOpt.get();
            Map<String, Object> resp = new HashMap<>();
            resp.put("userId",   person.getId());
            resp.put("username", u1.isEmpty() ? (u2.isEmpty() ? u3 : u2) : u1);
            resp.put("role",     "PATIENT");
            resp.put("personId", person.getId());
            return ResponseEntity.ok(resp);
        }

        return ResponseEntity.status(404).body(
            Map.of("error", "Usuario no encontrado en la base de datos local para: "
                + username + " / " + email));
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/users  — Listar todos los usuarios
    //
    //  se filtran los usuarios cuyo localId == -1
    // (existen en Keycloak pero no en H2). Sin este filtro, el
    // frontend llama a GET /users/-1/roles y recibe 500 / 401.
    // ─────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> listUsers() {

        List<Map<String, Object>> keycloakUsers = keycloakAdminService.listUsersWithRoles();

        List<UserSummaryResponse> result = keycloakUsers.stream()
                .map(ku -> {
                    String kcUsername = (String) ku.get("username");
                    String firstName  = (String) ku.getOrDefault("firstName", "");
                    String lastName   = (String) ku.getOrDefault("lastName",  "");
                    String kcEmail    = (String) ku.getOrDefault("email",     "");

                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) ku.getOrDefault("realmRoles", List.of());

                    // Buscar ID local; -1 si no existe en H2
                    Long localId = userRepository.findByUsername(kcUsername)
                            .map(User::getId)
                            .orElse(-1L);

                    return new UserSummaryResponse(
                            localId,
                            kcUsername,
                            (firstName + " " + lastName).trim(),
                            kcEmail,
                            roles
                    );
                })
                // Excluir cuentas de servicio internas de Keycloak
                .filter(u -> !u.username().startsWith("service-account-"))
                // CORRECCIÓN: excluir usuarios que no existen en H2 (localId == -1)
                // Para que el frontend no intente GET /users/-1/roles → 500/401
                .filter(u -> u.id() != null && u.id() > 0)
                .toList();

        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/users/{id}/roles  — Obtener roles de un usuario
    // Sin cambios en la lógica; el filtro de listUsers() previene
    // que llegue aquí con id == -1.
    // ─────────────────────────────────────────────────────────
    @GetMapping("/{id}/roles")
    public ResponseEntity<List<String>> getUserRoles(@PathVariable String id) {
        String keycloakId = resolveKeycloakId(id);
        return ResponseEntity.ok(keycloakAdminService.getUserRoles(keycloakId));
    }

    // ─────────────────────────────────────────────────────────
    // PUT /api/v1/users/{id}/roles  — Actualizar roles de un usuario
    // ─────────────────────────────────────────────────────────
    @PutMapping("/{id}/roles")
    public ResponseEntity<Void> updateUserRoles(
            @PathVariable String id,
            @RequestBody UserRolesRequest request) {

        if (request.roles() == null || request.roles().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String keycloakId = resolveKeycloakId(id);
        keycloakAdminService.updateUserRolesByKeycloakId(keycloakId, request.roles());
        return ResponseEntity.ok().build();
    }

    // =========================================================
    // Helpers privados
    // =========================================================

    /**
     * Resuelve el keycloakId a partir del {id} recibido en la URL.
     *
     * CORRECCIÓN: si el id numérico es negativo (< 0) se lanza una
     * excepción descriptiva en lugar de buscar un ID inválido en Keycloak,
     * lo que antes causaba un 500 incomprensible.
     */
    private String resolveKeycloakId(String id) {
        try {
            Long localId = Long.parseLong(id);

            // CORRECCIÓN: rechazar IDs negativos (usuario no sincronizado con H2)
            if (localId < 0) {
                throw new RuntimeException(
                    "El usuario con id=" + localId
                    + " no está sincronizado en la base de datos local. "
                    + "No se puede obtener su keycloakId.");
            }

            User user = userRepository.findById(localId)
                    .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado con id local: " + localId));
            return keycloakAdminService.getUserKeycloakId(user.getUsername());

        } catch (NumberFormatException ignored) {
            // No es un número: se asume que ya es un UUID de Keycloak
            return id;
        }
    }

    private static String nvl(String s)          { return s != null ? s.trim() : ""; }
    private static String stripDomain(String s)  { return s.contains("@") ? s.substring(0, s.indexOf('@')) : s; }

    private static boolean matches(String candidate, String u1, String u2, String u3,
                                   String u1ND, String u2ND, String u3ND) {
        if (candidate.isEmpty()) return false;
        String cND = stripDomain(candidate);
        return candidate.equalsIgnoreCase(u1) || candidate.equalsIgnoreCase(u2) || candidate.equalsIgnoreCase(u3)
            || (!cND.isEmpty() && (cND.equalsIgnoreCase(u1ND) || cND.equalsIgnoreCase(u2ND) || cND.equalsIgnoreCase(u3ND)));
    }
}