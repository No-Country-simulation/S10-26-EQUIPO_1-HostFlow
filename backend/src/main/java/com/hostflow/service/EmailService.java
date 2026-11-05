package com.hostflow.service;

import com.hostflow.model.Accommodation;
import com.hostflow.model.Reservation;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
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
 * <p>Utiliza Spring Boot Mail y Thymeleaf para enviar notificaciones transaccionales, incluyendo:</p>
 * <ul>
 *   <li>Recuperación y cambio de contraseñas (Texto plano).</li>
 *   <li>Verificación de cuentas de usuario (Texto plano).</li>
 *   <li>Creación, confirmación (HTML personalizado con branding, alergias y lógica de monedas) y cancelación de reservas.</li>
 *   <li>Registro de nuevos métodos de pago por parte del propietario.</li>
 * </ul>
 *
 * <p><strong>Política de resiliencia:</strong> Todos los métodos de envío están envueltos
 * en bloques {@code try-catch}. Si el envío de email falla (ej. servidor SMTP caído),
 * se registra el error en los logs pero NO se lanza una excepción. Esto evita que fallos
 * externos en el servicio de correo interrumpan o reviertan el flujo principal de negocio.</p>
 *
 * @author HostFlow Team
 * @version 3.3.0 (Corregido manejo de enum Currency y robustez en conversión de monedas)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ExchangeRateProviderService exchangeRateService;

    @Value("${spring.mail.username:no-reply@hostflow.com}")
    private String fromEmail;

    @Value("${spring.application.name:HostFlow}")
    private String appName;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    // =========================================================================
    // MÉTODOS AUXILIARES PARA LÓGICA DE MONEDAS
    // =========================================================================

    /**
     * Determina si el alojamiento opera exclusivamente con billeteras digitales argentinas.
     *
     * @param accommodation El alojamiento a evaluar.
     * @return true si solo debe mostrarse en ARS, false si deben mostrarse las 3 monedas.
     */
    private boolean isArgentinaOnly(Accommodation accommodation) {
        // ✅ CORRECCIÓN: Usamos .toString() para manejar tanto String como Enum Currency sin errores de compilación
        boolean isArsCurrency = accommodation.getBaseCurrency() != null &&
                accommodation.getBaseCurrency().toString().equalsIgnoreCase("ARS");

        // Si tu entidad Accommodation tiene una lista de métodos de pago, descomenta y adapta esto:
        /*
        boolean hasArgentinaWallets = accommodation.getPaymentMethods() != null &&
            accommodation.getPaymentMethods().stream().anyMatch(pm ->
                pm.getProviderName() != null && (pm.getProviderName().toLowerCase().contains("mercado pago") ||
                                                 pm.getProviderName().toLowerCase().contains("uala") ||
                                                 pm.getProviderName().toLowerCase().contains("argentina"))
            );
        return isArsCurrency && hasArgentinaWallets;
        */

        // Fallback seguro: si la moneda base es ARS, lo tratamos como caso local.
        return isArsCurrency;
    }

    /**
     * Convierte un monto de una moneda a otra utilizando el servicio de tasas de cambio.
     */
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
    // MÉTODOS: AUTENTICACIÓN Y SEGURIDAD (Texto Plano)
    // =========================================================================

    public void sendPasswordResetEmail(String toEmail, String resetCode, int expirationMin) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("🔐 " + appName + " - Código de recuperación de contraseña");

            String emailBody = String.format("""
                Hola,
                
                Hemos recibido una solicitud para restablecer tu contraseña en %s.
                
                Tu código de recuperación es:
                ╔══════════════════════╗
                ║   CÓDIGO: %s   ║
                ╚══════════════════════╝
                
                Este código expirará en %d minutos.
                
                Si no solicitaste este cambio, puedes ignorar este correo de forma segura.
                
                Saludos,
                El equipo de %s
                """, appName, resetCode, expirationMin, appName);

            message.setText(emailBody);
            mailSender.send(message);
            log.info("✅ Email de recuperación enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de recuperación a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendPasswordChangedConfirmation(String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("✅ " + appName + " - Contraseña actualizada exitosamente");

            String emailBody = String.format("""
                Hola,
                
                Te confirmamos que tu contraseña en %s ha sido actualizada exitosamente.
                
                Si no realizaste este cambio, contacta inmediatamente con nuestro equipo de soporte.
                
                Saludos,
                El equipo de %s
                """, appName, appName);

            message.setText(emailBody);
            mailSender.send(message);
            log.info("✅ Email de confirmación de cambio de contraseña enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de confirmación de cambio a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendEmailVerificationEmail(String toEmail, String verificationToken,
                                           String verificationUrl, int expirationMin) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("✅ " + appName + " - Verifica tu correo electrónico");

            String emailBody = String.format("""
                ¡Bienvenido a %s!
                
                Gracias por registrarte en nuestra plataforma.
                
                Para completar tu registro y activar tu cuenta, 
                por favor verifica tu correo electrónico haciendo clic en el siguiente enlace:
                
                ╔══════════════════════════════════════════════════════════╗
                ║  ENLACE DE VERIFICACIÓN:                                 ║
                ║  %s  ║
                ╚══════════════════════════════════════════════════════════╝
                
                ⚠️ Importante:
                • Este enlace expirará en %d minutos.
                • Es de un solo uso.
                
                Saludos,
                El equipo de %s
                """, appName, verificationUrl, expirationMin, appName);

            message.setText(emailBody);
            mailSender.send(message);
            log.info("✅ Email de verificación de cuenta enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de verificación a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendEmailVerificationResendEmail(String toEmail, String verificationToken,
                                                 String verificationUrl, int expirationMin) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("🔄 " + appName + " - Nuevo enlace de verificación");

            String emailBody = String.format("""
                Hola,
                
                Hemos recibido una solicitud para reenviar el enlace de verificación
                de tu cuenta en %s.
                
                Tu nuevo enlace de verificación es:
                
                ╔══════════════════════════════════════════════════════════╗
                ║  NUEVO ENLACE DE VERIFICACIÓN:                           ║
                ║  %s  ║
                ╚══════════════════════════════════════════════════════════╝
                
                ⚠️ Importante:
                • Este enlace expirará en %d minutos.
                • El enlace anterior ha sido invalidado automáticamente.
                
                Saludos,
                El equipo de %s
                """, appName, verificationUrl, expirationMin, appName);

            message.setText(emailBody);
            mailSender.send(message);
            log.info("✅ Email de reenvío de verificación enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de reenvío de verificación a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    // =========================================================================
    // MÉTODOS: SISTEMA DE RESERVAS (Texto Plano y HTML con Thymeleaf)
    // =========================================================================

    public void sendReservationConfirmationEmail(String toEmail, String guestName, String confirmationCode,
                                                 String confirmationUrl, String accommodationName,
                                                 LocalDateTime checkIn, LocalDateTime checkOut,
                                                 BigDecimal totalAmount, int expirationHours) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("🏨 " + appName + " - Confirma tu reserva " + confirmationCode);

            String emailBody = String.format("""
                ¡Hola %s!
                
                Un propietario ha creado una reserva para ti en %s.
                
                ═══════════════════════════════════════════════════════
                📋 DETALLES DE LA RESERVA
                ═══════════════════════════════════════════════════════
                Código: %s | Alojamiento: %s
                Check-in: %s | Check-out: %s
                Monto total: $%,.2f
                ═══════════════════════════════════════════════════════
                
                Para confirmar tu reserva, haz clic en el siguiente enlace:
                %s
                
                ⚠️ Este enlace expirará en %d horas y es de un solo uso.
                
                Saludos,
                El equipo de %s
                """,
                    guestName, accommodationName, confirmationCode, accommodationName,
                    checkIn.format(DATE_FORMATTER), checkOut.format(DATE_FORMATTER),
                    totalAmount, confirmationUrl, expirationHours, appName
            );

            message.setText(emailBody);
            mailSender.send(message);
            log.info("✅ Email de solicitud de confirmación de reserva enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de confirmación de reserva a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * 🆕 Envía un correo HTML personalizado al HUÉSPED tras confirmar la reserva.
     * Incluye branding, alergias y lógica de conversión de monedas.
     */
    public void sendReservationConfirmedGuestEmail(Reservation reservation, Accommodation accommodation,
                                                   String guestName, String reservationUrl) {
        try {
            if (reservation.getGuest() == null || reservation.getGuest().getEmail() == null) {
                log.warn("⚠️ No se puede enviar email al huésped: datos de huésped o email nulos.");
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(reservation.getGuest().getEmail());
            helper.setSubject("✅ " + appName + " - ¡Reserva confirmada! " + reservation.getConfirmationCode());

            Context context = new Context();
            context.setVariable("reservation", reservation);
            context.setVariable("accommodation", accommodation);
            context.setVariable("guestName", guestName);
            context.setVariable("watermark", "Plataforma de Reservas HostFlow");
            context.setVariable("reservationUrl", reservationUrl);

            // 🆕 Lógica de Monedas
            boolean argentinaOnly = isArgentinaOnly(accommodation);
            context.setVariable("isArgentinaOnly", argentinaOnly);

            // ✅ CORRECCIÓN: Usamos la moneda base del alojamiento como referencia principal
            String originalCurrency = accommodation.getBaseCurrency() != null ?
                    accommodation.getBaseCurrency().toString().toUpperCase() : "ARS";
            BigDecimal total = reservation.getTotalAmount() != null ? reservation.getTotalAmount() : BigDecimal.ZERO;

            context.setVariable("totalArs", convertCurrency(total, originalCurrency, "ARS"));
            context.setVariable("totalUsd", convertCurrency(total, originalCurrency, "USD"));
            context.setVariable("totalEur", convertCurrency(total, originalCurrency, "EUR"));
            context.setVariable("originalCurrency", originalCurrency);

            String htmlContent = templateEngine.process("emails/reservation-confirmation-guest", context);
            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("✅ Email HTML de reserva confirmada enviado al huésped: {}", reservation.getGuest().getEmail());
        } catch (Exception e) {
            log.error("❌ Error al enviar email HTML de reserva confirmada al huésped: {}", e.getMessage(), e);
        }
    }

    /**
     * 🆕 Envía un correo HTML personalizado al PROPIETARIO notificando una nueva reserva confirmada.
     * Incluye branding, alergias y lógica de conversión de monedas.
     */
    public void sendReservationConfirmedOwnerEmail(Reservation reservation, Accommodation accommodation,
                                                   String ownerName, String reservationUrl) {
        try {
            if (accommodation.getOwner() == null || accommodation.getOwner().getEmail() == null) {
                log.error("❌ No se puede enviar email al propietario: el alojamiento (ID: {}) no tiene un propietario válido o el email es nulo.", accommodation.getId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(accommodation.getOwner().getEmail());
            helper.setSubject("🔔 " + appName + " - ¡Nueva reserva confirmada! " + reservation.getConfirmationCode());

            Context context = new Context();
            context.setVariable("reservation", reservation);
            context.setVariable("accommodation", accommodation);
            context.setVariable("ownerName", ownerName);
            context.setVariable("watermark", "Panel de Administración HostFlow");
            context.setVariable("reservationUrl", reservationUrl);

            // 🆕 Lógica de Monedas
            boolean argentinaOnly = isArgentinaOnly(accommodation);
            context.setVariable("isArgentinaOnly", argentinaOnly);

            // ✅ CORRECCIÓN: Usamos la moneda base del alojamiento como referencia principal
            String originalCurrency = accommodation.getBaseCurrency() != null ?
                    accommodation.getBaseCurrency().toString().toUpperCase() : "ARS";
            BigDecimal total = reservation.getTotalAmount() != null ? reservation.getTotalAmount() : BigDecimal.ZERO;

            context.setVariable("totalArs", convertCurrency(total, originalCurrency, "ARS"));
            context.setVariable("totalUsd", convertCurrency(total, originalCurrency, "USD"));
            context.setVariable("totalEur", convertCurrency(total, originalCurrency, "EUR"));
            context.setVariable("originalCurrency", originalCurrency);

            String htmlContent = templateEngine.process("emails/reservation-notification-owner", context);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("✅ Email HTML de nueva reserva enviado al propietario: {}", accommodation.getOwner().getEmail());
        } catch (Exception e) {
            log.error("❌ Error al enviar email HTML de nueva reserva al propietario: {}", e.getMessage(), e);
        }
    }

    public void sendReservationCancelledEmail(String toEmail, String guestName, String confirmationCode,
                                              String accommodationName, String reason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("❌ " + appName + " - Reserva cancelada " + confirmationCode);

            StringBuilder emailBody = new StringBuilder();
            emailBody.append(String.format("""
                ¡Hola %s!
                
                Lamentamos informarte que tu reserva ha sido cancelada.
                
                ═══════════════════════════════════════════════════════
                📋 DETALLES DE LA RESERVA CANCELADA
                ═══════════════════════════════════════════════════════
                Código de confirmación: %s
                Alojamiento: %s
                ═══════════════════════════════════════════════════════
                """, guestName, confirmationCode, accommodationName));

            if (reason != null && !reason.isBlank()) {
                emailBody.append(String.format("\nMotivo de la cancelación:\n%s\n", reason));
            }

            emailBody.append(String.format("""
                
                Si tienes alguna pregunta o deseas realizar una nueva reserva en el futuro,
                no dudes en contactarnos a través de la plataforma.
                
                Saludos,
                El equipo de %s
                """, appName));

            message.setText(emailBody.toString());
            mailSender.send(message);
            log.info("✅ Email de cancelación de reserva enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de cancelación de reserva a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    // =========================================================================
    // MÉTODOS: MÉTODOS DE PAGO DEL ALOJAMIENTO
    // =========================================================================

    public void sendPaymentMethodCreatedEmail(String toEmail, String ownerName,
                                              String accommodationName,
                                              String paymentMethodName,
                                              String paymentType,
                                              boolean isDefault) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("💳 " + appName + " - Nuevo método de pago registrado");

            String defaultBadge = isDefault ? "⭐ SÍ (este es tu método predeterminado)" : "No";

            String emailBody = String.format("""
                    Hola %s,

                    Te informamos que se ha registrado un nuevo método de pago
                    en tu alojamiento "%s" dentro de la plataforma %s.

                    ╔══════════════════════════════════════════════════════════╗
                    ║  DETALLES DEL MÉTODO DE PAGO                             ║
                    ╠══════════════════════════════════════════════════════════╣
                    ║  Nombre:         %s
                    ║  Tipo:           %s
                    ║  Predeterminado: %s
                    ╚══════════════════════════════════════════════════════════╝

                    Puedes gestionar tus métodos de pago en cualquier momento
                    desde el panel de administración de tu alojamiento.

                    Saludos,
                    El equipo de %s
                    """,
                    ownerName, accommodationName, appName,
                    paymentMethodName, paymentType, defaultBadge, appName);

            message.setText(emailBody);
            mailSender.send(message);
            log.info("✅ Email de confirmación de método de pago enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de método de pago a {}: {}", toEmail, e.getMessage(), e);
        }
    }
}