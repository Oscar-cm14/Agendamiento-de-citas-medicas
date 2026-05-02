package com.clinica.appointments.application.services;

import com.clinica.appointments.domain.entities.Appointment;
import com.clinica.appointments.infrastructure.repositories.AppointmentRepository;
import com.clinica.doctors.domain.entities.Doctor;
import com.clinica.doctors.infrastructure.repositories.DoctorRepository;
import com.clinica.shared.domain.entities.AppointmentStatus;
import com.clinica.shared.dto.AppointmentRequest;
import com.clinica.shared.dto.AppointmentResponse;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas Unitarias para AppointmentServiceImpl.
 * Validando el 20% de Pruebas Unitarias automatizadas.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private AppointmentRequest validRequest;
    private Doctor mockDoctor;
    private Patient mockPatient;

    @BeforeEach
    void setUp() {
        validRequest = new AppointmentRequest(
                1L,
                2L,
                LocalDate.of(2026, 3, 22),
                LocalTime.of(10, 0),
                "Reserva de control"
        );

        mockDoctor = new Doctor();
        mockDoctor.setId(1L);
        mockDoctor.setFirstName("Juan");
        mockDoctor.setLastName("Pérez");
        mockDoctor.setSpecialty("Cardiología");

        mockPatient = new Patient();
        mockPatient.setId(2L);
        mockPatient.setFirstName("María");
        mockPatient.setLastName("Gómez");
    }

    @Test
    @DisplayName("Happy Path: Crea una cita exitosamente cuando hay disponibilidad")
    void createAppointment_Success() {
        // Arrange
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));
        when(appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                1L, LocalDate.of(2026, 3, 22), LocalTime.of(10, 0))).thenReturn(false);

        Appointment savedAppointment = new Appointment();
        savedAppointment.setId(100L);
        savedAppointment.setDoctorId(1L);
        savedAppointment.setPatientId(2L);
        savedAppointment.setDate(LocalDate.of(2026, 3, 22));
        savedAppointment.setStartTime(LocalTime.of(10, 0));
        savedAppointment.setEndTime(LocalTime.of(10, 30));
        savedAppointment.setStatus(AppointmentStatus.SCHEDULED);
        savedAppointment.setNotes("Reserva de control");

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        // Act
        AppointmentResponse response = appointmentService.createAppointment(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Juan Pérez", response.doctorName());
        assertEquals("María Gómez", response.patientName());
        assertEquals("Cardiología", response.specialty());
        assertEquals(AppointmentStatus.SCHEDULED, response.status());

        // Verificar que el repositorio fue llamado exactamente una vez
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Edge Case/Error: Falla al agendar si el paciente no existe")
    void createAppointment_PatientNotFound() {
        // Arrange
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(patientRepository.findById(2L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            appointmentService.createAppointment(validRequest);
        });

        assertEquals("Paciente no encontrado", exception.getMessage());
        
        // Verificar que NUNCA se intentó guardar en base de datos
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Edge Case/Error: Falla al agendar si la franja ya está ocupada")
    void createAppointment_SlotOccupied() {
        // Arrange
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));
        when(appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                1L, LocalDate.of(2026, 3, 22), LocalTime.of(10, 0))).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            appointmentService.createAppointment(validRequest);
        });

        assertEquals("Ya existe una cita en esa franja horaria", exception.getMessage());
        
        // Verificar que no se guardó la cita en bd
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }
}
