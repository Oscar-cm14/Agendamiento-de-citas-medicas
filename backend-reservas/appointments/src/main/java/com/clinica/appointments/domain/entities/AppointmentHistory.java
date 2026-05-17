package com.clinica.appointments.domain.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity representing the history/audit trail of an appointment.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "appointment_history")
public class AppointmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    private LocalDate previousDate;
    private LocalTime previousStartTime;
    private LocalTime previousEndTime;

    private LocalDate newDate;
    private LocalTime newStartTime;
    private LocalTime newEndTime;

    private LocalDateTime changedAt;
    
    // Almacena el usuario responsable del cambio extraído del contexto de seguridad
    private String changedBy;
    
    private String reason;
}
