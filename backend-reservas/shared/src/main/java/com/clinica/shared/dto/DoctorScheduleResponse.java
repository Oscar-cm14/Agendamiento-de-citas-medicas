package com.clinica.shared.dto;


import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/**
 * DTO for returning a doctor's schedule configuration.
 */
public record DoctorScheduleResponse(

        Long id,
        Long doctorId,
        String doctorName,
        Set<DayOfWeek> workingDays,
        LocalTime startTime,
        LocalTime endTime,
        Integer intervalMinutes
) {}