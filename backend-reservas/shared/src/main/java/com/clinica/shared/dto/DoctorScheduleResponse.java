package com.clinica.shared.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Record representing a response containing doctor's schedule details.
 */
public record DoctorScheduleResponse(
    Long id,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    Integer slotIntervalMinutes,
    Long doctorId
) {}
