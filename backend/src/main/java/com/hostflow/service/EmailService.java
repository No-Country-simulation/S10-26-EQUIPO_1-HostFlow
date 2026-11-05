package com.hostflow.service;

import com.hostflow.model.Accommodation;
import com.hostflow.model.Reservation;
import com.resend.Resend;
import com.resend.services.emails.model.SendEmailRequest;
import com.resend.services.emails.model.SendEmailResponse;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Servicio centralizado para el envío de correos electrónicos en HostFlow.
 *
 * <p>Utiliza un enfoque HÍBRIDO inteligente:</p>
 * <ul>
 *   <li>🟢 <strong>Producción (Render):</strong> Usa Resend API si la clave está configurada.</li>
 *   <li>🟡 <strong>Desarrollo (Local):</strong> Usa Gmail SMTP si Resend no está configurado.</li>
 *   <li>🔴 <strong>Modo Fallback:</strong> Si ninguno está configurado, solo registra el enlace en los logs.</li>
 * </ul>
 *
 * <p><strong>Política de resiliencia:</strong> Todos los métodos de envío están envueltos
 * en bloques {@code try-catch}. Si el envío de email falla, se registra el error en los logs
 * pero NO se lanza una excepción, evitando interrumpir el flujo principal de negocio.</p>
 *
 * @author HostFlow Team
 * @version 4.1.0 (Corregido patrón Builder en Resend SDK y optimización de código)
 */
@Service
@Slf4j
public class EmailService {

    // =========================================================================
    // DEPENDENCIAS E INYECCIÓN HÍBRIDA
    // =========================================================================

    // required = false evita que la aplicación falle al iniciar en Render si no hay config SMTP
    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final TemplateEngine templateEngine;
    private final ExchangeRateProviderService exchangeRateService;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${spring.mail.username:no-reply@hostflow.com}")
    private String fromEmail;

    @Value("${spring.application.name:HostFlow}")
    private String appName;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    // Constructor manual (necesario porque mezclamos @Autowired y inyección por constructor)
    public EmailService(TemplateEngine templateEngine, ExchangeRateProviderService exchangeRateService) {
        this.templateEngine = templateEngine;
        this.exchangeRateService = exchangeRateService;
    }

    // =========================================================================
    // MOTOR HÍBRIDO DE ENVÍO DE CORREOS (NÚCLEO)
    // =========================================================================

    /**
     * Enrutador inteligente que decide si usar Resend (Nube) o Gmail SMTP (Local).
     */
    private void sendHybridEmail(String to, String subject, String htmlContent, String plainTextContent) {
        // 🟢 ESCENARIO A: Render / Producción (API Key de Resend configurada y válida)
        if (resendApiKey != null && !resendApiKey.trim().isEmpty() && resendApiKey.startsWith("re_")) {
            sendViaResend(to, subject, htmlContent != null ? htmlContent : plainTextContent);
        }
        // 🟡 ESCENARIO B: Local (No hay API Key de Resend, usamos Gmail SMTP)
        else if (mailSender != null) {
            sendViaGmail(to, subject, htmlContent, plainTextContent);
        }
        // 🔴 ESCENARIO C: Modo desarrollo sin proveedores de email configurados
        else {
            log.warn("⚠️ MODO DESARROLLO: No se configuró ni Resend ni Gmail. El correo NO se envió a {}", to);
            log.info("🔗 Contenido simulado:\nAsunto: {}\n{}", subject, plainTextContent != null ? plainTextContent : htmlContent);
        }
    }

    /**
     * Implementación 1: Envío vía Resend (Ideal para Render / Producción)
     */
    private void sendViaResend(String to, String subject, String content) {
        try {
            Resend resend = new Resend(resendApiKey);

            // ✅ CORRECCIÓN: Usar Builder pattern requerido por el SDK de Resend
            SendEmailRequest request = SendEmailRequest.builder()
                    .from("HostFlow <onboarding@resend.dev>") // Cambiar por tu dominio verificado ej: "HostFlow <noreply@tu-dominio.com>"
                    .to(java.util.List.of(to))
                    .subject(subject)
                    .html(content)
                    .build();

            SendEmailResponse response = resend.emails().send(request);
            log.info("✅ Email enviado exitosamente vía Resend a {}. ID: {}", to, response.getId());
        } catch (Exception e) {
            log.error("❌ Error enviando email vía Resend a {}: {}", to, e.getMessage(), e);
            // No lanzamos excepción para cumplir con la política de resiliencia
        }
    }

