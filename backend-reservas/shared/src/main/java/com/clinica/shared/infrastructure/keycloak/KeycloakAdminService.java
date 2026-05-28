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
 *
 * CAMBIOS respecto a la versión original (solo tenía createUser):
 *  - listUsersWithRoles()           NUEVO: lista todos los usuarios del realm
 *                                   con sus roles, para el panel de gestión.
 *  - getUserRoles(keycloakId)       NUEVO: retorna los roles de un usuario por UUID.
 *  - getUserKeycloakId(username)    NUEVO: expuesto como public para UserController.
 *  - updateUserRolesByKeycloakId()  NUEVO: reemplaza todos los roles de un usuario.
 *
 * Los métodos originales (createUser, getAdminToken) NO se modificaron.
 */
@Service
public class KeycloakAdminService {

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
    // Obtener token de administrador (sin cambios)
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
    // Crear usuario en Keycloak y asignar rol (sin cambios)
    // =========================================================

    /**
     * Crea un usuario en Keycloak y le asigna el rol indicado.
     * (Flujo original — no se modificó.)
     */
    public void createUser(String username, String password, String email,
                           String firstName, String lastName, String roleName) {

        String token   = getAdminToken();
        String baseUrl = serverUrl + "/admin/realms/" + realm;

        Map<String, Object> userRepresentation = new HashMap<>();
        userRepresentation.put("username",  username);
        userRepresentation.put("firstName", firstName);
        userRepresentation.put("lastName",  lastName);
        userRepresentation.put("enabled",   true);
        userRepresentation.put("credentials", List.of(Map.of(
                "type",      "password",
                "value",     password,
                "temporary", false
        )));

        if (email != null && !email.isBlank()) {
            userRepresentation.put("email",         email);
            userRepresentation.put("emailVerified", true);
        }

        try {
            webClient.post()
                    .uri(baseUrl + "/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userRepresentation)
                    .retrieve()
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
        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                "Error al crear usuario '" + username + "' en Keycloak: " + ex.getMessage(), ex);
        }

        String userId = getUserKeycloakId(username, token, baseUrl);
        assignRoleToUser(userId, roleName, token, baseUrl);
    }

    // =========================================================
    // NUEVO: Listar usuarios del realm con sus roles
    // =========================================================

    /**
     * Retorna todos los usuarios del realm con sus roles realm asignados.
     *
     * Cada elemento del Map contiene los campos estándar de Keycloak
     * (id, username, firstName, lastName, email, enabled, …)
     * más la clave "realmRoles" con la lista de nombres de roles.
     *
     * Se excluyen roles internos de Keycloak:
     * "default-roles-*", "uma_authorization", "offline_access".
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listUsersWithRoles() {
        String token   = getAdminToken();
        String baseUrl = serverUrl + "/admin/realms/" + realm;

        // 1. Obtener todos los usuarios del realm (máximo 200)
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

        // 2. Para cada usuario, obtener sus roles realm y agregarlos al mapa
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> user : users) {
            String userId      = (String) user.get("id");
            List<String> roles = getUserRoles(userId, token, baseUrl);

            Map<String, Object> enriched = new HashMap<>(user);
            enriched.put("realmRoles", roles); // clave adicional con los roles
            result.add(enriched);
        }
        return result;
    }

    // =========================================================
    // NUEVO: Obtener roles de un usuario por keycloakId (public)
    // =========================================================

    /**
     * Retorna los nombres de los roles realm del usuario identificado
     * por su UUID de Keycloak.
     *
     * Se filtran los roles internos (default-roles-*, uma_*, offline_access).
     */
    public List<String> getUserRoles(String keycloakId) {
        String token   = getAdminToken();
        String baseUrl = serverUrl + "/admin/realms/" + realm;
        return getUserRoles(keycloakId, token, baseUrl);
    }

    // =========================================================
    // NUEVO: Obtener el keycloakId de un usuario por username (public)
    // =========================================================

    /**
     * Busca un usuario por username en el realm y retorna su UUID de Keycloak.
     * Lanza RuntimeException si el usuario no existe.
     */
    public String getUserKeycloakId(String username) {
        String token   = getAdminToken();
        String baseUrl = serverUrl + "/admin/realms/" + realm;
        return getUserKeycloakId(username, token, baseUrl);
    }

    // =========================================================
    // NUEVO: Reemplazar roles de un usuario por keycloakId
    // =========================================================

    /**
     * Reemplaza TODOS los roles realm del usuario identificado por su UUID.
     *
     * Proceso:
     *   1. Obtener los roles actuales del usuario.
     *   2. Eliminar todos los roles actuales.
     *   3. Asignar los nuevos roles de la lista recibida.
     *
     * @param keycloakId UUID del usuario en Keycloak
     * @param newRoles   Lista de nombres de roles a asignar (ej: ["ADMIN","DOCTOR"])
     */
    public void updateUserRolesByKeycloakId(String keycloakId, List<String> newRoles) {
        if (newRoles == null || newRoles.isEmpty()) {
            throw new IllegalArgumentException("El usuario debe tener al menos un rol.");
        }

        String token   = getAdminToken();
        String baseUrl = serverUrl + "/admin/realms/" + realm;

        // Paso 1: obtener roles actuales para poder eliminarlos
        List<String> currentRoleNames = getUserRoles(keycloakId, token, baseUrl);

        if (!currentRoleNames.isEmpty()) {
            // Convertir los nombres a representaciones de rol que Keycloak espera
            List<Map<String, Object>> currentRoleReps = currentRoleNames.stream()
                    .map(roleName -> getRoleRepresentation(roleName, token, baseUrl))
                    .filter(r -> r != null)
                    .collect(Collectors.toList());

            if (!currentRoleReps.isEmpty()) {
                // Paso 2: eliminar todos los roles actuales con DELETE
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

        // Paso 3: asignar los nuevos roles uno a uno
        for (String roleName : newRoles) {
            assignRoleToUser(keycloakId, roleName, token, baseUrl);
        }
    }

    // =========================================================
    // Métodos privados de soporte
    // =========================================================

    /**
     * Versión privada de getUserRoles que reutiliza token y baseUrl
     * ya obtenidos para evitar llamadas extra a Keycloak.
     */
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

            // Filtrar roles internos de Keycloak que no son de negocio
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

    /**
     * Versión privada de getUserKeycloakId reutiliza token y baseUrl.
     */
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

    /**
     * Obtiene la representación JSON completa de un rol por nombre.
     * Retorna null si el rol no existe (no lanza excepción).
     */
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
            // Si el rol no existe en el realm, lo ignoramos
            return null;
        }
    }

    /**
     * Asigna un rol al usuario. Obtiene primero la representación del rol
     * y luego hace POST al endpoint de role-mappings.
     */
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