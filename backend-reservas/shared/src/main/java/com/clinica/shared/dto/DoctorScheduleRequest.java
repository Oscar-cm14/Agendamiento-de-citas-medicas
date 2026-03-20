package com.clinica.shared.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Record representing a request to create or update a doctor's schedule.
 */
public record DoctorScheduleRequest(
    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Start time is required")
    LocalTime startTime,

    @NotNull(message = "End time is required")
    LocalTime endTime,

    @NotNull(message = "Slot interval is required")
    @Min(value = 15, message = "Minimum slot interval is 15 minutes")
    @Max(value = 60, message = "Maximum slot interval is 60 minutes")
    Integer slotIntervalMinutes
) {}
