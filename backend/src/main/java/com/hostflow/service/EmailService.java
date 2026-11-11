package com.hostflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostflow.exception.EmailDeliveryException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.Reservation;
import com.hostflow.enums.Currency;
import com.hostflow.enums.PaymentMethodType;
import com.hostflow.model.AccommodationPaymentMethod;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Servicio centralizado para el envío de correos electrónicos en HostFlow.
 *
 * <p>Utiliza un enfoque HÍBRIDO inteligente:</p>
 * <ul>
 *   <li>🟢 <strong>Producción (Render):</strong> Usa la API de Brevo si la clave está configurada.</li>
 *   <li>🟡 <strong>Desarrollo (Local):</strong> Usa Gmail SMTP si Brevo no está configurado.</li>
 *   <li>🔴 <strong>Modo Fallback:</strong> Si ninguno está configurado, solo registra el contenido en los logs
 *       (modo tolerante) o falla (modo estricto).</li>
 * </ul>
 *
 * <p><strong>Política de envío (Fase A — reservas):</strong></p>
 * <ul>
 *   <li><strong>Estricto:</strong> creación y renovación del enlace de onboarding. Si el proveedor
 *       rechaza el mensaje o no hay proveedor configurado, se propaga
 *       {@link com.hostflow.exception.EmailDeliveryException} para revertir la transacción.</li>
 *   <li><strong>Tolerante:</strong> pago, cancelación, auth y métodos de pago. Los fallos se
 *       registran en log y no interrumpen la operación de negocio.</li>
 * </ul>
 *
 * <p>La aceptación del proveedor no garantiza entrega en bandeja de entrada; solo confirma
 * que el proveedor aceptó el mensaje (Brevo HTTP 201 o SMTP sin excepción).</p>
 *
 * @author HostFlow Team
 * @version 4.4.0 (Política dual estricto/tolerante para correos de reservas)
 */
@Service
@Slf4j
public class EmailService {

    // =========================================================================
    // DEPENDENCIAS E INYECCIÓN HÍBRIDA
    // =========================================================================

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final TemplateEngine templateEngine;
    private final CurrencyConversionService currencyConversionService;
    private final ObjectMapper objectMapper;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${spring.mail.username:no-reply@hostflow.com}")
    private String fromEmail;

    @Value("${spring.application.name:HostFlow}")
    private String appName;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Constructor actualizado para incluir ObjectMapper
    public EmailService(TemplateEngine templateEngine, CurrencyConversionService currencyConversionService,
                        ObjectMapper objectMapper) {
        this.templateEngine = templateEngine;
        this.currencyConversionService = currencyConversionService;
        this.objectMapper = objectMapper;
    }

    // =========================================================================
    // MOTOR HÍBRIDO DE ENVÍO DE CORREOS (NÚCLEO)
    // =========================================================================

    /**
     * Envío tolerante: fallos solo en log (comportamiento histórico para auth y notificaciones).
     */
    private void sendHybridEmail(String to, String subject, String htmlContent, String plainTextContent) {
        sendHybridEmail(to, subject, htmlContent, plainTextContent, false);
    }

    /**
     * @param strict si es true, propaga {@link EmailDeliveryException} cuando el proveedor
     *               rechaza el mensaje o no hay proveedor configurado.
     */
    private void sendHybridEmail(String to, String subject, String htmlContent, String plainTextContent,
                                 boolean strict) {
        try {
            if (isBrevoConfigured()) {
                sendViaBrevo(to, subject, htmlContent != null ? htmlContent : plainTextContent, strict);
            } else if (mailSender != null) {
                sendViaGmail(to, subject, htmlContent, plainTextContent, strict);
            } else {
                log.warn("⚠️ No hay proveedor de correo configurado. El mensaje NO se envió a {}. Asunto: {}",
                        to, subject);
                log.info("🔗 Contenido simulado:\nAsunto: {}\n{}", subject,
                        plainTextContent != null ? plainTextContent : htmlContent);
                if (strict) {
                    throw new EmailDeliveryException(
                            "No hay proveedor de correo configurado. No se pudo enviar el enlace de la reserva.");
                }
            }
        } catch (EmailDeliveryException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (strict) {
                throw new EmailDeliveryException(
                        "No se pudo enviar el correo a " + to + ": " + ex.getMessage(), ex);
            }
            log.error("❌ Error inesperado enviando correo a {}: {}", to, ex.getMessage(), ex);
        }
    }

