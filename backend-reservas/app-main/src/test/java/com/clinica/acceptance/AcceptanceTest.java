package com.clinica.acceptance;

/**
 * PRUEBAS DE ACEPTACIÓN
 * =====================
 * Verifican los REQUISITOS FUNCIONALES desde la perspectiva del usuario.
 * Lenguaje DADO-CUANDO-ENTONCES (Given-When-Then).
 *
 * RF3 — Paciente puede registrarse desde la web
 * RF4 — Sistema rechaza datos inválidos
 * RF5 — Paciente puede agendar cita con médico disponible
 * RF6 — Paciente puede cancelar una cita programada
 * RF7 — Paciente puede ver sus citas
 * RF8 — Especialidades requieren consulta general previa
 *
 * Nota: BusinessRuleException → 409 CONFLICT (GlobalExceptionHandler)
 */

import com.clinica.config.TestSecurityConfig;
import com.clinica.doctors.domain.entities.Doctor;
import com.clinica.doctors.domain.entities.DoctorSchedule;
import com.clinica.doctors.infrastructure.repositories.DoctorRepository;
import com.clinica.doctors.infrastructure.repositories.DoctorScheduleRepository;
import com.clinica.shared.domain.UserRole;
import com.clinica.shared.infrastructure.keycloak.KeycloakAdminService;
import com.clinica.users.domain.entities.Patient;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
class AcceptanceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private DoctorScheduleRepository doctorScheduleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockBean private KeycloakAdminService keycloakAdminService;

    private Long doctorGeneralId;
    private Long doctorEspecialistaId;
    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void configurarEntorno() {
        doNothing().when(keycloakAdminService)
                .createUser(any(), any(), any(), any(), any(), any());
        doNothing().when(keycloakAdminService)
                .updatePassword(any(), any());

        // Médico 1: Consulta General
        Doctor general = new Doctor();
        general.setIdentification("DOC-GEN");
        general.setFirstName("Doctor");
        general.setLastName("General");
        general.setSpecialty("Consulta General");
        general.setPhone("3020000001");
        general.setEmail("general@clinica.com");
        doctorGeneralId = doctorRepository.save(general).getId();

        // Médico 2: Fisioterapia (requiere consulta general previa)
        Doctor especialista = new Doctor();
        especialista.setIdentification("DOC-ESP");
        especialista.setFirstName("Doctor");
        especialista.setLastName("Especialista");
        especialista.setSpecialty("Fisioterapia");
        especialista.setPhone("3020000002");
        especialista.setEmail("especialista@clinica.com");
        doctorEspecialistaId = doctorRepository.save(especialista).getId();

        // Horario para ambos médicos
        for (Long docId : List.of(doctorGeneralId, doctorEspecialistaId)) {
            DoctorSchedule h = new DoctorSchedule();
            h.setDoctorId(docId);
            h.setWorkingDays(Set.of(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
            h.setStartTime(LocalTime.of(8, 0));
            h.setEndTime(LocalTime.of(17, 0));
            h.setIntervalMinutes(30);
            doctorScheduleRepository.save(h);
        }
    }

    // =================================================================
    // RF3 — Registro de paciente
    // =================================================================

    @Test
    @DisplayName("AT-RF3-01: DADO paciente nuevo CUANDO se registra ENTONCES recibe ID y nombre completo")
    void RF3_01_pacienteNuevoPuedeRegistrarse() throws Exception {
        // DADO: paciente que no existe
        Map<String, Object> datos = Map.of(
                "identification", "RF3001",
                "firstName",      "Maria",
                "lastName",       "Prueba",
                "phone",          "3030001111",
                "gender",         "Femenino",
                "email",          "maria@test.com",
                "username",       "RF3001",
                "password",       "clave1234"
        );

        // CUANDO: envía el formulario de registro
        MvcResult res = mockMvc.perform(
                        post("/api/v1/patients/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(datos)))
                // ENTONCES: sistema confirma con 201
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Maria Prueba"))
                .andReturn();

        // Y el paciente tiene un ID positivo
        Map<?, ?> resp = objectMapper.readValue(
                res.getResponse().getContentAsString(), Map.class);
        assertNotNull(resp.get("id"), "RF3: Debe tener ID");
        assertTrue((Integer) resp.get("id") > 0, "RF3: ID debe ser positivo");
    }

    // =================================================================
    // RF4 — Validación de datos
    // =================================================================

    @Test
    @DisplayName("AT-RF4-01: DADO datos incompletos CUANDO se intenta registrar ENTONCES sistema rechaza 400")
    void RF4_01_datosIncompletosRechazados() throws Exception {
        // DADO: solo se envía el nombre, faltan todos los campos obligatorios
        Map<String, Object> incompleto = Map.of("firstName", "Solo");

        // CUANDO + ENTONCES: 400 Bad Request
        mockMvc.perform(post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incompleto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("AT-RF4-02: DADO username en uso CUANDO otro lo intenta ENTONCES sistema rechaza")
    void RF4_02_usernameYaExisteRechazado() throws Exception {
        // DADO: paciente ya registrado con username "userdup01"
        Map<String, Object> primero = new HashMap<>();
        primero.put("identification", "RF4A01");
        primero.put("firstName",      "Primero");
        primero.put("lastName",       "Registrado");
        primero.put("phone",          "3030002222");
        primero.put("gender",         "Masculino");
        primero.put("email",          "primero@test.com");
        primero.put("username",       "userdup01");
        primero.put("password",       "clave1234");

        mockMvc.perform(post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(primero)))
                .andExpect(status().isCreated());

        // CUANDO: otro paciente (distinta cédula) usa el mismo username
        Map<String, Object> segundo = new HashMap<>();
        segundo.put("identification", "RF4B02"); // diferente cédula
        segundo.put("firstName",      "Segundo");
        segundo.put("lastName",       "Intento");
        segundo.put("phone",          "3030003333");
        segundo.put("gender",         "Femenino");
        segundo.put("email",          "segundo@test.com");
        segundo.put("username",       "userdup01"); // mismo username → debe fallar
        segundo.put("password",       "otraClave");

        // ENTONCES: sistema rechaza (400, 409 o 500 según la implementación)
        mockMvc.perform(post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(segundo)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status >= 400,
                            "RF4: Debe rechazar username duplicado. Status: " + status);
                });
    }

    // =================================================================
    // RF5 — Agendamiento de cita
    // =================================================================

    @Test
    @DisplayName("AT-RF5-01: DADO paciente sin citas CUANDO agenda Consulta General ENTONCES cita SCHEDULED")
    void RF5_01_agendarConsultaGeneral() throws Exception {
        // DADO: paciente sin historial
        Long patientId = crearPaciente("RF5001", "Luis", "Agenda");
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        // CUANDO: agenda Consulta General
        Map<String, Object> cita = Map.of(
                "doctorId", doctorGeneralId, "patientId", patientId,
                "date", lunes.toString(), "startTime", "08:00", "notes", "primera consulta"
        );

        // ENTONCES: cita queda SCHEDULED
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita))
                        .header("Authorization", TOKEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.specialty").value("Consulta General"))
                .andExpect(jsonPath("$.patientName").value("Luis Agenda"));
    }

    @Test
    @DisplayName("AT-RF5-02: DADO franja ocupada CUANDO otro paciente la pide ENTONCES sistema rechaza 409")
    void RF5_02_franjaOcupada_rechazada() throws Exception {
        // DADO: franja ya ocupada por el paciente 1
        Long p1 = crearPaciente("RF5P1", "Uno",  "Primero");
        Long p2 = crearPaciente("RF5P2", "Dos",  "Segundo");
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        Map<String, Object> c1 = Map.of("doctorId", doctorGeneralId, "patientId", p1,
                "date", lunes.toString(), "startTime", "09:00", "notes", "c1");
        mockMvc.perform(post("/api/v1/appointments").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c1)).header("Authorization", TOKEN))
                .andExpect(status().isCreated());

        // CUANDO: paciente 2 pide la misma franja
        Map<String, Object> c2 = Map.of("doctorId", doctorGeneralId, "patientId", p2,
                "date", lunes.toString(), "startTime", "09:00", "notes", "c2");

        // ENTONCES: sistema rechaza con 409
        mockMvc.perform(post("/api/v1/appointments").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c2)).header("Authorization", TOKEN))
                .andExpect(status().isConflict());
    }

    // =================================================================
    // RF6 — Cancelación de cita
    // =================================================================

    @Test
    @DisplayName("AT-RF6-01: DADO cita SCHEDULED CUANDO se cancela con motivo ENTONCES queda CANCELLED")
    void RF6_01_cancelarCitaConMotivo() throws Exception {
        // DADO: paciente con cita programada
        Long patientId = crearPaciente("RF6001", "Julia", "Cancela");
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        Map<String, Object> cita = Map.of("doctorId", doctorGeneralId, "patientId", patientId,
                "date", lunes.toString(), "startTime", "10:00", "notes", "a cancelar");
        MvcResult cr = mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita))
                        .header("Authorization", TOKEN))
                .andExpect(status().isCreated()).andReturn();

        Integer citaId = (Integer) objectMapper
                .readValue(cr.getResponse().getContentAsString(), Map.class).get("id");

        // CUANDO: paciente cancela con motivo
        // ENTONCES: cita queda CANCELLED
        mockMvc.perform(patch("/api/v1/appointments/" + citaId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"No puedo asistir ese día\"}")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("AT-RF6-02: DADO cita SCHEDULED CUANDO se cancela SIN motivo ENTONCES sistema rechaza")
    void RF6_02_cancelarSinMotivo_rechazado() throws Exception {
        // DADO: cita programada
        Long patientId = crearPaciente("RF6002", "Pedro", "SinMotivo");
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        Map<String, Object> cita = Map.of("doctorId", doctorGeneralId, "patientId", patientId,
                "date", lunes.toString(), "startTime", "11:00", "notes", "cita");
        MvcResult cr = mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita))
                        .header("Authorization", TOKEN))
                .andExpect(status().isCreated()).andReturn();

        Integer citaId = (Integer) objectMapper
                .readValue(cr.getResponse().getContentAsString(), Map.class).get("id");

        // CUANDO: cancela con reason vacío
        // ENTONCES: sistema rechaza (409 porque BusinessRuleException)
        mockMvc.perform(patch("/api/v1/appointments/" + citaId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}")
                        .header("Authorization", TOKEN))
                .andExpect(status().isConflict());
    }

    // =================================================================
    // RF7 — Ver citas del paciente
    // =================================================================

    @Test
    @DisplayName("AT-RF7-01: DADO paciente con 1 cita CUANDO consulta sus citas ENTONCES ve 1 cita SCHEDULED")
    void RF7_01_pacienteVeSusCitas() throws Exception {
        // DADO: 1 cita programada
        Long patientId = crearPaciente("RF7001", "Rosa", "ListaCitas");
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        Map<String, Object> cita = Map.of("doctorId", doctorGeneralId, "patientId", patientId,
                "date", lunes.toString(), "startTime", "08:30", "notes", "mi cita");
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita))
                        .header("Authorization", TOKEN))
                .andExpect(status().isCreated());

        // CUANDO: consulta su lista
        MvcResult r = mockMvc.perform(get("/api/v1/appointments")
                        .param("patientId", patientId.toString())
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk()).andReturn();

        // ENTONCES: ve exactamente 1 cita en estado SCHEDULED
        List<?> citas = objectMapper.readValue(
                r.getResponse().getContentAsString(), List.class);
        assertEquals(1, citas.size(), "RF7: Debe haber 1 cita");
        assertEquals("SCHEDULED", ((Map<?, ?>) citas.get(0)).get("status"),
                "RF7: La cita debe estar programada");
    }

    // =================================================================
    // RF8 — Consulta general previa obligatoria
    // =================================================================

    @Test
    @DisplayName("AT-RF8-01: DADO sin consulta general CUANDO pide especialista ENTONCES sistema rechaza 409")
    void RF8_01_especialistaSinConsultaGeneral_rechazado() throws Exception {
        // DADO: paciente sin ninguna consulta general completada
        Long patientId = crearPaciente("RF8001", "Ana", "SinGeneral");
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        // CUANDO: intenta agendar directamente con el especialista (Fisioterapia)
        Map<String, Object> cita = Map.of(
                "doctorId",  doctorEspecialistaId,
                "patientId", patientId,
                "date",      lunes.toString(),
                "startTime", "08:00",
                "notes",     "quiero fisioterapia directa"
        );

        // ENTONCES: sistema rechaza con 409 (BusinessRuleException)
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita))
                        .header("Authorization", TOKEN))
                .andExpect(status().isConflict());
    }

    // =================================================================
    // Helper
    // =================================================================
    private Long crearPaciente(String cedula, String nombre, String apellido) {
        Patient p = new Patient();
        p.setIdentification(cedula);
        p.setFirstName(nombre);
        p.setLastName(apellido);
        p.setPhone("3000000000");
        p.setGender("Masculino");
        p.setEmail(cedula + "@test.com");

        User u = new User();
        u.setUsername(cedula);
        u.setPassword(passwordEncoder.encode("clave1234"));
        u.setEnabled(true);
        u.setRole(UserRole.PATIENT);
        u.setPerson(p);
        return userRepository.save(u).getPerson().getId();
    }
}
