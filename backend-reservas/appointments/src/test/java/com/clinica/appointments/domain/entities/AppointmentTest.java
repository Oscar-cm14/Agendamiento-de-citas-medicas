package com.clinica.appointments.domain.entities;

import com.clinica.shared.domain.entities.AppointmentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppointmentTest {

    @Test
    void testAppointmentGettersAndSetters() {
        // Arrange
        Appointment appointment = new Appointment();
        Long id = 1L;
        LocalDate date = LocalDate.of(2025, 5, 20);
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(10, 30);
        AppointmentStatus status = AppointmentStatus.SCHEDULED;
        Long doctorId = 10L;
        Long patientId = 20L;
        String notes = "Patient has slight fever";

        // Act
        appointment.setId(id);
        appointment.setDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(status);
        appointment.setDoctorId(doctorId);
        appointment.setPatientId(patientId);
        appointment.setNotes(notes);

        // Assert
        assertEquals(id, appointment.getId());
        assertEquals(date, appointment.getDate());
        assertEquals(startTime, appointment.getStartTime());
        assertEquals(endTime, appointment.getEndTime());
        assertEquals(status, appointment.getStatus());
        assertEquals(doctorId, appointment.getDoctorId());
        assertEquals(patientId, appointment.getPatientId());
        assertEquals(notes, appointment.getNotes());
    }

    @Test
    void testAppointmentDefaultConstructor() {
        // Arrange & Act
        Appointment appointment = new Appointment();

        // Assert
        assertNull(appointment.getId());
        assertNull(appointment.getDate());
        assertNull(appointment.getStartTime());
        assertNull(appointment.getEndTime());
        assertNull(appointment.getStatus());
        assertNull(appointment.getDoctorId());
        assertNull(appointment.getPatientId());
        assertNull(appointment.getNotes());
    }
}
