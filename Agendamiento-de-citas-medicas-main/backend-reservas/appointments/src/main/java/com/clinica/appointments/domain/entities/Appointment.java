package com.clinica.appointments.domain.entities;


import com.clinica.shared.domain.entities.AppointmentStatus;
import com.clinica.shared.domain.entities.Person;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entity representing a medical appointment in the clinic system.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Fecha de la cita
    private LocalDate date;

    // Hora de inicio de la cita
    private LocalTime startTime;

    // Hora de fin (calculada según intervalo del médico)
    private LocalTime endTime;

    // Estado de la cita
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    // ID del médico (referencia al módulo doctors)
    private Long doctorId;

    // ID del paciente (referencia al módulo users)
    private Long patientId;

    // Notas adicionales
    private String notes;
}
