package com.clinica.integration;

/**
 * PRUEBAS DE INTEGRACIÓN
 * ======================
 * Verifican que Controller + Service + Repositorio funcionen JUNTOS
 * usando la BD H2 real en memoria. Solo se mockea Keycloak Admin.
 *
 * Cada test:
 *   - Arranca con BD limpia (@Transactional revierte al terminar)
 *   - Envía una petición HTTP real con MockMvc
 *   - Verifica el código HTTP y el JSON de respuesta
 */

import com.clinica.config.TestSecurityConfig;
import com.clinica.shared.infrastructure.keycloak.KeycloakAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
class PatientIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Solo mockeamos Keycloak Admin (no necesitamos servidor real)
    @MockBean
    private KeycloakAdminService keycloakAdminService;

    // Token ficticio: TestSecurityConfig lo acepta sin validar
    private static final String TOKEN = "Bearer test-token-valido";

    @BeforeEach
    void configurar() {
        // Keycloak no hace llamadas reales en los tests
        doNothing().when(keycloakAdminService)
                .createUser(any(), any(), any(), any(), any(), any());
        doNothing().when(keycloakAdminService)
                .updatePassword(any(), any());
    }

    // ------------------------------------------------------------------
    // IT-01: Registro exitoso de paciente nuevo → 201
    // ------------------------------------------------------------------
    @Test
    @DisplayName("IT-01: POST /patients/register → 201 con datos correctos")
    void IT01_registrarPacienteNuevo_retorna201() throws Exception {

        Map<String, Object> datos = new HashMap<>();
        datos.put("identification", "10001001");
        datos.put("firstName",      "Laura");
        datos.put("lastName",       "Gomez");
        datos.put("phone",          "3001112222");
        datos.put("gender",         "Femenino");
        datos.put("email",          "laura@test.com");
        datos.put("username",       "10001001");
        datos.put("password",       "clave1234");
        datos.put("birthDate",      "1998-03-20");

        mockMvc.perform(post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(datos)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Laura Gomez"))
                .andExpect(jsonPath("$.username").value("10001001"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    // ------------------------------------------------------------------
    // IT-02: Cédula duplicada → devuelve el paciente existente (no falla)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("IT-02: POST /patients/register con cédula duplicada → devuelve existente")
    void IT02_registrarCedulaDuplicada_retornaExistente() throws Exception {

        Map<String, Object> datos = new HashMap<>();
        datos.put("identification", "20002002");
        datos.put("firstName",      "Mario");
        datos.put("lastName",       "Ruiz");
        datos.put("phone",          "3002223333");
        datos.put("gender",         "Masculino");
        datos.put("email",          "mario@test.com");
        datos.put("username",       "20002002");
        datos.put("password",       "clave1234");

        // Primer registro
        mockMvc.perform(post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(datos)))
                .andExpect(status().isCreated());

        // Segundo con misma cédula → no falla, devuelve el existente
        mockMvc.perform(post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(datos)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Mario Ruiz"));
    }

    // ------------------------------------------------------------------
    // IT-03: Buscar paciente por cédula → 200 con sus datos
    // ------------------------------------------------------------------
    @Test
    @DisplayName("IT-03: GET /patients/by-identification → 200 con datos del paciente")
    void IT03_buscarPorCedula_retornaDatos() throws Exception {

        // Registrar primero
        Map<String, Object> datos = new HashMap<>();
        datos.put("identification", "30003003");
        datos.put("firstName",      "Sofia");
        datos.put("lastName",       "Torres");
        datos.put("phone",          "3003334444");
        datos.put("gender",         "Femenino");
        datos.put("email",          "sofia@test.com");
        datos.put("username",       "30003003");
        datos.put("password",       "clave1234");

        mockMvc.perform(post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(datos)))
                .andExpect(status().isCreated());

        // Buscar por cédula
        mockMvc.perform(get("/api/v1/patients/by-identification")
                        .param("identification", "30003003")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identification").value("30003003"))
                .andExpect(jsonPath("$.firstName").value("Sofia"))
                .andExpect(jsonPath("$.lastName").value("Torres"));
    }

    // ------------------------------------------------------------------
    // IT-04: Buscar paciente por username → 200
    // ------------------------------------------------------------------
    @Test
    @DisplayName("IT-04: GET /patients/by-username → 200 con datos del paciente")
    void IT04_buscarPorUsername_retornaDatos() throws Exception {

        Map<String, Object> datos = new HashMap<>();
        datos.put("identification", "40004004");
        datos.put("firstName",      "Carlos");
        datos.put("lastName",       "Mora");
        datos.put("phone",          "3004445555");
        datos.put("gender",         "Masculino");
        datos.put("email",          "carlos@test.com");
        datos.put("username",       "40004004");
        datos.put("password",       "clave1234");

        mockMvc.perform(post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(datos)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/patients/by-username")
                        .param("username", "40004004")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identification").value("40004004"))
                .andExpect(jsonPath("$.username").value("40004004"));
    }

    // ------------------------------------------------------------------
    // IT-05: Paciente inexistente → 404
    // ------------------------------------------------------------------
    @Test
    @DisplayName("IT-05: GET /patients/by-username inexistente → 404")
    void IT05_buscarPacienteInexistente_retorna404() throws Exception {

        mockMvc.perform(get("/api/v1/patients/by-username")
                        .param("username", "noexiste9999")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // IT-06: Registro sin campos obligatorios → 400
    // ------------------------------------------------------------------
    @Test
    @DisplayName("IT-06: POST /patients/register sin datos → 400 Bad Request")
    void IT06_registrarSinDatos_retorna400() throws Exception {

        Map<String, Object> incompleto = Map.of("firstName", "Solo");

        mockMvc.perform(post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incompleto)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // IT-07: Endpoint protegido sin token → 401
    // ------------------------------------------------------------------
    @Test
    @DisplayName("IT-07: GET /patients/by-username sin token → 401 Unauthorized")
    void IT07_sinToken_retorna401() throws Exception {

        mockMvc.perform(get("/api/v1/patients/by-username")
                        .param("username", "alguien"))
                .andExpect(status().isUnauthorized());
    }
}
