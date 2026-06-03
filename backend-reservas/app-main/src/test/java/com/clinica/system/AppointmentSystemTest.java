package com.clinica.system;

/**
 * PRUEBAS DE SISTEMA
 * ==================
 * Prueban flujos COMPLETOS encadenados como lo haría el usuario real:
 *   registro → franjas → crear cita → cancelar cita
 *
 * Diferencia con integración: aquí se encadenan MÚLTIPLES pasos.
 * BusinessRuleException → 409 CONFLICT (ver GlobalExceptionHandler)
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
class AppointmentSystemTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private DoctorScheduleRepository doctorScheduleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockBean private KeycloakAdminService keycloakAdminService;

    private Long doctorId;
    private static final String TOKEN = "Bearer test-token";

    @BeforeEach
    void prepararEntorno() {
        doNothing().when(keycloakAdminService)
                .createUser(any(), any(), any(), any(), any(), any());
        doNothing().when(keycloakAdminService)
                .updatePassword(any(), any());

        // Médico con Consulta General
        Doctor doctor = new Doctor();
        doctor.setIdentification("MED-ST-001");
        doctor.setFirstName("Ana");
        doctor.setLastName("Medica");
        doctor.setEmail("ana@clinica.com");
        doctor.setPhone("3010000001");
        doctor.setSpecialty("Consulta General");
        doctorId = doctorRepository.save(doctor).getId();

        // Horario: lunes–viernes 08:00–17:00, cada 30 min
        DoctorSchedule h = new DoctorSchedule();
        h.setDoctorId(doctorId);
        h.setWorkingDays(Set.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
        h.setStartTime(LocalTime.of(8, 0));
        h.setEndTime(LocalTime.of(17, 0));
        h.setIntervalMinutes(30);
        doctorScheduleRepository.save(h);
    }

    // ------------------------------------------------------------------
    // ST-01: Registro → franjas → crear cita (flujo completo)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("ST-01: Paciente se registra → consulta franjas → agenda cita")
    void ST01_registrarYAgendarCita() throws Exception {

        // PASO 1: Registrar paciente
        Map<String, Object> p = new HashMap<>();
        p.put("identification", "ST01001");
        p.put("firstName",      "Juan");
        p.put("lastName",       "Sistema");
        p.put("phone",          "3011112222");
        p.put("gender",         "Masculino");
        p.put("email",          "juan@test.com");
        p.put("username",       "ST01001");
        p.put("password",       "clave1234");

        MvcResult regRes = mockMvc.perform(
                post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer patientId = (Integer) objectMapper
                .readValue(regRes.getResponse().getContentAsString(), Map.class).get("id");
        assertNotNull(patientId, "El paciente debe tener ID");

        // PASO 2: Ver franjas disponibles
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        MvcResult franjasRes = mockMvc.perform(
                get("/api/v1/appointments/slots")
                        .param("doctorId", doctorId.toString())
                        .param("date", lunes.toString())
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andReturn();

        List<?> franjas = objectMapper.readValue(
                franjasRes.getResponse().getContentAsString(), List.class);
        assertFalse(franjas.isEmpty(), "Debe haber franjas el lunes");

        Map<?, ?> primera = (Map<?, ?>) franjas.get(0);
        assertTrue((Boolean) primera.get("available"), "La primera franja debe estar libre");

        // PASO 3: Crear cita
        Map<String, Object> cita = new HashMap<>();
        cita.put("doctorId",  doctorId);
        cita.put("patientId", patientId);
        cita.put("date",      lunes.toString());
        cita.put("startTime", primera.get("startTime"));
        cita.put("notes",     "Cita ST-01");

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita))
                        .header("Authorization", TOKEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.patientName").value("Juan Sistema"))
                .andExpect(jsonPath("$.doctorName").value("Ana Medica"));
    }

    // ------------------------------------------------------------------
    // ST-02: Crear cita → cancelarla
    // ------------------------------------------------------------------
    @Test
    @DisplayName("ST-02: Cita creada → se cancela → queda CANCELLED")
    void ST02_crearYCancelarCita() throws Exception {

        Long patientId = crearPaciente("ST02001", "Pedro", "Cancel");
        LocalDate martes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY));

        Map<String, Object> cita = Map.of(
                "doctorId", doctorId, "patientId", patientId,
                "date", martes.toString(), "startTime", "09:00", "notes", "a cancelar"
        );

        MvcResult cr = mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita))
                        .header("Authorization", TOKEN))
                .andExpect(status().isCreated())
                .andReturn();

        Integer citaId = (Integer) objectMapper
                .readValue(cr.getResponse().getContentAsString(), Map.class).get("id");

        // Cancelar
        mockMvc.perform(patch("/api/v1/appointments/" + citaId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"No puedo asistir\"}")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ------------------------------------------------------------------
    // ST-03: Franja ocupada → segunda cita rechazada con 409
    // BusinessRuleException → HTTP 409 CONFLICT (GlobalExceptionHandler)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("ST-03: Franja ocupada → segunda cita rechazada con 409")
    void ST03_franjaOcupada_retorna409() throws Exception {

        Long p1 = crearPaciente("ST03A01", "Uno",  "Ocupado");
        Long p2 = crearPaciente("ST03B02", "Dos",  "Espera");
        LocalDate miercoles = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));

        // Cita 1 → debe crearse
        Map<String, Object> c1 = Map.of("doctorId", doctorId, "patientId", p1,
                "date", miercoles.toString(), "startTime", "10:00", "notes", "c1");
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c1))
                        .header("Authorization", TOKEN))
                .andExpect(status().isCreated());

        // Cita 2 misma franja → debe rechazarse
        Map<String, Object> c2 = Map.of("doctorId", doctorId, "patientId", p2,
                "date", miercoles.toString(), "startTime", "10:00", "notes", "c2");
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c2))
                        .header("Authorization", TOKEN))
                // BusinessRuleException → 409 CONFLICT
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------
    // ST-04: Paciente con cita SCHEDULED no puede agendar otra → 409
    // ------------------------------------------------------------------
    @Test
    @DisplayName("ST-04: Paciente con cita activa no puede agendar otra → 409")
    void ST04_pacienteConCitaActiva_rechazado() throws Exception {

        Long patientId = crearPaciente("ST04001", "Ana", "DosCitas");
        LocalDate jueves = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY));
        LocalDate viernes = jueves.plusDays(1);

        // Primera cita
        Map<String, Object> c1 = Map.of("doctorId", doctorId, "patientId", patientId,
                "date", jueves.toString(), "startTime", "08:00", "notes", "primera");
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c1))
                        .header("Authorization", TOKEN))
                .andExpect(status().isCreated());

        // Segunda cita → debe rechazarse porque ya tiene una activa
        Map<String, Object> c2 = Map.of("doctorId", doctorId, "patientId", patientId,
                "date", viernes.toString(), "startTime", "08:00", "notes", "segunda");
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c2))
                        .header("Authorization", TOKEN))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------
    // ST-05: Listar citas del paciente
    // ------------------------------------------------------------------
    @Test
    @DisplayName("ST-05: Paciente puede ver su lista de citas")
    void ST05_listarCitasDelPaciente() throws Exception {

        Long patientId = crearPaciente("ST05001", "Rosa", "Citas");
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        Map<String, Object> cita = Map.of("doctorId", doctorId, "patientId", patientId,
                "date", lunes.toString(), "startTime", "11:00", "notes", "mi cita");
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita))
                        .header("Authorization", TOKEN))
                .andExpect(status().isCreated());

        MvcResult lista = mockMvc.perform(
                get("/api/v1/appointments")
                        .param("patientId", patientId.toString())
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andReturn();

        List<?> citas = objectMapper.readValue(
                lista.getResponse().getContentAsString(), List.class);
        assertEquals(1, citas.size(), "Debe haber exactamente 1 cita");
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------
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
