package com.hostflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Entidad que registra todos los intentos de inicio de sesión en el sistema.
 *
 * <p>Esta tabla sirve como auditoría de seguridad y permite implementar
 * el bloqueo automático de cuentas tras múltiples intentos fallidos.</p>
 *
 * <p>Cada registro contiene:</p>
 * <ul>
 *   <li>Información del usuario (ID, nombre, email, rol)</li>
 *   <li>Resultado del intento (exitoso o fallido)</li>
 *   <li>Fecha y hora del intento</li>
 *   <li>Información de bloqueo (si este intento causó el bloqueo)</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Entity
@Table(name = "login_attempts", indexes = {
        @Index(name = "idx_email_attempt", columnList = "email, attempt_at"),
        @Index(name = "idx_owner_id", columnList = "owner_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    /**
     * Identificador único del registro de intento de login.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID del usuario en la base de datos.
     * Puede ser {@code null} si el email no está registrado (intento con email inexistente).
     */
    @Column(name = "owner_id")
    private Long ownerId;

    /**
     * Correo electrónico utilizado en el intento de login.
     */
    @Column(nullable = false, length = 100)
    private String email;

    /**
     * Nombre completo del usuario.
     * Puede ser {@code null} si el email no está registrado.
     */
    @Column(name = "full_name", length = 150)
    private String fullName;

    /**
     * Rol principal del usuario (ROLE_OWNER o ROLE_ADMIN).
     * Puede ser {@code null} si el email no está registrado.
     */
    @Column(length = 50)
    private String role;

    /**
     * Indica si el intento de login fue exitoso.
     * {@code true} = login exitoso, {@code false} = credenciales incorrectas.
     */
    @Column(nullable = false)
    private boolean success;

    /**
     * Indica si este intento causó el bloqueo de la cuenta.
     * {@code true} = este fue el intento número 5 y se bloqueó la cuenta.
     */
    @Column(nullable = false)
    private boolean blocked;

    /**
     * Fecha y hora hasta cuando está bloqueada la cuenta.
     * {@code null} si la cuenta no está bloqueada.
     */
    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    /**
     * Fecha y hora en que se realizó el intento de login.
     * Se establece automáticamente al persistir la entidad.
     */
    @CreationTimestamp
    @Column(name = "attempt_at", nullable = false, updatable = false)
    private LocalDateTime attemptAt;

    /**
     * Dirección IP desde donde se realizó el intento (opcional, para auditoría avanzada).
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User-Agent del navegador/dispositivo (opcional, para auditoría avanzada).
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
}