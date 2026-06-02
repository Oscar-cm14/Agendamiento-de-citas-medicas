package com.clinica.appointments.application.services;

import com.clinica.shared.dto.AppointmentResponse;

/**
 * Servicio de notificaciones para citas médicas.
 * Envía confirmaciones por correo electrónico y/o WhatsApp
 * cuando un paciente reserva, cancela o reagenda una cita.
 */
public interface NotificationService {

    /**
     * Notifica al paciente que su cita fue creada exitosamente.
     */
    void notificarCitaCreada(AppointmentResponse cita, String emailPaciente, String telefonoPaciente);

    /**
     * Notifica al paciente que su cita fue cancelada.
     */
    void notificarCitaCancelada(AppointmentResponse cita, String emailPaciente, String telefonoPaciente);

    /**
     * Notifica al paciente que su cita fue reagendada.
     */
    void notificarCitaReagendada(AppointmentResponse cita, String emailPaciente, String telefonoPaciente);
}