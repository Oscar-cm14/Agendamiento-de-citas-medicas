package com.clinica.appointments.application.services;


import com.clinica.appointments.domain.entities.Appointment;
import com.clinica.appointments.infrastructure.repositories.AppointmentRepository;
import com.clinica.doctors.domain.entities.Doctor;
import com.clinica.doctors.infrastructure.repositories.DoctorRepository;
import com.clinica.users.domain.entities.Patient;
import com.clinica.users.infrastructure.repositories.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * =====================================================================
 * RF5 - NUEVO ARCHIVO
 * Implementación del servicio de exportación a CSV.
 *
 * Patrón de diseño: Strategy
 *   - CsvExportService = interfaz (estrategia abstracta)
 *   - Esta clase      = estrategia concreta para formato CSV
 *   - Ventaja: se puede añadir PdfExportServiceImpl sin tocar el
 *     controlador ni el cliente.
 *
 * El CSV generado incluye:
 *   - Líneas de meta-información (comentarios con #)
 *   - Cabecera con nombres de columnas
 *   - Una fila por cita con datos del paciente
 *   - BOM UTF-8 (añadido en el controlador) para compatibilidad Excel
 * =====================================================================
 */
@Service
public class CsvExportServiceImpl implements CsvExportService {

    // ── Constantes de formato ────────────────────────────────────────────
    private static final String CSV_HEADER =
            "N°,Paciente,Identificación,Celular,Género,Fecha,Hora Inicio,Hora Fin,Estado,Notas";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── Dependencias ─────────────────────────────────────────────────────
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository      doctorRepository;
    private final PatientRepository     patientRepository;

    public CsvExportServiceImpl(AppointmentRepository appointmentRepository,
                                DoctorRepository doctorRepository,
                                PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository      = doctorRepository;
        this.patientRepository     = patientRepository;
    }

    /**
     * RF5: Genera el CSV de citas de un médico en una fecha determinada.
     * Cada fila representa una cita; la primera fila es la cabecera.
     */
    @Override
    public String exportAppointmentsToCsv(Long doctorId, LocalDate date) {

        // Validar que el médico exista antes de generar el archivo
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException(
                        "Médico no encontrado con id: " + doctorId));

        // Obtener todas las citas del médico en esa fecha
        List<Appointment> appointments =
                appointmentRepository.findByDoctorIdAndDate(doctorId, date);

        StringBuilder csv = new StringBuilder();

        // ── Líneas de meta-información (no interfieren con el parseo CSV) ──
        csv.append("# Listado de citas - ")
           .append(doctor.getFirstName()).append(" ").append(doctor.getLastName())
           .append(" (").append(doctor.getSpecialty()).append(")")
           .append(" - ").append(date.format(DATE_FMT))
           .append("\n");
        csv.append("# Total de citas: ").append(appointments.size()).append("\n");

        // ── Cabecera del CSV ──────────────────────────────────────────────
        csv.append(CSV_HEADER).append("\n");

        // ── Filas de datos (una por cita) ─────────────────────────────────
        int number = 1;
        for (Appointment appt : appointments) {

            // Buscar datos del paciente (puede ser null si fue eliminado)
            Patient patient = patientRepository.findById(appt.getPatientId()).orElse(null);

            // Construir cada campo con escape CSV
            String patientName  = patient != null
                    ? escapeCsv(patient.getFirstName() + " " + patient.getLastName())
                    : "Desconocido";
            String identification = patient != null
                    ? escapeCsv(patient.getIdentification()) : "";
            String phone          = patient != null && patient.getPhone() != null
                    ? escapeCsv(patient.getPhone()) : "";
            String gender         = patient != null && patient.getGender() != null
                    ? escapeCsv(patient.getGender()) : "";
            String dateStr        = appt.getDate().format(DATE_FMT);
            String startTimeStr   = appt.getStartTime().format(TIME_FMT);
            String endTimeStr     = appt.getEndTime() != null
                    ? appt.getEndTime().format(TIME_FMT) : "";
            String status         = appt.getStatus() != null
                    ? appt.getStatus().name() : "";
            String notes          = appt.getNotes() != null
                    ? escapeCsv(appt.getNotes()) : "";

            // Escribir la fila
            csv.append(number++).append(",")
               .append(patientName).append(",")
               .append(identification).append(",")
               .append(phone).append(",")
               .append(gender).append(",")
               .append(dateStr).append(",")
               .append(startTimeStr).append(",")
               .append(endTimeStr).append(",")
               .append(status).append(",")
               .append(notes).append("\n");
        }

        return csv.toString();
    }

    /**
     * Escapa un campo CSV según RFC 4180:
     * Si el valor contiene coma, comilla doble o salto de línea,
     * lo envuelve en comillas dobles y duplica las comillas internas.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}