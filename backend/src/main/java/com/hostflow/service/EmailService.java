package com.hostflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Servicio centralizado para el envío de correos electrónicos en HostFlow.
 *
 * <p>Utiliza Spring Boot Mail para enviar notificaciones transaccionales, incluyendo:</p>
 * <ul>
 *   <li>Recuperación y cambio de contraseñas.</li>
 *   <li>Verificación de cuentas de usuario.</li>
 *   <li>Creación, confirmación y cancelación de reservas.</li>
 *   <li>🆕 Registro de nuevos métodos de pago por parte del propietario.</li>
 * </ul>
 *
 * <p><strong>Política de resiliencia:</strong> Todos los métodos de envío están envueltos
 * en bloques {@code try-catch}. Si el envío de email falla (ej. servidor SMTP caído),
 * se registra el error en los logs pero NO se lanza una excepción. Esto evita que fallos
 * externos en el servicio de correo interrumpan el flujo principal de negocio
 * (como la creación de una reserva o el registro de un usuario).</p>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@hostflow.com}")
    private String fromEmail;

    @Value("${spring.application.name:HostFlow}")
    private String appName;

    // Formateador reutilizable para fechas en los correos (ej: "15/12/2026 a las 14:00")
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    // =========================================================================
    // MÉTODOS EXISTENTES: AUTENTICACIÓN Y SEGURIDAD
    // =========================================================================

    /**
     * Envía un correo electrónico con el código de recuperación de contraseña.
     *
     * @param toEmail       correo del destinatario
     * @param resetCode     código numérico de recuperación
     * @param expirationMin tiempo de expiración en minutos
     */
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
            log.error("❌ Error al enviar email de recuperación a {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Envía un correo electrónico de confirmación tras un cambio de contraseña.
     *
     * @param toEmail correo del destinatario
     */
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
            log.error("❌ Error al enviar email de confirmación de cambio a {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Envía un correo electrónico de verificación de cuenta tras el registro.
     *
     * @param toEmail           correo del destinatario
     * @param verificationToken token UUID de verificación (para logs/auditoría)
     * @param verificationUrl   URL completa del enlace de verificación
     * @param expirationMin     tiempo de expiración en minutos
     */
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
                • Si no solicitaste este registro, puedes ignorar este correo.
                
                Una vez verificado, podrás iniciar sesión con tus credenciales.
                
                Saludos,
                El equipo de %s
                """, appName, verificationUrl, expirationMin, appName);

            message.setText(emailBody);
            mailSender.send(message);
            log.info("✅ Email de verificación de cuenta enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de verificación a {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Envía un correo electrónico con un nuevo enlace de verificación.
     *
     * @param toEmail           correo del destinatario
     * @param verificationToken nuevo token UUID (para logs/auditoría)
     * @param verificationUrl   URL completa del nuevo enlace
     * @param expirationMin     tiempo de expiración en minutos
     */
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
                • Es de un solo uso.
                
                Si no solicitaste este reenvío, puedes ignorar este correo.
                
                Saludos,
                El equipo de %s
                """, appName, verificationUrl, expirationMin, appName);

            message.setText(emailBody);
            mailSender.send(message);
            log.info("✅ Email de reenvío de verificación enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de reenvío de verificación a {}: {}", toEmail, e.getMessage());
        }
    }

    // =========================================================================
    // MÉTODOS: SISTEMA DE RESERVAS
    // =========================================================================

    /**
     * Envía un correo al huésped con el enlace para confirmar una reserva creada por el owner.
     *
     * @param toEmail           correo del huésped
     * @param guestName         nombre completo del huésped
     * @param confirmationCode  código único de la reserva (ej: RES-A1B2C3)
     * @param confirmationUrl   URL completa del enlace de confirmación
     * @param accommodationName nombre del alojamiento
     * @param checkIn           fecha y hora de ingreso
     * @param checkOut          fecha y hora de salida
     * @param totalAmount       monto total a pagar
     * @param expirationHours   horas de validez del enlace
     */
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
                Código de confirmación: %s
                Alojamiento: %s
                Check-in: %s
                Check-out: %s
                Monto total: $%,.2f
                ═══════════════════════════════════════════════════════
                
                Para confirmar tu reserva y asegurar tu estadía, haz clic en el siguiente enlace:
                
                ╔══════════════════════════════════════════════════════════╗
                ║  ENLACE DE CONFIRMACIÓN:                                 ║
                ║  %s  ║
                ╚══════════════════════════════════════════════════════════╝
                
                ⚠️ Importante:
                • Este enlace expirará en %d horas.
                • Es de un solo uso.
                • Si no solicitaste esta reserva, puedes ignorar este correo.
                
                Una vez confirmada, recibirás un correo de confirmación final.
                
                Saludos,
                El equipo de %s
                """,
                    guestName,
                    accommodationName,
                    confirmationCode,
                    accommodationName,
                    checkIn.format(DATE_FORMATTER),
                    checkOut.format(DATE_FORMATTER),
                    totalAmount,
                    confirmationUrl,
                    expirationHours,
                    appName
            );

            message.setText(emailBody);
            mailSender.send(message);
            log.info("✅ Email de solicitud de confirmación de reserva enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de confirmación de reserva a {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Envía un correo de confirmación final al huésped tras confirmar la reserva.
     *
     * @param toEmail           correo del huésped
     * @param guestName         nombre completo del huésped
     * @param confirmationCode  código único de la reserva
     * @param accommodationName nombre del alojamiento
     * @param checkIn           fecha y hora de ingreso
     * @param checkOut          fecha y hora de salida
     * @param totalAmount       monto total pagado/reservado
     */
    public void sendReservationConfirmedEmail(String toEmail, String guestName, String confirmationCode,
                                              String accommodationName, LocalDateTime checkIn,
                                              LocalDateTime checkOut, BigDecimal totalAmount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("✅ " + appName + " - ¡Reserva confirmada! " + confirmationCode);

            String emailBody = String.format("""
                ¡Hola %s!
                
                ¡Excelente noticia! Tu reserva ha sido confirmada exitosamente.
                
                ═══════════════════════════════════════════════════════
                📋 DETALLES DE TU RESERVA CONFIRMADA
                ═══════════════════════════════════════════════════════
                Código de confirmación: %s
                Alojamiento: %s
                Check-in: %s
                Check-out: %s
                Monto total: $%,.2f
                ═══════════════════════════════════════════════════════
                
                📞 Contacto del alojamiento:
                Si tienes alguna pregunta o necesitas asistencia antes de tu llegada,
                puedes contactar directamente al propietario a través de la plataforma.
                
                ¡Gracias por elegir %s!
                
                Saludos,
                El equipo de %s
                """,
                    guestName,
                    confirmationCode,
                    accommodationName,
                    checkIn.format(DATE_FORMATTER),
                    checkOut.format(DATE_FORMATTER),
                    totalAmount,
                    appName,
                    appName
            );

            message.setText(emailBody);
            mailSender.send(message);
            log.info("✅ Email de reserva confirmada enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de reserva confirmada a {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Envía un correo al huésped notificando la cancelación de su reserva.
     *
     * @param toEmail           correo del huésped
     * @param guestName         nombre completo del huésped
     * @param confirmationCode  código único de la reserva
     * @param accommodationName nombre del alojamiento
     * @param reason            motivo de la cancelación (puede ser null o vacío)
     */
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

            // Agregar motivo solo si se proporcionó uno
            if (reason != null && !reason.isBlank()) {
                emailBody.append(String.format("""
                    
                    Motivo de la cancelación:
                    %s
                    """, reason));
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
            log.error("❌ Error al enviar email de cancelación de reserva a {}: {}", toEmail, e.getMessage());
        }
    }

    // =========================================================================
    // 🆕 MÉTODOS NUEVOS: MÉTODOS DE PAGO DEL ALOJAMIENTO
    // =========================================================================

    /**
     * 🆕 Envía un correo electrónico de confirmación al propietario cuando
     * se registra un nuevo método de pago en su alojamiento.
     *
     * <p>Este correo tiene fines de auditoría y seguridad, permitiendo al
     * propietario saber que se ha configurado una nueva forma de recibir
     * pagos en su alojamiento.</p>
     *
     * @param toEmail           el correo electrónico del propietario
     * @param ownerName         el nombre completo del propietario
     * @param accommodationName el nombre del alojamiento
     * @param paymentMethodName el nombre descriptivo del método de pago
     * @param paymentType       el tipo de método de pago (ej: BANK_TRANSFER)
     * @param isDefault         indica si es el método predeterminado
     */
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

                    Este método estará disponible para que los huéspedes
                    realicen sus reservas y te consignen los pagos.

                    Puedes gestionar tus métodos de pago en cualquier momento
                    desde el panel de administración de tu alojamiento.

                    ⚠️ Si no realizaste este cambio, contacta inmediatamente
                    con nuestro equipo de soporte.

                    Saludos,
                    El equipo de %s
                    """,
                    ownerName,
                    accommodationName,
                    appName,
                    paymentMethodName,
                    paymentType,
                    defaultBadge,
                    appName);

            message.setText(emailBody);
            mailSender.send(message);
            log.info("✅ Email de confirmación de método de pago enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de método de pago a {}: {}", toEmail, e.getMessage());
            // No lanzamos excepción para no bloquear el flujo si el email falla
        }
    }
}