    /**
     * Implementación 2: Envío vía Gmail SMTP (Ideal para Desarrollo Local)
     */
    private void sendViaGmail(String to, String subject, String htmlContent, String plainTextContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            if (htmlContent != null && !htmlContent.trim().isEmpty()) {
                helper.setText(htmlContent, true); // true = es HTML
            } else if (plainTextContent != null) {
                helper.setText(plainTextContent, false); // false = es texto plano
            }

            mailSender.send(message);
            log.info("✅ Email enviado exitosamente vía Gmail SMTP a: {}", to);
        } catch (Exception e) {
            log.error("❌ Error enviando email vía Gmail a {}: {}", to, e.getMessage(), e);
            // No lanzamos excepción para cumplir con la política de resiliencia
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PARA LÓGICA DE MONEDAS
    // =========================================================================

    private boolean isArgentinaOnly(Accommodation accommodation) {
        boolean isArsCurrency = accommodation.getBaseCurrency() != null &&
                accommodation.getBaseCurrency().toString().equalsIgnoreCase("ARS");
        return isArsCurrency;
    }

    private BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || fromCurrency == null || toCurrency == null) {
            return BigDecimal.ZERO;
        }
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }
        try {
            BigDecimal rate = exchangeRateService.getConversionRate(fromCurrency, toCurrency);
            return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("⚠️ No se pudo convertir de {} a {}. Usando monto original.", fromCurrency, toCurrency);
            return amount;
        }
    }

    // =========================================================================
    // MÉTODOS: AUTENTICACIÓN Y SEGURIDAD
    // =========================================================================

    public void sendPasswordResetEmail(String toEmail, String resetCode, int expirationMin) {
        String resetLink = "https://hostflow-lilac.vercel.app/reset-password?token=" + resetCode; // Ajusta a tu URL real

        // Contenido HTML profesional
        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0;">
                    <h1>🏨 %s</h1>
                </div>
                <div style="background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px;">
                    <h2>Hola,</h2>
                    <p>Hemos recibido una solicitud para restablecer tu contraseña.</p>
                    <p><strong>Tu código de recuperación es:</strong> <span style="font-size: 24px; font-weight: bold; color: #667eea;">%s</span></p>
                    <p style="margin-top: 20px; font-size: 14px; color: #666;">O haz clic en el siguiente enlace (expira en %d minutos):<br>
                    <a href="%s" style="color: #667eea;">%s</a></p>
                    <p style="margin-top: 20px; font-size: 14px; color: #666;">Si no lo solicitaste, ignora este correo.</p>
                </div>
            </div>
            """, appName, resetCode, expirationMin, resetLink, resetLink);

        // Texto plano como fallback
        String plainText = String.format("Tu código de recuperación en %s es: %s. Expira en %d minutos. Link: %s", appName, resetCode, expirationMin, resetLink);

        sendHybridEmail(toEmail, "🔐 " + appName + " - Código de recuperación de contraseña", htmlContent, plainText);
    }

    public void sendPasswordChangedConfirmation(String toEmail) {
        String plainText = String.format("Te confirmamos que tu contraseña en %s ha sido actualizada exitosamente.", appName);
        sendHybridEmail(toEmail, "✅ " + appName + " - Contraseña actualizada", null, plainText);
    }

    public void sendEmailVerificationEmail(String toEmail, String verificationToken, String verificationUrl, int expirationMin) {
        String plainText = String.format("¡Bienvenido a %s!\n\nVerifica tu correo haciendo clic aquí: %s\nExpira en %d minutos.", appName, verificationUrl, expirationMin);
        sendHybridEmail(toEmail, "✅ " + appName + " - Verifica tu correo electrónico", null, plainText);
    }

    public void sendEmailVerificationResendEmail(String toEmail, String verificationToken, String verificationUrl, int expirationMin) {
        String plainText = String.format("Aquí tienes tu nuevo enlace de verificación para %s: %s\nExpira en %d minutos.", appName, verificationUrl, expirationMin);
        sendHybridEmail(toEmail, "🔄 " + appName + " - Nuevo enlace de verificación", null, plainText);
    }

    // =========================================================================
    // MÉTODOS: SISTEMA DE RESERVAS
    // =========================================================================

    public void sendReservationConfirmationEmail(String toEmail, String guestName, String confirmationCode,
                                                 String confirmationUrl, String accommodationName,
                                                 LocalDateTime checkIn, LocalDateTime checkOut,
                                                 BigDecimal totalAmount, int expirationHours) {
        String plainText = String.format("""
            ¡Hola %s!
            Un propietario ha creado una reserva para ti en %s.
            Código: %s | Check-in: %s | Check-out: %s | Monto total: $%,.2f
            Para confirmar, haz clic en: %s (Expira en %d horas)
            """, guestName, accommodationName, confirmationCode,
                checkIn.format(DATE_FORMATTER), checkOut.format(DATE_FORMATTER), totalAmount, confirmationUrl, expirationHours);

        sendHybridEmail(toEmail, "🏨 " + appName + " - Confirma tu reserva " + confirmationCode, null, plainText);
    }

    public void sendReservationConfirmedGuestEmail(Reservation reservation, Accommodation accommodation,
                                                   String guestName, String reservationUrl) {
        if (reservation.getGuest() == null || reservation.getGuest().getEmail() == null) {
            log.warn("⚠️ No se puede enviar email al huésped: datos nulos.");
            return;
        }

        Context context = new Context();
        context.setVariable("reservation", reservation);
        context.setVariable("accommodation", accommodation);
        context.setVariable("guestName", guestName);
        context.setVariable("watermark", "Plataforma de Reservas HostFlow");
        context.setVariable("reservationUrl", reservationUrl);

        boolean argentinaOnly = isArgentinaOnly(accommodation);
        context.setVariable("isArgentinaOnly", argentinaOnly);

        String originalCurrency = accommodation.getBaseCurrency() != null ?
                accommodation.getBaseCurrency().toString().toUpperCase() : "ARS";
        BigDecimal total = reservation.getTotalAmount() != null ? reservation.getTotalAmount() : BigDecimal.ZERO;

        context.setVariable("totalArs", convertCurrency(total, originalCurrency, "ARS"));
        context.setVariable("totalUsd", convertCurrency(total, originalCurrency, "USD"));
        context.setVariable("totalEur", convertCurrency(total, originalCurrency, "EUR"));
        context.setVariable("originalCurrency", originalCurrency);

        String htmlContent = templateEngine.process("emails/reservation-confirmation-guest", context);

        sendHybridEmail(
                reservation.getGuest().getEmail(),
                "✅ " + appName + " - ¡Reserva confirmada! " + reservation.getConfirmationCode(),
                htmlContent,
                null // No necesita texto plano porque Thymeleaf ya genera HTML perfecto
        );
    }

    public void sendReservationConfirmedOwnerEmail(Reservation reservation, Accommodation accommodation,
                                                   String ownerName, String reservationUrl) {
        if (accommodation.getOwner() == null || accommodation.getOwner().getEmail() == null) {
            log.error("❌ No se puede enviar email al propietario: datos nulos.");
            return;
        }

        Context context = new Context();
        context.setVariable("reservation", reservation);
        context.setVariable("accommodation", accommodation);
        context.setVariable("ownerName", ownerName);
        context.setVariable("watermark", "Panel de Administración HostFlow");
        context.setVariable("reservationUrl", reservationUrl);

        boolean argentinaOnly = isArgentinaOnly(accommodation);
        context.setVariable("isArgentinaOnly", argentinaOnly);

        String originalCurrency = accommodation.getBaseCurrency() != null ?
                accommodation.getBaseCurrency().toString().toUpperCase() : "ARS";
        BigDecimal total = reservation.getTotalAmount() != null ? reservation.getTotalAmount() : BigDecimal.ZERO;

        context.setVariable("totalArs", convertCurrency(total, originalCurrency, "ARS"));
        context.setVariable("totalUsd", convertCurrency(total, originalCurrency, "USD"));
        context.setVariable("totalEur", convertCurrency(total, originalCurrency, "EUR"));
        context.setVariable("originalCurrency", originalCurrency);

        String htmlContent = templateEngine.process("emails/reservation-notification-owner", context);

        sendHybridEmail(
                accommodation.getOwner().getEmail(),
                "🔔 " + appName + " - ¡Nueva reserva confirmada! " + reservation.getConfirmationCode(),
                htmlContent,
                null
        );
    }

    public void sendReservationCancelledEmail(String toEmail, String guestName, String confirmationCode,
                                              String accommodationName, String reason) {
        String plainText = String.format("¡Hola %s!\n\nTu reserva %s en %s ha sido cancelada.\nMotivo: %s",
                guestName, confirmationCode, accommodationName, reason != null ? reason : "No especificado");

        sendHybridEmail(toEmail, "❌ " + appName + " - Reserva cancelada " + confirmationCode, null, plainText);
    }

    // =========================================================================
    // MÉTODOS: MÉTODOS DE PAGO DEL ALOJAMIENTO
    // =========================================================================

    public void sendPaymentMethodCreatedEmail(String toEmail, String ownerName,
                                              String accommodationName,
                                              String paymentMethodName,
                                              String paymentType,
                                              boolean isDefault) {
        String plainText = String.format("Hola %s,\n\nSe ha registrado un nuevo método de pago (%s - %s) en tu alojamiento \"%s\".",
                ownerName, paymentMethodName, paymentType, accommodationName);

        sendHybridEmail(toEmail, "💳 " + appName + " - Nuevo método de pago registrado", null, plainText);
    }
}