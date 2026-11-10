package com.hostflow.service;

import com.hostflow.model.Accommodation;
import com.hostflow.model.Owner;
import com.hostflow.model.Reservation;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * Envía confirmación de reserva al HUÉSPED con la marca del alojamiento.
     */
    public void sendGuestConfirmation(Reservation reservation, Owner owner, Accommodation accommodation) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("no-reply@hostflow.com");
            helper.setTo(reservation.getGuest().getEmail());
            helper.setSubject("✅ Confirmación de tu reserva en " + accommodation.getName());

            // Preparar variables para la plantilla Thymeleaf
            Context context = new Context();
            context.setVariable("guestName", reservation.getGuest().getFullName());
            context.setVariable("accommodation", accommodation);
            context.setVariable("reservation", reservation);
            context.setVariable("watermark", "Powered by HostFlow");

            String htmlContent = templateEngine.process("emails/reservation-confirmation-guest", context);
            helper.setText(htmlContent, true); // true = es HTML

            mailSender.send(message);
            log.info("✅ Email de confirmación enviado al huésped: {}", reservation.getGuest().getEmail());
        } catch (MessagingException e) {
            log.error("❌ Error al enviar email al huésped: {}", e.getMessage());
        }
    }

    /**
     * Envía notificación de nueva reserva al PROPIETARIO.
     */
    public void sendOwnerNotification(Reservation reservation, Owner owner, Accommodation accommodation) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("no-reply@hostflow.com");
            helper.setTo(owner.getEmail());
            helper.setSubject("🔔 Nueva reserva recibida: " + accommodation.getName());

            Context context = new Context();
            context.setVariable("ownerName", owner.getFullName());
            context.setVariable("accommodation", accommodation);
            context.setVariable("reservation", reservation);
            context.setVariable("watermark", "Panel de Administración HostFlow");

            String htmlContent = templateEngine.process("emails/reservation-notification-owner.html", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ Email de notificación enviado al propietario: {}", owner.getEmail());
        } catch (MessagingException e) {
            log.error("❌ Error al enviar email al propietario: {}", e.getMessage());
        }
    }
}