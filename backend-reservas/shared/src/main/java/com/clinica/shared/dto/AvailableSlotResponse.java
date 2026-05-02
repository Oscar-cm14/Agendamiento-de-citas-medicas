package com.clinica.shared.dto;


import java.time.LocalTime;

/**
 * DTO representing an available time slot for a doctor on a given date.
 * Used in RF3 to show available appointment times to the patient.
 */
public record AvailableSlotResponse(

        LocalTime startTime,
        LocalTime endTime,
        boolean available
) {}