    private boolean isBrevoConfigured() {
        return brevoApiKey != null && !brevoApiKey.trim().isEmpty() && brevoApiKey.startsWith("xkeysib-");
    }

    /**
     * Envío vía API REST de Brevo. En modo estricto exige HTTP 201.
     */
    private void sendViaBrevo(String to, String subject, String content, boolean strict) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String senderEmail = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail : "noreply@hostflow.com";

            Map<String, Object> sender = Map.of("name", appName, "email", senderEmail);
            Map<String, Object> recipient = Map.of("email", to);

            Map<String, Object> requestBody = Map.of(
                    "sender", sender,
                    "to", List.of(recipient),
                    "subject", subject,
                    "htmlContent", content
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", brevoApiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                log.info("✅ Email aceptado por Brevo para: {}", to);
            } else {
                log.error("❌ Brevo rechazó el email. Status: {}, Respuesta: {}",
                        response.statusCode(), response.body());
                if (strict) {
                    throw new EmailDeliveryException(
                            "El proveedor de correo rechazó el mensaje (HTTP " + response.statusCode() + ").");
                }
            }
        } catch (EmailDeliveryException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("❌ Excepción enviando email vía Brevo a {}: {}", to, e.getMessage(), e);
            if (strict) {
                throw new EmailDeliveryException(
                        "No se pudo enviar el correo mediante Brevo: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Envío vía Gmail SMTP. En modo estricto propaga fallos del servidor SMTP.
     */
    private void sendViaGmail(String to, String subject, String htmlContent, String plainTextContent,
                              boolean strict) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            if (htmlContent != null && !htmlContent.trim().isEmpty()) {
                helper.setText(htmlContent, true);
            } else if (plainTextContent != null) {
                helper.setText(plainTextContent, false);
            }

            mailSender.send(message);
            log.info("✅ Email enviado exitosamente vía Gmail SMTP a: {}", to);
        } catch (Exception e) {
            log.error("❌ Error enviando email vía Gmail a {}: {}", to, e.getMessage(), e);
            if (strict) {
                throw new EmailDeliveryException(
                        "No se pudo enviar el correo mediante Gmail SMTP: " + e.getMessage(), e);
            }
        }
    }

    // =========================================================================
    // EQUIVALENCIAS INFORMATIVAS PARA RESERVAS CON PAYPAL
    // =========================================================================

    private void addReservationCurrencyVariables(Context context, Reservation reservation,
                                                 Accommodation accommodation) {
        BigDecimal total = reservation.getTotalAmount() != null
                ? reservation.getTotalAmount() : BigDecimal.ZERO;
        addReservationCurrencyVariables(context, accommodation, total);
    }

    private void addReservationCurrencyVariables(Context context, Accommodation accommodation,
                                                 BigDecimal amount) {
        String originalCurrency = accommodation.getBaseCurrency() != null
                ? accommodation.getBaseCurrency().name() : Currency.ARS.name();
        BigDecimal total = amount != null ? amount : BigDecimal.ZERO;
        // Informativo: billetera digital o PayPal → USD/EUR; solo transferencia → solo moneda base.
        boolean showEquivalents = shouldShowInternationalCurrencyEquivalents(accommodation);

        context.setVariable("originalCurrency", originalCurrency);
        context.setVariable("originalTotal", total);
        context.setVariable("showCurrencyEquivalents", showEquivalents);
        context.setVariable("totalUsd", showEquivalents && accommodation.getBaseCurrency() != Currency.USD
                ? currencyConversionService.convertIfAvailable(accommodation.getId(), total, Currency.USD) : null);
        context.setVariable("totalEur", showEquivalents && accommodation.getBaseCurrency() != Currency.EUR
                ? currencyConversionService.convertIfAvailable(accommodation.getId(), total, Currency.EUR) : null);
    }

    private boolean shouldShowInternationalCurrencyEquivalents(Accommodation accommodation) {
        if (accommodation.getPaymentMethods() == null) {
            return false;
        }
        return accommodation.getPaymentMethods().stream()
                .filter(this::isUsablePaymentMethod)
                .anyMatch(this::isInternationalPaymentMethod);
    }

    private boolean isInternationalPaymentMethod(AccommodationPaymentMethod method) {
        if (method.getPaymentType() == PaymentMethodType.DIGITAL_WALLET) {
            return true;
        }
        return containsPaypal(method.getDisplayName()) || containsPaypal(method.getProviderName());
    }

    private boolean isUsablePaymentMethod(AccommodationPaymentMethod method) {
        if (method == null || !method.isActive() || method.getPaymentType() == null
                || method.getDisplayName() == null || method.getDisplayName().isBlank()) return false;
        return switch (method.getPaymentType()) {
            case BANK_TRANSFER -> notBlank(method.getBankName()) && notBlank(method.getAccountHolder())
                    && (notBlank(method.getCbuCvu()) || notBlank(method.getAccountNumber()) || notBlank(method.getAlias()));
            case DIGITAL_WALLET -> notBlank(method.getProviderName())
                    && (notBlank(method.getUserIdentifier()) || notBlank(method.getWalletPhone())
                    || notBlank(method.getWalletEmail()) || notBlank(method.getAlias()));
            case CASH -> true;
            case CARD, OTHER -> notBlank(method.getInstructions()) || notBlank(method.getProviderName())
                    || notBlank(method.getAccountNumber());
        };
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private boolean containsPaypal(String value) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains("paypal");
    }

    private void appendEquivalent(StringBuilder amounts, Accommodation accommodation,
                                  BigDecimal amount, Currency currency) {
        if (accommodation.getBaseCurrency() == currency) return;
        BigDecimal converted = currencyConversionService.convertIfAvailable(accommodation.getId(), amount, currency);
        if (converted != null) {
            amounts.append(String.format(java.util.Locale.ROOT, "\nEquivalente informativo: %s %s",
                    formatAmount(converted), currency.name()));
        }
    }


    /**
     * Variables de plantilla para el desglose de servicios adicionales (solo visualización).
     */
    private void addAdditionalServicesVariables(Context context, Reservation reservation) {
        var services = reservation.getAdditionalServices() == null
                ? java.util.List.of()
                : reservation.getAdditionalServices();
        context.setVariable("additionalServices", services);
        context.setVariable("hasAdditionalServices", !services.isEmpty());
        context.setVariable("additionalServicesTotal",
                reservation.getAdditionalServicesTotal() != null
                        ? reservation.getAdditionalServicesTotal() : BigDecimal.ZERO);
        context.setVariable("accommodationSubtotal",
                reservation.getSubtotal() != null ? reservation.getSubtotal() : BigDecimal.ZERO);
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "—" : String.format(java.util.Locale.ROOT, "%,.2f", amount);
    }

    // =========================================================================
    // MÉTODOS: AUTENTICACIÓN Y SEGURIDAD
    // =========================================================================

    public void sendPasswordResetEmail(String toEmail, String resetCode, int expirationMin) {
        String resetLink = "https://hostflow-lilac.vercel.app/reset-password?token=" + resetCode;

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

    /**
     * Envío del enlace de onboarding (creación o renovación).
     * <p><strong>Modo estricto:</strong> si el proveedor falla, lanza
     * {@link EmailDeliveryException} para que la transacción de negocio se revierta.</p>
     */
    public void sendReservationConfirmationEmail(String toEmail, String guestName, String confirmationCode,
                                                 String confirmationUrl, Accommodation accommodation,
                                                 String accommodationName, LocalDate checkIn,
                                                 LocalDate checkOut, BigDecimal totalAmount,
                                                 int expirationHours) {
        Context context = new Context();
        context.setVariable("guestName", guestName);
        context.setVariable("confirmationCode", confirmationCode);
        context.setVariable("confirmationUrl", confirmationUrl);
        context.setVariable("accommodation", accommodation);
        context.setVariable("accommodationName", accommodationName);
        context.setVariable("checkIn", checkIn);
        context.setVariable("checkOut", checkOut);
        context.setVariable("expirationHours", expirationHours);
        context.setVariable("watermark", "Powered by HostFlow");
        addReservationCurrencyVariables(context, accommodation, totalAmount);

        String htmlContent = templateEngine.process("emails/reservation-link-guest", context);
        String plainText = String.format(java.util.Locale.ROOT, """
            ¡Hola %s!
            Se ha creado una reserva para ti en %s.
            Código: %s | Check-in: %s | Check-out: %s
            Importe inicial: %s %s
            Para completar tu reserva, ingresa aquí: %s
            Este enlace vence en %d horas.
            """, guestName, accommodationName, confirmationCode,
                checkIn.format(DATE_FORMATTER), checkOut.format(DATE_FORMATTER),
                formatAmount(totalAmount), accommodation.getBaseCurrency() != null
                        ? accommodation.getBaseCurrency().name() : Currency.ARS.name(),
                confirmationUrl, expirationHours);

        // Estricto: creación y renovación no deben confirmar éxito si el correo no salió.
        sendHybridEmail(toEmail, "🏡 " + accommodationName + " - Completa tu reserva " + confirmationCode,
                htmlContent, plainText, true);
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
        context.setVariable("paymentConfirmed", true);
        addAdditionalServicesVariables(context, reservation);
        addReservationCurrencyVariables(context, reservation, accommodation);

        String htmlContent = templateEngine.process("emails/reservation-confirmation-guest", context);

        sendHybridEmail(
                reservation.getGuest().getEmail(),
                "✅ " + appName + " - Pago recibido y reserva confirmada " + reservation.getConfirmationCode(),
                htmlContent,
                null
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
        context.setVariable("paymentConfirmed", true);
        addAdditionalServicesVariables(context, reservation);
        addReservationCurrencyVariables(context, reservation, accommodation);

        String htmlContent = templateEngine.process("emails/reservation-notification-owner", context);

        sendHybridEmail(
                accommodation.getOwner().getEmail(),
                "💰 " + appName + " - Pago recibido para la reserva " + reservation.getConfirmationCode(),
                htmlContent,
                null
        );
    }

    /**
     * Aviso de seguridad enviado al correo anterior cuando cambia el contacto principal del huésped.
     * Es tolerante: un fallo de correo nunca revierte el onboarding ni la reserva.
     */
    public void sendGuestEmailChangedNotification(String previousEmail, String guestName, String maskedNewEmail) {
        if (previousEmail == null || previousEmail.isBlank()) {
            log.warn("No se envió aviso de cambio de correo: el huésped no tenía correo anterior registrado.");
            return;
        }
        String displayName = guestName == null || guestName.isBlank() ? "Huésped" : guestName.trim();
        String displayEmail = maskedNewEmail == null || maskedNewEmail.isBlank()
                ? "No disponible" : maskedNewEmail.trim();
        String plainText = "Hola " + displayName + ",\n\n"
                + "El correo electrónico asociado a tu perfil de huésped en HostFlow fue actualizado.\n\n"
                + "Nuevo correo registrado: " + displayEmail + "\n\n"
                + "Si realizaste este cambio, no necesitas hacer nada. Si no reconoces esta modificación, "
                + "comunícate con el propietario del alojamiento o con el equipo de soporte de HostFlow.\n\n"
                + "Por seguridad, no compartas contraseñas ni códigos de verificación.\n\n"
                + "Este es un aviso automático de seguridad de " + appName + ".";

        Context context = new Context();
        context.setVariable("guestName", displayName);
        context.setVariable("maskedNewEmail", displayEmail);
        context.setVariable("appName", appName);
        String htmlContent = templateEngine.process("emails/guest-email-changed", context);
        sendHybridEmail(previousEmail, "Aviso de seguridad: tu correo de HostFlow fue actualizado",
                htmlContent, plainText);
    }

    public void sendReservationCancelledEmail(String toEmail, String guestName, String confirmationCode,
                                              String accommodationName, String reason) {
        String displayName = guestName == null || guestName.isBlank() ? "Huésped" : guestName.trim();
        String displayCode = confirmationCode == null || confirmationCode.isBlank()
                ? "No disponible" : confirmationCode.trim();
        String displayAccommodation = accommodationName == null || accommodationName.isBlank()
                ? "el alojamiento" : accommodationName.trim();
        String displayReason = reason == null || reason.isBlank() ? "No especificado" : reason.trim();
        String plainText = "Hola " + displayName + ",\n\n"
                + "Te informamos que tu reserva " + displayCode + " en " + displayAccommodation
                + " ha sido cancelada.\n\nMotivo: " + displayReason + "\n\n"
                + "Si necesitas aclarar alguna duda, comunícate directamente con el alojamiento.\n\n"
                + "Este mensaje es una notificación automática de " + appName + ".";

        Context context = new Context();
        context.setVariable("guestName", displayName);
        context.setVariable("confirmationCode", displayCode);
        context.setVariable("accommodationName", displayAccommodation);
        context.setVariable("reason", displayReason);
        context.setVariable("appName", appName);
        String htmlContent = templateEngine.process("emails/reservation-cancelled", context);
        sendHybridEmail(toEmail, "Reserva cancelada " + displayCode + " - " + displayAccommodation,
                htmlContent, plainText);
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