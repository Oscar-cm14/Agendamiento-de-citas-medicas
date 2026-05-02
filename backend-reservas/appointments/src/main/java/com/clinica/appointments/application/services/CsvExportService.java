package com.clinica.appointments.application.services;


import java.time.LocalDate;

/**
 * =====================================================================
 * RF5 - NUEVO ARCHIVO
 * Interfaz del servicio de exportación de citas a CSV.
 * Patrón Strategy: define el contrato de exportación.
 * Si en el futuro se necesita PDF o XLSX, se añade otra implementación
 * sin modificar el controlador.
 * =====================================================================
 */
public interface CsvExportService {

    /**
     * Genera el contenido CSV de las citas de un médico en una fecha dada.
     *
     * @param doctorId ID del médico/terapista
     * @param date     Fecha de las citas a exportar
     * @return String con el contenido CSV listo para descargar
     */
    String exportAppointmentsToCsv(Long doctorId, LocalDate date);
}