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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.clinica.appointments.domain.entities.AppointmentHistory;
import com.clinica.appointments.infrastructure.repositories.AppointmentHistoryRepository;
import com.clinica.shared.dto.RescheduleAppointmentRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas Unitarias para AppointmentServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private com.clinica.doctors.infrastructure.repositories.DoctorScheduleRepository doctorScheduleRepository;

    @Mock
    private AppointmentHistoryRepository appointmentHistoryRepository;

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
        when(doctorScheduleRepository.findByDoctorId(1L)).thenReturn(Optional.empty());

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

    @Test
    @DisplayName("Happy Path: Lista citas por médico y fecha")
    void listAppointmentsByDoctorAndDate_Success() {
        // Arrange
        Appointment app = new Appointment();
        app.setId(10L);
        app.setDoctorId(1L);
        app.setPatientId(2L);
        when(appointmentRepository.findByDoctorIdAndDate(1L, LocalDate.of(2026, 3, 22))).thenReturn(java.util.List.of(app));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));

        // Act
        java.util.List<AppointmentResponse> list = appointmentService.listAppointmentsByDoctorAndDate(1L, LocalDate.of(2026, 3, 22));

        // Assert
        assertEquals(1, list.size());
        assertEquals(10L, list.get(0).id());
        assertEquals("Juan Pérez", list.get(0).doctorName());
        assertEquals("María Gómez", list.get(0).patientName());
    }

    @Test
    @DisplayName("Happy Path: Lista citas por paciente")
    void listAppointmentsByPatient_Success() {
        // Arrange
        Appointment app = new Appointment();
        app.setId(11L);
        app.setDoctorId(1L);
        app.setPatientId(2L);
        when(appointmentRepository.findByPatientId(2L)).thenReturn(java.util.List.of(app));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));

        // Act
        java.util.List<AppointmentResponse> list = appointmentService.listAppointmentsByPatient(2L);

        // Assert
        assertEquals(1, list.size());
        assertEquals(11L, list.get(0).id());
    }

    @Test
    @DisplayName("Happy Path: Obtiene franjas disponibles con horario configurado")
    void getAvailableSlots_WithSchedule() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 3, 23); // Monday
        com.clinica.doctors.domain.entities.DoctorSchedule schedule = new com.clinica.doctors.domain.entities.DoctorSchedule();
        schedule.setWorkingDays(java.util.Set.of(date.getDayOfWeek()));
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(9, 0));
        schedule.setIntervalMinutes(30);

        when(doctorScheduleRepository.findByDoctorId(1L)).thenReturn(Optional.of(schedule));
        when(appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                1L, date, LocalTime.of(8, 0))).thenReturn(false);
        when(appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                1L, date, LocalTime.of(8, 30))).thenReturn(true);

        // Act
        java.util.List<com.clinica.shared.dto.AvailableSlotResponse> slots = appointmentService.getAvailableSlots(1L, date);

        // Assert
        assertEquals(2, slots.size());
        assertEquals(LocalTime.of(8, 0), slots.get(0).startTime());
        assertEquals(true, slots.get(0).available()); // First slot is available
        assertEquals(LocalTime.of(8, 30), slots.get(1).startTime());
        assertEquals(false, slots.get(1).available()); // Second slot is occupied
    }

    @Test
    @DisplayName("Happy Path: Re-agenda una cita exitosamente conservando el historial")
    void rescheduleAppointment_Success() {
        // Arrange
        Long appointmentId = 1L;
        RescheduleAppointmentRequest rescheduleRequest = new RescheduleAppointmentRequest(
                LocalDate.of(2026, 4, 10),
                LocalTime.of(14, 0),
                "Paciente no puede asistir"
        );

        Appointment existingAppointment = new Appointment();
        existingAppointment.setId(appointmentId);
        existingAppointment.setDoctorId(1L);
        existingAppointment.setPatientId(2L);
        existingAppointment.setDate(LocalDate.of(2026, 3, 22));
        existingAppointment.setStartTime(LocalTime.of(10, 0));
        existingAppointment.setEndTime(LocalTime.of(10, 30));
        existingAppointment.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(existingAppointment));
        when(appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                1L, LocalDate.of(2026, 4, 10), LocalTime.of(14, 0))).thenReturn(false);
        when(doctorScheduleRepository.findByDoctorId(1L)).thenReturn(Optional.empty()); // Default 30 mins
        
        // Mock SecurityContext with real Authentication
        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("testUser", "password", java.util.Collections.emptyList());
        SecurityContext securityContext = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(appointmentHistoryRepository.save(any(AppointmentHistory.class))).thenAnswer(i -> i.getArgument(0));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));
        
        // Mocks para toResponse()
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));

        // Act
        AppointmentResponse response = appointmentService.rescheduleAppointment(appointmentId, rescheduleRequest);

        // Assert
        assertNotNull(response);
        assertEquals(LocalDate.of(2026, 4, 10), response.date());
        assertEquals(LocalTime.of(14, 0), response.startTime());
        assertEquals(LocalTime.of(14, 30), response.endTime());
        
        verify(appointmentHistoryRepository).save(any(AppointmentHistory.class));
        verify(appointmentRepository).save(any(Appointment.class));
        
        // Clear SecurityContext
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Edge Case/Error: Falla al re-agendar si la nueva franja está ocupada")
    void rescheduleAppointment_SlotOccupied() {
        // Arrange
        Long appointmentId = 1L;
        RescheduleAppointmentRequest rescheduleRequest = new RescheduleAppointmentRequest(
                LocalDate.of(2026, 4, 10),
                LocalTime.of(14, 0),
                "Paciente no puede asistir"
        );

        Appointment existingAppointment = new Appointment();
        existingAppointment.setId(appointmentId);
        existingAppointment.setDoctorId(1L);
        existingAppointment.setDate(LocalDate.of(2026, 3, 22));
        existingAppointment.setStartTime(LocalTime.of(10, 0));

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(existingAppointment));
        when(appointmentRepository.existsByDoctorIdAndDateAndStartTime(
                1L, LocalDate.of(2026, 4, 10), LocalTime.of(14, 0))).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            appointmentService.rescheduleAppointment(appointmentId, rescheduleRequest);
        });

        assertEquals("Ya existe una cita en esa franja horaria para el médico", exception.getMessage());
        
        verify(appointmentHistoryRepository, never()).save(any(AppointmentHistory.class));
    }
}
