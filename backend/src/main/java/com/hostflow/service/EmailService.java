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
 * incluyendo códigos de recuperación de contraseña.</p>
 *
 * @author HostFlow Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    // Se agrega ":no-reply@hostflow.com" como valor por defecto seguro
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
                    ╚══════════════════════╝
                    
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
}