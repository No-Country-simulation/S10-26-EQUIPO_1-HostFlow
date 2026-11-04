package com.hostflow.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuración explícita del bean JavaMailSender.
 *
 * <p>Spring Boot auto-configura JavaMailSender automáticamente cuando detecta
 * las propiedades spring.mail.*, pero en algunos casos es necesario definir
 * el bean explícitamente para garantizar su creación, especialmente cuando
 * se usan perfiles personalizados o hay conflictos en la configuración.</p>
 *
 * <p>Esta configuración asegura que el EmailService siempre tenga disponible
 * un JavaMailSender válido, independientemente del estado de las propiedades
 * de configuración.</p>
 *
 * @author HostFlow Team
 */
@Configuration
public class EmailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    /**
     * Crea y configura explícitamente el bean JavaMailSender.
     *
     * <p>Este método configura las propiedades SMTP necesarias para el envío
     * de correos electrónicos, incluyendo autenticación y encriptación TLS.</p>
     *
     * @return JavaMailSender configurado y listo para usar
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        // Configurar propiedades SMTP para autenticación y TLS
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.debug", "false"); // Cambiar a "true" para debug en desarrollo

        return mailSender;
    }
}