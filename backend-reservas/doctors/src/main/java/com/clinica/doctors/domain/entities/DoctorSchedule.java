package com.clinica.doctors.domain.entities;


import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/**
 * Entity representing the weekly schedule of a doctor.
 * Used in RF4: admin configures doctor availability.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "doctor_schedules")
public class DoctorSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID del médico al que pertenece este horario
    private Long doctorId;

    // Días de la semana que atiende
    @ElementCollection
    @CollectionTable(name = "schedule_days",
            joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "day_of_week")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> workingDays;

    // Hora de inicio de atención
    private LocalTime startTime;

    // Hora de fin de atención
    private LocalTime endTime;

    // Intervalo en minutos entre cita y cita
    private Integer intervalMinutes;
}