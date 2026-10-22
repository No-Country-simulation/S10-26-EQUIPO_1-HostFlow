package com.hostflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Servicio para el envío de correos electrónicos en HostFlow.
 *
 * <p>Utiliza Spring Boot Mail para enviar notificaciones por email,
 * incluyendo códigos de recuperación de contraseña y verificación de email.</p>
 *
 * @author HostFlow Team
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

    /**
     * Envía un correo electrónico con el código de recuperación de contraseña.
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
                ══════════════════════╝
                
                Este código expirará en %d minutos.
                
                Si no solicitaste este cambio, puedes ignorar este correo.
                
                Saludos,
                El equipo de %s
                """, appName, resetCode, expirationMin, appName);

            message.setText(emailBody);
            mailSender.send(message);
            log.info("✅ Email de recuperación enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de recuperación a {}: {}", toEmail, e.getMessage());
            // No lanzamos excepción para no bloquear el flujo si el email falla en desarrollo
        }
    }

    /**
     * Envía un correo electrónico de confirmación de cambio de contraseña.
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
                
                Si no realizaste este cambio, contacta inmediatamente con nuestro soporte.
                
                Saludos,
                El equipo de %s
                """, appName, appName);

            message.setText(emailBody);
            mailSender.send(message);
            log.info("✅ Email de confirmación enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de confirmación a {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * 🆕 Envía un correo electrónico de verificación de cuenta.
     *
     * <p>Este correo se envía automáticamente después del registro exitoso
     * del usuario. Contiene un enlace único que el usuario debe hacer clic
     * para activar su cuenta y poder iniciar sesión.</p>
     *
     * @param toEmail el correo electrónico del destinatario
     * @param verificationToken el token UUID de verificación
     * @param verificationUrl la URL completa del enlace de verificación
     * @param expirationMin el tiempo de expiración en minutos
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
                  ENLACE DE VERIFICACIÓN:                                 ║
                ║  %s  
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
            log.info("✅ Email de verificación enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Error al enviar email de verificación a {}: {}", toEmail, e.getMessage());
            // No lanzamos excepción para no bloquear el flujo si el email falla en desarrollo
        }
    }

    /**
     * 🆕 Envía un correo electrónico para reenviar el enlace de verificación.
     *
     * <p>Se utiliza cuando el usuario solicita un nuevo enlace de verificación
     * porque el anterior expiró o no lo recibió.</p>
     *
     * @param toEmail el correo electrónico del destinatario
     * @param verificationToken el nuevo token UUID de verificación
     * @param verificationUrl la URL completa del nuevo enlace de verificación
     * @param expirationMin el tiempo de expiración en minutos
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
                • El enlace anterior ha sido invalidado.
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
}