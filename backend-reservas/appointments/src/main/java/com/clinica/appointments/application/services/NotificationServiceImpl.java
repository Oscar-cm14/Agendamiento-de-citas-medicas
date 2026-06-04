package com.clinica.appointments.application.services;

import com.clinica.shared.dto.AppointmentResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Implementación de NotificationService.
 *
 * CORREO   → Spring Mail (SMTP, por defecto Gmail/Outlook).
 * WHATSAPP → Twilio WhatsApp API (sandbox gratuito disponible).
 *
 * Ambos canales son opcionales e independientes:
 *   - Si el paciente no tiene email → solo WhatsApp.
 *   - Si no tiene teléfono          → solo email.
 *   - Si no tiene ninguno           → se loguea una advertencia.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    // ── Dependencias ─────────────────────────────────────────────────────────
    private final JavaMailSender mailSender;
    private final RestTemplate   restTemplate;

    // ── Configuración correo ─────────────────────────────────────────────────
    @Value("${notification.email.from:noreply@clinica.com}")
    private String emailFrom;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    // ── Configuración WhatsApp / Twilio ──────────────────────────────────────
    @Value("${twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${twilio.whatsapp.from:whatsapp:+14155238886}")
    private String twilioWhatsappFrom;

    @Value("${notification.whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    // ── Nombre de la clínica (configurable) ─────────────────────────────────
    @Value("${clinica.nombre:Clínica}")
    private String nombreClinica;

    // ─────────────────────────────────────────────────────────────────────────

    @Autowired
    public NotificationServiceImpl(@Nullable JavaMailSender mailSender) {
        this.mailSender  = mailSender;
        this.restTemplate = new RestTemplate();
    }

    // =========================================================================
    // MÉTODOS PÚBLICOS
    // =========================================================================

    @Override
    public void notificarCitaCreada(AppointmentResponse cita,
                                    String emailPaciente,
                                    String telefonoPaciente) {

        String asunto  = "✅ Cita confirmada – " + nombreClinica;
        String cuerpoHtml = construirHtmlCreada(cita);
        String textWhatsApp = construirTextoCreada(cita);

        enviarEmail(emailPaciente, asunto, cuerpoHtml);
        enviarWhatsApp(telefonoPaciente, textWhatsApp);
    }

    @Override
    public void notificarCitaCancelada(AppointmentResponse cita,
                                       String emailPaciente,
                                       String telefonoPaciente) {

        String asunto  = "❌ Cita cancelada – " + nombreClinica;
        String cuerpoHtml = construirHtmlCancelada(cita);
        String textWhatsApp = construirTextoCancelada(cita);

        enviarEmail(emailPaciente, asunto, cuerpoHtml);
        enviarWhatsApp(telefonoPaciente, textWhatsApp);
    }

    @Override
    public void notificarCitaReagendada(AppointmentResponse cita,
                                        String emailPaciente,
                                        String telefonoPaciente) {

        String asunto  = "🔄 Cita reagendada – " + nombreClinica;
        String cuerpoHtml = construirHtmlReagendada(cita);
        String textWhatsApp = construirTextoReagendada(cita);

        enviarEmail(emailPaciente, asunto, cuerpoHtml);
        enviarWhatsApp(telefonoPaciente, textWhatsApp);
    }

    // =========================================================================
    // ENVÍO DE CORREO
    // =========================================================================

    private void enviarEmail(String destinatario, String asunto, String cuerpoHtml) {

        if (mailSender == null) {
            log.warn("[NOTIF-EMAIL] JavaMailSender no configurado. Se omite envío a {}", destinatario);
            return;
        }
        if (!emailEnabled) {
            log.info("[NOTIF-EMAIL] Canal deshabilitado. Se omite envío a {}", destinatario);
            return;
        }
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("[NOTIF-EMAIL] El paciente no tiene email registrado. Se omite notificación.");
            return;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true); // true = HTML

            mailSender.send(mensaje);
            log.info("[NOTIF-EMAIL] Correo enviado a {}", destinatario);

        } catch (MessagingException e) {
            // No lanzamos excepción; la notificación no debe bloquear la reserva
            log.error("[NOTIF-EMAIL] Error enviando correo a {}: {}", destinatario, e.getMessage());
        }
    }

    // =========================================================================
    // ENVÍO DE WHATSAPP (Twilio)
    // =========================================================================

    private void enviarWhatsApp(String telefonoPaciente, String texto) {

        if (!whatsappEnabled) {
            log.info("[NOTIF-WA] Canal deshabilitado. Se omite envío.");
            return;
        }
        if (telefonoPaciente == null || telefonoPaciente.isBlank()) {
            log.warn("[NOTIF-WA] El paciente no tiene teléfono registrado. Se omite notificación.");
            return;
        }
        if (twilioAccountSid == null || twilioAccountSid.isBlank() ||
            twilioAuthToken  == null || twilioAuthToken.isBlank()) {
            log.error("[NOTIF-WA] Credenciales Twilio no configuradas (twilio.account-sid / twilio.auth-token).");
            return;
        }

        // Normalizar número → formato E.164 con prefijo whatsapp:
        String destino = normalizarTelefono(telefonoPaciente);

        // Twilio Messages API
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid + "/Messages.json";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(twilioAccountSid, twilioAuthToken);

        String body = "From=" + encode(twilioWhatsappFrom)
                    + "&To="   + encode(destino)
                    + "&Body=" + encode(texto);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[NOTIF-WA] WhatsApp enviado a {}", destino);
            } else {
                log.warn("[NOTIF-WA] Respuesta inesperada de Twilio: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("[NOTIF-WA] Error enviando WhatsApp a {}: {}", destino, e.getMessage());
        }
    }

    // =========================================================================
    // BUILDERS DE MENSAJES – CORREO (HTML)
    // =========================================================================

    private String construirHtmlCreada(AppointmentResponse cita) {
        return html(
            "✅ Cita Confirmada",
            "Su cita ha sido <strong>confirmada exitosamente</strong>.",
            cita,
            "#28a745"
        );
    }

    private String construirHtmlCancelada(AppointmentResponse cita) {
        return html(
            "❌ Cita Cancelada",
            "Su cita ha sido <strong>cancelada</strong>."
                + (cita.cancellationReason() != null
                    ? "<br><em>Motivo: " + cita.cancellationReason() + "</em>"
                    : ""),
            cita,
            "#dc3545"
        );
    }

    private String construirHtmlReagendada(AppointmentResponse cita) {
        return html(
            "🔄 Cita Reagendada",
            "Su cita ha sido <strong>reagendada</strong> a la siguiente fecha y hora.",
            cita,
            "#fd7e14"
        );
    }

    private String html(String titulo, String mensaje, AppointmentResponse cita, String color) {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter horaFmt  = DateTimeFormatter.ofPattern("HH:mm");

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px;">
              <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.15);">

                <!-- Encabezado -->
                <div style="background:%s;padding:24px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:22px;">%s</h1>
                </div>

                <!-- Cuerpo -->
                <div style="padding:28px;">
                  <p style="font-size:16px;color:#333;">%s</p>

                  <table style="width:100%%;border-collapse:collapse;margin-top:16px;">
                    <tr style="background:#f8f9fa;">
                      <td style="padding:10px;border:1px solid #dee2e6;font-weight:bold;">Paciente</td>
                      <td style="padding:10px;border:1px solid #dee2e6;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px;border:1px solid #dee2e6;font-weight:bold;">Médico</td>
                      <td style="padding:10px;border:1px solid #dee2e6;">Dr(a). %s</td>
                    </tr>
                    <tr style="background:#f8f9fa;">
                      <td style="padding:10px;border:1px solid #dee2e6;font-weight:bold;">Especialidad</td>
                      <td style="padding:10px;border:1px solid #dee2e6;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:10px;border:1px solid #dee2e6;font-weight:bold;">Fecha</td>
                      <td style="padding:10px;border:1px solid #dee2e6;">%s</td>
                    </tr>
                    <tr style="background:#f8f9fa;">
                      <td style="padding:10px;border:1px solid #dee2e6;font-weight:bold;">Hora</td>
                      <td style="padding:10px;border:1px solid #dee2e6;">%s – %s</td>
                    </tr>
                  </table>

                  <p style="color:#6c757d;font-size:13px;margin-top:24px;">
                    Si tiene alguna pregunta, contáctenos. Gracias por confiar en <strong>%s</strong>.
                  </p>
                </div>

                <!-- Pie -->
                <div style="background:#f8f9fa;padding:14px;text-align:center;font-size:12px;color:#aaa;">
                  Este correo es generado automáticamente, por favor no responda.
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                color, titulo, mensaje,
                cita.patientName(),
                cita.doctorName(),
                cita.specialty() != null ? cita.specialty() : "—",
                cita.date().format(fechaFmt),
                cita.startTime().format(horaFmt),
                cita.endTime().format(horaFmt),
                nombreClinica
            );
    }

    // =========================================================================
    // BUILDERS DE MENSAJES – WHATSAPP (texto plano)
    // =========================================================================

    private String construirTextoCreada(AppointmentResponse cita) {
        return formatTextoWA("✅ *CITA CONFIRMADA*", cita, null);
    }

    private String construirTextoCancelada(AppointmentResponse cita) {
        String extra = cita.cancellationReason() != null
            ? "\n📌 *Motivo:* " + cita.cancellationReason()
            : "";
        return formatTextoWA("❌ *CITA CANCELADA*", cita, extra);
    }

    private String construirTextoReagendada(AppointmentResponse cita) {
        return formatTextoWA("🔄 *CITA REAGENDADA*", cita, null);
    }

    private String formatTextoWA(String titulo, AppointmentResponse cita, String extra) {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter horaFmt  = DateTimeFormatter.ofPattern("HH:mm");

        return titulo + "\n\n"
            + "👤 *Paciente:* " + cita.patientName() + "\n"
            + "👨‍⚕️ *Médico:* Dr(a). " + cita.doctorName() + "\n"
            + "🏥 *Especialidad:* " + (cita.specialty() != null ? cita.specialty() : "—") + "\n"
            + "📅 *Fecha:* " + cita.date().format(fechaFmt) + "\n"
            + "⏰ *Hora:* " + cita.startTime().format(horaFmt) + " – " + cita.endTime().format(horaFmt)
            + (extra != null ? extra : "")
            + "\n\n_" + nombreClinica + " – Sistema de Agendamiento_";
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    /** Normaliza el teléfono colombiano al formato WhatsApp de Twilio. */
    private String normalizarTelefono(String telefono) {
        String limpio = telefono.replaceAll("[^0-9+]", "");
        if (!limpio.startsWith("+")) {
            // Asume Colombia +57 si el número tiene 10 dígitos y empieza en 3
            if (limpio.length() == 10 && limpio.startsWith("3")) {
                limpio = "+57" + limpio;
            } else if (!limpio.startsWith("57")) {
                limpio = "+57" + limpio;
            } else {
                limpio = "+" + limpio;
            }
        }
        return "whatsapp:" + limpio;
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}