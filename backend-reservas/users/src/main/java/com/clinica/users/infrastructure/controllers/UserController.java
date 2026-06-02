package com.clinica.users.infrastructure.controllers;

import com.clinica.shared.domain.entities.Person;
import com.clinica.shared.dto.LoginRequest;
import com.clinica.shared.dto.UserRolesRequest;
import com.clinica.shared.dto.UserSummaryResponse;
import com.clinica.shared.infrastructure.keycloak.KeycloakAdminService;
import com.clinica.users.application.services.PatientService;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.PersonRepository;
import com.clinica.users.infrastructure.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.client-id:clinica-frontend}")
    private String clientId;

    private final UserRepository       userRepository;
    private final PersonRepository     personRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final PatientService       patientService;
    private final WebClient            webClient;
    private final ObjectMapper         objectMapper = new ObjectMapper();

    public UserController(UserRepository userRepository,
                          PersonRepository personRepository,
                          KeycloakAdminService keycloakAdminService,
                          PatientService patientService,
                          WebClient.Builder webClientBuilder) {
        this.userRepository       = userRepository;
        this.personRepository     = personRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.patientService       = patientService;
        this.webClient            = webClientBuilder.build();
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/v1/users/login  — Login (proxy a Keycloak)
    // ─────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id",  clientId);
        body.add("grant_type", "password");
        body.add("username",   request.username());
        body.add("password",   request.password());

        Map<?, ?> keycloakResponse;
        try {
            keycloakResponse = webClient.post()
                    .uri(serverUrl + "/realms/" + realm
                         + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(body))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException ex) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Usuario o contraseña incorrectos"));
        } catch (Exception ex) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Keycloak no disponible: " + ex.getMessage()));
        }

        if (keycloakResponse == null || !keycloakResponse.containsKey("access_token")) {
            return ResponseEntity.status(500).body(Map.of("error", "Error de autenticación"));
        }

        String token = (String) keycloakResponse.get("access_token");
        String role  = extraerRol(token);

        Map<String, Object> result = new HashMap<>();
        result.put("token",    token);
        result.put("role",     role);
        result.put("username", request.username());

        if ("PATIENT".equals(role)) {
            patientService.findByUsername(request.username())
                    .ifPresent(p -> result.put("userId", p.id()));
        }

        return ResponseEntity.ok(result);
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
            Map.of("error", "Usuario no encontrado: " + username + " / " + email));
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/users  — Listar todos
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

                    Long localId = userRepository.findByUsername(kcUsername)
                            .map(User::getId)
                            .orElse(-1L);

                    return new UserSummaryResponse(localId, kcUsername,
                            (firstName + " " + lastName).trim(), kcEmail, roles);
                })
                .filter(u -> !u.username().startsWith("service-account-"))
                .filter(u -> u.id() != null && u.id() > 0)
                .toList();

        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/users/{id}/roles
    // ─────────────────────────────────────────────────────────
    @GetMapping("/{id}/roles")
    public ResponseEntity<List<String>> getUserRoles(@PathVariable String id) {
        String keycloakId = resolveKeycloakId(id);
        return ResponseEntity.ok(keycloakAdminService.getUserRoles(keycloakId));
    }

    // ─────────────────────────────────────────────────────────
    // PUT /api/v1/users/{id}/roles
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

    @SuppressWarnings("unchecked")
    private String extraerRol(String jwt) {
        try {
            String[] parts   = jwt.split("\\.");
            String   payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> claims      = objectMapper.readValue(payload, Map.class);
            Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
            if (realmAccess != null) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                if (roles != null) {
                    if (roles.contains("ADMIN"))     return "ADMIN";
                    if (roles.contains("DOCTOR"))    return "DOCTOR";
                    if (roles.contains("SCHEDULER")) return "SCHEDULER";
                    if (roles.contains("PATIENT"))   return "PATIENT";
                }
            }
        } catch (Exception ignored) { }
        return "PATIENT";
    }

    private String resolveKeycloakId(String id) {
        try {
            Long localId = Long.parseLong(id);
            if (localId < 0) throw new RuntimeException(
                "Usuario id=" + localId + " no sincronizado con H2.");
            User user = userRepository.findById(localId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + localId));
            return keycloakAdminService.getUserKeycloakId(user.getUsername());
        } catch (NumberFormatException ignored) {
            return id;
        }
    }

    private static String nvl(String s)         { return s != null ? s.trim() : ""; }
    private static String stripDomain(String s) { return s.contains("@") ? s.substring(0, s.indexOf('@')) : s; }

    private static boolean matches(String candidate, String u1, String u2, String u3,
                                   String u1ND, String u2ND, String u3ND) {
        if (candidate.isEmpty()) return false;
        String cND = stripDomain(candidate);
        return candidate.equalsIgnoreCase(u1) || candidate.equalsIgnoreCase(u2) || candidate.equalsIgnoreCase(u3)
            || (!cND.isEmpty() && (cND.equalsIgnoreCase(u1ND) || cND.equalsIgnoreCase(u2ND) || cND.equalsIgnoreCase(u3ND)));
    }
}