package com.clinica.appointments.domain.entities;

import com.clinica.shared.domain.entities.AppointmentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    private Long doctorId;
    private Long patientId;

    private String notes;

    // motivo de cancelación
    private String cancellationReason;

    // ── CAMPOS DE PRIORIDAD ─────────────────────────────────────────
    private boolean priority = false;

    /** HIGH | MEDIUM | LOW | null */
    private String urgencyLevel;

    private String priorityReason;
}