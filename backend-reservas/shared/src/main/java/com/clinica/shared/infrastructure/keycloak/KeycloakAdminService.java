package com.clinica.shared.infrastructure.keycloak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Servicio que interactúa con la API Admin de Keycloak para crear
 * usuarios automáticamente cuando el administrador registra médicos
 * o agendadores desde el panel.
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

    /**
     * Obtiene un token de acceso de administrador desde Keycloak.
     */
    private String getAdminToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("username", adminUsername);
        body.add("password", adminPassword);
        body.add("grant_type", "password");

        Map<?, ?> response = webClient.post()
                .uri(serverUrl + "/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(body))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("access_token")) {
            throw new RuntimeException("No se pudo obtener el token de administrador de Keycloak");
        }
        return (String) response.get("access_token");
    }

    /**
     * Crea un usuario en Keycloak con el rol indicado.
     *
     * @param username nombre de usuario
     * @param password contraseña en texto plano
     * @param email    correo electrónico
     * @param firstName nombre
     * @param lastName  apellido
     * @param roleName  rol a asignar: "DOCTOR", "SCHEDULER", "PATIENT", etc.
     */
    public void createUser(String username, String password, String email,
                           String firstName, String lastName, String roleName) {
        String token = getAdminToken();
        String baseUrl = serverUrl + "/admin/realms/" + realm;

        // 1. Crear el usuario
        Map<String, Object> userRepresentation = Map.of(
                "username", username,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false
                ))
        );

        webClient.post()
                .uri(baseUrl + "/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userRepresentation)
                .retrieve()
                .toBodilessEntity()
                .block();

        // 2. Buscar el ID del usuario recién creado
        List<?> users = webClient.get()
                .uri(baseUrl + "/users?username=" + username + "&exact=true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (users == null || users.isEmpty()) {
            throw new RuntimeException("Usuario creado en Keycloak pero no se pudo recuperar su ID");
        }

        String userId = (String) ((Map<?, ?>) users.get(0)).get("id");

        // 3. Buscar el rol del realm por nombre
        Map<?, ?> role = webClient.get()
                .uri(baseUrl + "/roles/" + roleName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (role == null) {
            throw new RuntimeException("Rol no encontrado en Keycloak: " + roleName);
        }

        // 4. Asignar el rol al usuario
        webClient.post()
                .uri(baseUrl + "/users/" + userId + "/role-mappings/realm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(role))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}