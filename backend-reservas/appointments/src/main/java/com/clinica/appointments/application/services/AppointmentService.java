package com.clinica.appointments.application.services;

import com.clinica.shared.dto.ManualAppointmentRequest;

/**
 * Interface defining operations for managing medical appointments.
 */
public interface AppointmentService {

    /**
     * Schedules an appointment manually (typically by a scheduler or admin).
     *
     * @param request The data required to schedule the appointment.
     */
    void scheduleManualAppointment(ManualAppointmentRequest request);
}
