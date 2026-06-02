package com.clinica.shared.infrastructure.keycloak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio que interactúa con la API Admin de Keycloak.
 */
@Service
public class KeycloakAdminService {

    // Excepción interna para distinguir el 409 (usuario ya existe)
    private static class UserAlreadyExistsInKeycloakException extends RuntimeException {
        public UserAlreadyExistsInKeycloakException(String msg) { super(msg); }
    }

    private final WebClient webClient;

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    public KeycloakAdminService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    // =========================================================
    // Obtener token de administrador
    // =========================================================

    private String getAdminToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("username", adminUsername);
        body.add("password", adminPassword);
        body.add("grant_type", "password");

        try {
            Map<?, ?> response = webClient.post()
                    .uri(serverUrl + "/realms/master/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(body))
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .map(errorBody -> new RuntimeException(
                                "Keycloak rechazó la autenticación admin. Status: "
                                + clientResponse.statusCode()
                                + " | Body: " + errorBody))
                    )
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("access_token")) {
                throw new RuntimeException(
                    "Keycloak no devolvió access_token. Verifica las credenciales en application.properties");
            }
            return (String) response.get("access_token");

        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                "Error conectando con Keycloak en " + serverUrl
                + ". Verifica que Keycloak esté corriendo. Detalle: " + ex.getMessage(), ex);
        }
    }

    // =========================================================
    // Crear usuario en Keycloak y asignar rol
    // BUG-FIX: si el usuario ya existe (409), actualiza su contraseña
    //          y re-asigna el rol en vez de fallar.
    // BUG-FIX: reintenta getUserKeycloakId para evitar race condition de timing.
    // =========================================================

    public void createUser(String username, String password, String email,
                           String firstName, String lastName, String roleName) {

        String token   = getAdminToken();
        String baseUrl = serverUrl + "/admin/realms/" + realm;

        Map<String, Object> userRepresentation = new HashMap<>();
        userRepresentation.put("username",       username);
        userRepresentation.put("firstName",      firstName);
        userRepresentation.put("lastName",       lastName);
        userRepresentation.put("enabled",        true);
        userRepresentation.put("requiredActions", List.of());
        userRepresentation.put("credentials", List.of(Map.of(
                "type",      "password",
                "value",     password,
                "temporary", false
        )));

        userRepresentation.put("emailVerified", true);
        if (email != null && !email.isBlank()) {
            userRepresentation.put("email", email);
        }

        // BUG-FIX 1: Si el usuario ya existe en Keycloak (409 Conflict),
        // en vez de lanzar excepción (que haría rollback de H2),
        // actualizamos su contraseña y continuamos.
        boolean usuarioYaExistia = false;
        try {
            webClient.post()
                    .uri(baseUrl + "/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userRepresentation)
                    .retrieve()
                    .onStatus(
                        status -> status.value() == 409,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .map(body -> new UserAlreadyExistsInKeycloakException(
                                "Usuario '" + username + "' ya existe en Keycloak"))
                    )
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .map(errorBody -> new RuntimeException(
                                "Keycloak rechazó la creación del usuario '" + username
                                + "'. Status: " + clientResponse.statusCode()
                                + " | Body: " + errorBody))
                    )
                    .toBodilessEntity()
                    .block();
        } catch (UserAlreadyExistsInKeycloakException ex) {
            usuarioYaExistia = true;
        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                "Error al crear usuario '" + username + "' en Keycloak: " + ex.getMessage(), ex);
        }

        // BUG-FIX 2: Obtener el UUID del usuario con reintentos.
        // Keycloak a veces no indexa al usuario inmediatamente luego del POST,
        // por lo que un GET inmediato puede devolver lista vacía.
        String userId = getUserKeycloakIdConReintento(username, token, baseUrl);

        if (usuarioYaExistia) {
            // Actualizar la contraseña con la nueva que eligió el paciente
            actualizarPasswordEnKeycloak(userId, password, token, baseUrl);
        }

        // Asignar el rol. Si el usuario ya lo tiene, Keycloak lo ignora.
        assignRoleToUser(userId, roleName, token, baseUrl);
    }

    // =========================================================
    // Listar usuarios del realm con sus roles
    // =========================================================

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listUsersWithRoles() {
        String token   = getAdminToken();
        String baseUrl = serverUrl + "/admin/realms/" + realm;

        List<Map<String, Object>> users;
        try {
            users = (List<Map<String, Object>>) webClient.get()
                    .uri(baseUrl + "/users?max=200")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .map(errorBody -> new RuntimeException(
                                "Error listando usuarios de Keycloak. Status: "
                                + clientResponse.statusCode() + " | Body: " + errorBody))
                    )
                    .bodyToMono(List.class)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Error listando usuarios de Keycloak: " + ex.getMessage(), ex);
        }

        if (users == null) return List.of();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> user : users) {
            String userId      = (String) user.get("id");
            List<String> roles = getUserRoles(userId, token, baseUrl);

            Map<String, Object> enriched = new HashMap<>(user);
            enriched.put("realmRoles", roles);
            result.add(enriched);
        }
        return result;
    }

    // =========================================================
    // Obtener roles de un usuario por keycloakId (public)
    // =========================================================

    public List<String> getUserRoles(String keycloakId) {
        String token   = getAdminToken();
        String baseUrl = serverUrl + "/admin/realms/" + realm;
        return getUserRoles(keycloakId, token, baseUrl);
    }

    // =========================================================
    // Obtener el keycloakId de un usuario por username (public)
    // =========================================================

    public String getUserKeycloakId(String username) {
        String token   = getAdminToken();
        String baseUrl = serverUrl + "/admin/realms/" + realm;
        return getUserKeycloakId(username, token, baseUrl);
    }

    // =========================================================
    // Reemplazar roles de un usuario por keycloakId
    // =========================================================

    public void updateUserRolesByKeycloakId(String keycloakId, List<String> newRoles) {
        if (newRoles == null || newRoles.isEmpty()) {
            throw new IllegalArgumentException("El usuario debe tener al menos un rol.");
        }

        String token   = getAdminToken();
        String baseUrl = serverUrl + "/admin/realms/" + realm;

        List<String> currentRoleNames = getUserRoles(keycloakId, token, baseUrl);

        if (!currentRoleNames.isEmpty()) {
            List<Map<String, Object>> currentRoleReps = currentRoleNames.stream()
                    .map(roleName -> getRoleRepresentation(roleName, token, baseUrl))
                    .filter(r -> r != null)
                    .collect(Collectors.toList());

            if (!currentRoleReps.isEmpty()) {
                webClient.method(HttpMethod.DELETE)
                        .uri(baseUrl + "/users/" + keycloakId + "/role-mappings/realm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(currentRoleReps)
                        .retrieve()
                        .toBodilessEntity()
                        .block();
            }
        }

        for (String roleName : newRoles) {
            assignRoleToUser(keycloakId, roleName, token, baseUrl);
        }
    }

    // =========================================================
    // Actualizar contraseña de un usuario por username (public)
    // =========================================================

    public void updatePassword(String username, String newPassword) {
        String token   = getAdminToken();
        String baseUrl = serverUrl + "/admin/realms/" + realm;
        String userId  = getUserKeycloakId(username, token, baseUrl);
        actualizarPasswordEnKeycloak(userId, newPassword, token, baseUrl);
    }

    // =========================================================
    // Métodos privados de soporte
    // =========================================================

    /**
     * BUG-FIX: Reintenta hasta 3 veces con 300ms de espera entre intentos.
     * Keycloak puede no indexar al usuario inmediatamente después del POST /users,
     * haciendo que un GET inmediato devuelva lista vacía y lance excepción.
     */
    private String getUserKeycloakIdConReintento(String username, String token, String baseUrl) {
        int maxIntentos = 3;
        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                return getUserKeycloakId(username, token, baseUrl);
            } catch (RuntimeException ex) {
                if (intento == maxIntentos) throw ex;
                try { Thread.sleep(300L * intento); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw new RuntimeException("No se pudo obtener el keycloakId de '" + username + "' tras " + maxIntentos + " intentos.");
    }

    /**
     * Actualiza la contraseña de un usuario en Keycloak usando su UUID.
     */
    private void actualizarPasswordEnKeycloak(String userId, String newPassword, String token, String baseUrl) {
        Map<String, Object> credential = new HashMap<>();
        credential.put("type",      "password");
        credential.put("temporary", false);
        credential.put("value",     newPassword);

        try {
            webClient.put()
                    .uri(baseUrl + "/users/" + userId + "/reset-password")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(credential)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .map(errorBody -> new RuntimeException(
                                "Error actualizando contraseña. Status: "
                                + clientResponse.statusCode()
                                + " | Body: " + errorBody))
                    )
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                "Error actualizando contraseña en Keycloak: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getUserRoles(String keycloakId, String token, String baseUrl) {
        try {
            List<Map<String, Object>> roles = (List<Map<String, Object>>) webClient.get()
                    .uri(baseUrl + "/users/" + keycloakId + "/role-mappings/realm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .map(errorBody -> new RuntimeException(
                                "Error obteniendo roles del usuario '" + keycloakId
                                + "'. Status: " + clientResponse.statusCode()
                                + " | Body: " + errorBody))
                    )
                    .bodyToMono(List.class)
                    .block();

            if (roles == null) return List.of();

            return roles.stream()
                    .map(r -> (String) r.get("name"))
                    .filter(name -> name != null
                            && !name.startsWith("default-roles-")
                            && !name.startsWith("uma_")
                            && !name.equals("offline_access"))
                    .collect(Collectors.toList());

        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                "Error obteniendo roles del usuario '" + keycloakId + "': " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private String getUserKeycloakId(String username, String token, String baseUrl) {
        List<?> users;
        try {
            users = webClient.get()
                    .uri(baseUrl + "/users?username=" + username + "&exact=true")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .map(errorBody -> new RuntimeException(
                                "Error buscando usuario '" + username + "' en Keycloak. Status: "
                                + clientResponse.statusCode() + " | Body: " + errorBody))
                    )
                    .bodyToMono(List.class)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                "Error buscando ID del usuario '" + username + "' en Keycloak: " + ex.getMessage(), ex);
        }

        if (users == null || users.isEmpty()) {
            throw new RuntimeException(
                "Usuario '" + username + "' no encontrado en Keycloak realm '" + realm + "'.");
        }
        return (String) ((Map<?, ?>) users.get(0)).get("id");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getRoleRepresentation(String roleName, String token, String baseUrl) {
        try {
            return (Map<String, Object>) webClient.get()
                    .uri(baseUrl + "/roles/" + roleName)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .map(errorBody -> new RuntimeException(
                                "Rol '" + roleName + "' no encontrado. Status: "
                                + clientResponse.statusCode() + " | Body: " + errorBody))
                    )
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception ex) {
            return null;
        }
    }

    private void assignRoleToUser(String userId, String roleName, String token, String baseUrl) {
        Map<String, Object> role = getRoleRepresentation(roleName, token, baseUrl);
        if (role == null) {
            throw new RuntimeException(
                "El rol '" + roleName + "' no existe en el realm '" + realm + "'. "
                + "Créalo en Keycloak > " + realm + " > Realm roles.");
        }
        try {
            webClient.post()
                    .uri(baseUrl + "/users/" + userId + "/role-mappings/realm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(List.of(role))
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .map(errorBody -> new RuntimeException(
                                "Error asignando rol '" + roleName + "' al usuario '" + userId
                                + "'. Status: " + clientResponse.statusCode()
                                + " | Body: " + errorBody))
                    )
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                "Error asignando rol '" + roleName + "': " + ex.getMessage(), ex);
        }
    }
}