package com.hostflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostflow.enums.Currency;
import com.hostflow.exception.EmailDeliveryException;
import com.hostflow.model.Accommodation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica la política dual de correo de reservas (Fase A):
 * creación/renovación estrictas; cancelación y pago tolerantes.
 */
class EmailServiceReservationPolicyTest {

    private final TemplateEngine templateEngine = mock(TemplateEngine.class);
    private final CurrencyConversionService currencyConversionService = mock(CurrencyConversionService.class);
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final MimeMessage mimeMessage = mock(MimeMessage.class);

    private EmailService emailService;
    private Accommodation accommodation;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(templateEngine, currencyConversionService, new ObjectMapper());
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
        ReflectionTestUtils.setField(emailService, "brevoApiKey", "");
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@hostflow.com");
        ReflectionTestUtils.setField(emailService, "appName", "HostFlow");

        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        accommodation = new Accommodation();
        accommodation.setName("Casa Test");
        accommodation.setBaseCurrency(Currency.ARS);
    }

    @Test
    void createLinkEmailWithoutProviderThrowsInStrictMode() {
        ReflectionTestUtils.setField(emailService, "mailSender", null);

        assertThrows(EmailDeliveryException.class, () ->
                emailService.sendReservationConfirmationEmail(
                        "guest@example.com", "Ana", "HF-TEST",
                        "https://app.example/confirm?token=abc",
                        accommodation, accommodation.getName(),
                        LocalDate.now().plusDays(2), LocalDate.now().plusDays(5),
                        new BigDecimal("30000"), 24));
    }

    @Test
    void createLinkEmailPropagatesSmtpFailure() {
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(EmailDeliveryException.class, () ->
                emailService.sendReservationConfirmationEmail(
                        "guest@example.com", "Ana", "HF-TEST",
                        "https://app.example/confirm?token=abc",
                        accommodation, accommodation.getName(),
                        LocalDate.now().plusDays(2), LocalDate.now().plusDays(5),
                        new BigDecimal("30000"), 24));
    }

    @Test
    void createLinkEmailSucceedsWhenSmtpAccepts() {
        assertDoesNotThrow(() ->
                emailService.sendReservationConfirmationEmail(
                        "guest@example.com", "Ana", "HF-TEST",
                        "https://app.example/confirm?token=abc",
                        accommodation, accommodation.getName(),
                        LocalDate.now().plusDays(2), LocalDate.now().plusDays(5),
                        new BigDecimal("30000"), 24));

        verify(mailSender).send(any(MimeMessage.class));
        verify(templateEngine).process(eq("emails/reservation-link-guest"), any(Context.class));
    }

    @Test
    void cancellationEmailWithoutProviderDoesNotThrow() {
        ReflectionTestUtils.setField(emailService, "mailSender", null);

        assertDoesNotThrow(() ->
                emailService.sendReservationCancelledEmail(
                        "guest@example.com", "Ana", "HF-TEST", "Casa Test", "Cambio de planes"));
    }

    @Test
    void cancellationEmailSmtpFailureDoesNotThrow() {
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendReservationCancelledEmail(
                        "guest@example.com", "Ana", "HF-TEST", "Casa Test", "Cambio de planes"));
    }
    @Test
    void guestEmailChangedNotificationUsesSecurityHtmlTemplate() {
        emailService.sendGuestEmailChangedNotification(
                "old@example.com", "Ana Pérez", "a***@example.com");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("emails/guest-email-changed"), contextCaptor.capture());
        assertEquals("Ana Pérez", contextCaptor.getValue().getVariable("guestName"));
        assertEquals("a***@example.com", contextCaptor.getValue().getVariable("maskedNewEmail"));
        assertEquals("HostFlow", contextCaptor.getValue().getVariable("appName"));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void cancellationEmailUsesReservationCancellationHtmlTemplate() {
        emailService.sendReservationCancelledEmail(
                "guest@example.com", "Ana", "HF-TEST", "Casa Test", "Cambio de planes");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("emails/reservation-cancelled"), contextCaptor.capture());
        assertEquals("Ana", contextCaptor.getValue().getVariable("guestName"));
        assertEquals("HF-TEST", contextCaptor.getValue().getVariable("confirmationCode"));
        assertEquals("Casa Test", contextCaptor.getValue().getVariable("accommodationName"));
        assertEquals("Cambio de planes", contextCaptor.getValue().getVariable("reason"));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void cancellationEmailUsesSafeDefaultsWhenOptionalTextIsMissing() {
        emailService.sendReservationCancelledEmail(
                "guest@example.com", " ", null, null, " ");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("emails/reservation-cancelled"), contextCaptor.capture());
        assertEquals("Huésped", contextCaptor.getValue().getVariable("guestName"));
        assertEquals("No disponible", contextCaptor.getValue().getVariable("confirmationCode"));
        assertEquals("el alojamiento", contextCaptor.getValue().getVariable("accommodationName"));
        assertEquals("No especificado", contextCaptor.getValue().getVariable("reason"));
    }

}
