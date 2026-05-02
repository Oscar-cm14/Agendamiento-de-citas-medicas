package com.clinica.appointments.infrastructure.controllers;



import com.clinica.appointments.application.services.CsvExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * =====================================================================
 * RF5 - NUEVO ARCHIVO
 * Controlador REST para exportar citas a CSV.
 *
 * Endpoint:
 *   GET /api/v1/appointments/export?doctorId=1&date=2026-05-10
 *
 * Roles permitidos: ADMIN, SCHEDULER, DOCTOR
 *   (configurado en SecurityConfig)
 *
 * Retorna un archivo CSV con nombre "citas_YYYY-MM-DD.csv"
 * con BOM UTF-8 para compatibilidad con Excel en Windows.
 * =====================================================================
 */
@RestController
@RequestMapping("/api/v1/appointments")
public class CsvExportController {

    private final CsvExportService csvExportService;

    public CsvExportController(CsvExportService csvExportService) {
        this.csvExportService = csvExportService;
    }

    /**
     * RF5: Exporta las citas de un médico en una fecha a formato CSV.
     *
     * @param doctorId ID del médico/terapista
     * @param date     Fecha (formato ISO: yyyy-MM-dd)
     * @return Archivo CSV para descarga directa en el navegador
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToCsv(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // Generar contenido del CSV usando el servicio
        String csvContent = csvExportService.exportAppointmentsToCsv(doctorId, date);

        // Nombre del archivo de descarga: citas_2026-05-10.csv
        String filename = "citas_" + date.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";

        // ── BOM UTF-8 ──────────────────────────────────────────────────────
        // Excel en Windows necesita el BOM (Byte Order Mark) para detectar
        // automáticamente que el archivo está en UTF-8 y no mostrar caracteres
        // extraños en tildes y ñ.
        byte[] bom     = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = csvContent.getBytes(StandardCharsets.UTF_8);
        byte[] result  = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);

        // ── Headers de respuesta HTTP ──────────────────────────────────────
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        // "attachment" hace que el navegador descargue el archivo en lugar de mostrarlo
        headers.setContentDispositionFormData("attachment", filename);
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");

        return ResponseEntity.ok()
                .headers(headers)
                .body(result);
    }
}
