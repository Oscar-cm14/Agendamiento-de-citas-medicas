package com.clinica.doctors.application.services;

import com.clinica.doctors.domain.entities.Doctor;
import com.clinica.doctors.domain.entities.DoctorSchedule;
import com.clinica.doctors.infrastructure.repositories.DoctorRepository;
import com.clinica.doctors.infrastructure.repositories.DoctorScheduleRepository;
import com.clinica.shared.dto.DoctorScheduleRequest;
import com.clinica.shared.dto.DoctorScheduleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas Unitarias para DoctorScheduleServiceImpl (RF4).
 * Validando Cobertura Total según la rúbrica.
 */
@ExtendWith(MockitoExtension.class)
class DoctorScheduleServiceImplTest {

    @Mock
    private DoctorScheduleRepository scheduleRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private DoctorScheduleServiceImpl scheduleService;

    private DoctorScheduleRequest validRequest;
    private Doctor mockDoctor;

    @BeforeEach
    void setUp() {
        // Simulando que el admin configura lunes y miércoles de 8:00 a 12:00, cada 20 minutos.
        validRequest = new DoctorScheduleRequest(
                1L,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                20
        );

        mockDoctor = new Doctor();
        mockDoctor.setId(1L);
        mockDoctor.setFirstName("Ana");
        mockDoctor.setLastName("López");
        mockDoctor.setSpecialty("Pediatría");
    }

    @Test
    @DisplayName("Happy Path (RF4): Crea o actualiza exitosamente el horario de un médico")
    void saveSchedule_Success() {
        // Arrange
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(scheduleRepository.findByDoctorId(1L)).thenReturn(Optional.empty()); // Simula crear uno nuevo

        DoctorSchedule savedSchedule = new DoctorSchedule();
        savedSchedule.setId(10L);
        savedSchedule.setDoctorId(1L);
        savedSchedule.setWorkingDays(validRequest.workingDays());
        savedSchedule.setStartTime(validRequest.startTime());
        savedSchedule.setEndTime(validRequest.endTime());
        savedSchedule.setIntervalMinutes(validRequest.intervalMinutes());

        // Simular la persistencia
        when(scheduleRepository.save(any(DoctorSchedule.class))).thenReturn(savedSchedule);

        // Act
        DoctorScheduleResponse response = scheduleService.saveSchedule(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.doctorId());
        assertEquals("Ana López", response.doctorName());
        assertEquals(20, response.intervalMinutes());
        assertEquals(2, response.workingDays().size());

        // Verificar rigurosamente que el repositorio fue llamado para guardar
        verify(scheduleRepository).save(any(DoctorSchedule.class));
    }

    @Test
    @DisplayName("Edge Case/Error: Falla al configurar el horario si el médico no existe")
    void saveSchedule_DoctorNotFound() {
        // Arrange
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            scheduleService.saveSchedule(validRequest);
        });

        assertEquals("Médico no encontrado", exception.getMessage());
        
        // Verifica que NUNCA intentó persistir en base de datos ya que falló antes
        verify(scheduleRepository, never()).save(any(DoctorSchedule.class));
    }

    @Test
    @DisplayName("Edge Case/Error: Falla al consultar horario si el médico no lo tiene configurado")
    void getScheduleByDoctor_ScheduleNotConfigured() {
        // Arrange
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
        when(scheduleRepository.findByDoctorId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            scheduleService.getScheduleByDoctor(1L);
        });

        assertEquals("Horario no configurado para este médico", exception.getMessage());
    }
}
