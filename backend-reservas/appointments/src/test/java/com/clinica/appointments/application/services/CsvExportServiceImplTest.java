package com.clinica.appointments.application.services;


import com.clinica.appointments.domain.entities.Appointment;
import com.clinica.appointments.infrastructure.repositories.AppointmentRepository;
import com.clinica.doctors.domain.entities.Doctor;
import com.clinica.doctors.infrastructure.repositories.DoctorRepository;
import com.clinica.shared.domain.entities.AppointmentStatus;
import com.clinica.users.domain.entities.Patient;
import com.clinica.users.infrastructure.repositories.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * =====================================================================
 * PRUEBAS UNITARIAS - CsvExportServiceImpl
 * Cubre RF5: exportar citas a CSV.
 *
 * Casos cubiertos:
 *   - Happy Path: CSV con datos correctos
 *   - Happy Path: meta-información con total de citas
 *   - Happy Path: CSV vacío cuando no hay citas (solo cabecera)
 *   - Happy Path: escape de comas en nombres de pacientes
 *   - Error Case: médico no encontrado lanza excepción
 *   - Edge Case: paciente no encontrado muestra "Desconocido"
 * =====================================================================
 */
@ExtendWith(MockitoExtension.class)
class CsvExportServiceImplTest {

    // ── Mocks ────────────────────────────────────────────────────────────
    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    // Clase bajo prueba (se inyectan los mocks automáticamente)
    @InjectMocks
    private CsvExportServiceImpl csvExportService;

    // ── Datos de prueba reutilizables ────────────────────────────────────
    private Doctor mockDoctor;
    private Patient mockPatient;
    private Appointment mockAppointment;
    private final LocalDate testDate = LocalDate.of(2026, 5, 10);

    @BeforeEach
    void setUp() {
        // Médico de prueba
        mockDoctor = new Doctor();
        mockDoctor.setId(1L);
        mockDoctor.setFirstName("Carlos");
        mockDoctor.setLastName("Ramírez");
        mockDoctor.setSpecialty("Medicina General");

        // Paciente de prueba
        mockPatient = new Patient();
        mockPatient.setId(2L);
        mockPatient.setFirstName("Ana");
        mockPatient.setLastName("Torres");
        mockPatient.setIdentification("1075123456");
        mockPatient.setPhone("3001234567");
        mockPatient.setGender("Mujer");

        // Cita de prueba
        mockAppointment = new Appointment();
        mockAppointment.setId(10L);
        mockAppointment.setDoctorId(1L);
        mockAppointment.setPatientId(2L);
        mockAppointment.setDate(testDate);
        mockAppointment.setStartTime(LocalTime.of(9, 0));
        mockAppointment.setEndTime(LocalTime.of(9, 30));
        mockAppointment.setStatus(AppointmentStatus.SCHEDULED);
        mockAppointment.setNotes("Control mensual");
    }

    // ── Happy Path Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Happy Path: genera CSV con cabecera y fila de datos correcta")
    void exportAppointmentsToCsv_Success() {
        // Arrange: configurar mocks para retornar datos válidos
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(appointmentRepository.findByDoctorIdAndDate(1L, testDate))
                .thenReturn(List.of(mockAppointment));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));

        // Act
        String csv = csvExportService.exportAppointmentsToCsv(1L, testDate);

        // Assert: verificar que el CSV contiene todos los campos esperados
        assertThat(csv).contains("N°,Paciente,Identificación,Celular,Género,Fecha,Hora Inicio,Hora Fin,Estado,Notas");
        assertThat(csv).contains("Ana Torres");
        assertThat(csv).contains("1075123456");
        assertThat(csv).contains("3001234567");
        assertThat(csv).contains("Mujer");
        assertThat(csv).contains("09:00");
        assertThat(csv).contains("09:30");
        assertThat(csv).contains("SCHEDULED");
        assertThat(csv).contains("Control mensual");
        // Verificar meta-información del médico
        assertThat(csv).contains("Carlos Ramírez");
        assertThat(csv).contains("Medicina General");
    }

    @Test
    @DisplayName("Happy Path: la meta-información incluye el total de citas")
    void exportAppointmentsToCsv_TotalCount() {
        // Arrange
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(appointmentRepository.findByDoctorIdAndDate(1L, testDate))
                .thenReturn(List.of(mockAppointment));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));

        // Act
        String csv = csvExportService.exportAppointmentsToCsv(1L, testDate);

        // Assert
        assertThat(csv).contains("Total de citas: 1");
    }

    @Test
    @DisplayName("Happy Path: CSV vacío (solo cabecera) cuando el médico no tiene citas ese día")
    void exportAppointmentsToCsv_NoCitas_OnlyHeader() {
        // Arrange: el médico existe pero no tiene citas ese día
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(appointmentRepository.findByDoctorIdAndDate(1L, testDate))
                .thenReturn(Collections.emptyList());

        // Act
        String csv = csvExportService.exportAppointmentsToCsv(1L, testDate);

        // Assert: debe tener cabecera pero cero filas de datos
        assertThat(csv).contains("N°,Paciente,Identificación");
        assertThat(csv).contains("Total de citas: 0");
        // Contar líneas de datos (excluir # y cabecera)
        long dataLines = Arrays.stream(csv.split("\n"))
                .filter(l -> !l.startsWith("#") && !l.startsWith("N°"))
                .count();
        assertThat(dataLines).isEqualTo(0);
    }

    @Test
    @DisplayName("Happy Path: escapa correctamente los campos que contienen coma")
    void exportAppointmentsToCsv_EscapesCommaInName() {
        // Arrange: paciente con coma en el nombre (caso real: "García, Jr.")
        mockPatient.setFirstName("Juan, Jr.");
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(appointmentRepository.findByDoctorIdAndDate(1L, testDate))
                .thenReturn(List.of(mockAppointment));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));

        // Act
        String csv = csvExportService.exportAppointmentsToCsv(1L, testDate);

        // Assert: el campo con coma debe estar envuelto en comillas dobles
        assertThat(csv).contains("\"Juan, Jr. Torres\"");
    }

    // ── Error / Edge Case Tests ──────────────────────────────────────────

    @Test
    @DisplayName("Error Case: lanza excepción si el médico no existe")
    void exportAppointmentsToCsv_DoctorNotFound() {
        // Arrange: médico inexistente
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert: debe lanzar RuntimeException con mensaje descriptivo
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> csvExportService.exportAppointmentsToCsv(99L, testDate));

        assertThat(ex.getMessage()).contains("Médico no encontrado");
    }

    @Test
    @DisplayName("Edge Case: si el paciente fue eliminado, muestra 'Desconocido' en el CSV")
    void exportAppointmentsToCsv_PatientNotFound_ShowsUnknown() {
        // Arrange: la cita existe pero el paciente ya no está en BD
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(appointmentRepository.findByDoctorIdAndDate(1L, testDate))
                .thenReturn(List.of(mockAppointment));
        when(patientRepository.findById(2L)).thenReturn(Optional.empty());

        // Act
        String csv = csvExportService.exportAppointmentsToCsv(1L, testDate);

        // Assert: no debe romper, debe mostrar "Desconocido"
        assertThat(csv).contains("Desconocido");
    }
}