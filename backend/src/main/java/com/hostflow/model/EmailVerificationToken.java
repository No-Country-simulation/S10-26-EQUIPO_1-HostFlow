package com.hostflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un token de verificación de correo electrónico en HostFlow.
 *
 * <p>Cuando un usuario se registra en la plataforma, se genera automáticamente un token
 * único (UUID) que se almacena en esta tabla junto con su fecha de expiración. Este token
 * es de un solo uso y expira después de un tiempo configurable (por defecto 1 hora).</p>
 *
 * <p><strong>Propósito de seguridad:</strong></p>
 * <ul>
 *   <li>Garantiza que el correo electrónico proporcionado durante el registro sea válido
 *       y esté bajo el control del usuario.</li>
 *   <li>Previene el registro de cuentas con correos electrónicos falsos o inexistentes.</li>
 *   <li>Restringe el acceso al sistema hasta que el usuario complete la verificación.</li>
 * </ul>
 *
 * <p><strong>Ciclo de vida del token:</strong></p>
 * <ol>
 *   <li>Se genera automáticamente durante el registro del usuario.</li>
 *   <li>Se envía por correo electrónico como parte de un enlace de verificación.</li>
 *   <li>El usuario hace clic en el enlace dentro del período de validez.</li>
 *   <li>El sistema valida el token y marca la cuenta como verificada.</li>
 *   <li>El token se marca como utilizado ({@code used = true}) y no puede reutilizarse.</li>
 * </ol>
 *
 * <p><strong>Características de seguridad:</strong></p>
 * <ul>
 *   <li>Token UUID de 128 bits: imposible de adivinar por fuerza bruta.</li>
 *   <li>Expiración configurable: previene el uso de tokens antiguos.</li>
 *   <li>Un solo uso: cada token solo puede verificarse una vez.</li>
 *   <li>Índice único en el campo {@code token}: garantiza integridad y rendimiento.</li>
 * </ul>
 *
 * <p><strong>Relaciones:</strong></p>
 * <ul>
 *   <li>No tiene relación directa con la entidad {@link Owner} para mantener
 *       la independencia y permitir la verificación incluso si el usuario
 *       no ha completado el registro.</li>
 *   <li>Se asocia al usuario mediante el campo {@code email}.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 * @see com.hostflow.service.AuthService#verifyEmail(String)
 * @see com.hostflow.repository.EmailVerificationTokenRepository
 */
@Entity
@Table(name = "email_verification_tokens",
        indexes = {
                // Índice único para búsquedas rápidas por token
                @Index(name = "idx_token_unique", columnList = "token", unique = true),
                // Índice para búsquedas por email (útil para invalidar tokens pendientes)
                @Index(name = "idx_email", columnList = "email")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {

    // =========================================================================
    // IDENTIFICADOR
    // =========================================================================

    /**
     * Identificador único y clave primaria del token de verificación.
     *
     * <p>Se utiliza {@link GenerationType#IDENTITY} para delegar la generación
     * del valor al motor de la base de datos (AUTO_INCREMENT en PostgreSQL),
     * lo cual es la práctica más eficiente y portable.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // DATOS DEL TOKEN
    // =========================================================================

    /**
     * Token UUID único generado para la verificación de correo electrónico.
     *
     * <p>Se genera utilizando {@link java.util.UUID#randomUUID()} que produce
     * un identificador de 128 bits con formato estándar:
     * {@code "a1b2c3d4-e5f6-7890-abcd-ef1234567890"}</p>
     *
     * <p><strong>Características de seguridad:</strong></p>
     * <ul>
     *   <li>Longitud: 36 caracteres (incluyendo guiones)</li>
     *   <li>Entropía: 122 bits de aleatoriedad (imposible de adivinar)</li>
     *   <li>Formato: UUID v4 estándar (aleatorio)</li>
     *   <li>Unicidad: garantizada por la restricción UNIQUE en la base de datos</li>
     * </ul>
     *
     * <p>Ejemplo de uso en URL de verificación:</p>
     * <pre>
     * http://localhost:8080/api/v1/auth/verify-email?token=a1b2c3d4-e5f6-7890-abcd-ef1234567890
     * </pre>
     */
    @Column(name = "token", nullable = false, unique = true, length = 100)
    private String token;

    /**
     * Correo electrónico del usuario que solicitó la verificación.
     *
     * <p>Se utiliza para identificar al propietario del token y asociarlo
     * con la cuenta de usuario correspondiente en la tabla {@code owners}.</p>
     *
     * <p><strong>Consideraciones de diseño:</strong></p>
     * <ul>
     *   <li>No se usa relación JPA directa con {@link Owner} para mantener
     *       la independencia y permitir verificación de emails no registrados.</li>
     *   <li>Se almacena como String para flexibilidad y rendimiento.</li>
     *   <li>Longitud máxima: 150 caracteres (estándar para emails).</li>
     * </ul>
     *
     * <p>Ejemplo: {@code "usuario@dominio.com"}</p>
     */
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    // =========================================================================
    // METADATOS DE TIEMPO
    // =========================================================================

    /**
     * Fecha y hora en que se generó el token.
     *
     * <p>Se establece automáticamente mediante la anotación {@link CreationTimestamp}
     * de Hibernate al persistir la entidad por primera vez.</p>
     *
     * <p><strong>Características:</strong></p>
     * <ul>
     *   <li>No puede ser nulo ({@code nullable = false})</li>
     *   <li>No es actualizable ({@code updatable = false})</li>
     *   <li>Se utiliza para auditoría y cálculo de expiración</li>
     * </ul>
     *
     * <p>Ejemplo: {@code 2026-10-22T10:30:00}</p>
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha y hora en que el token expira y deja de ser válido.
     *
     * <p>Se calcula automáticamente al generar el token sumando el tiempo
     * de expiración configurado (por defecto 60 minutos) a la fecha de creación.</p>
     *
     * <p><strong>Fórmula de cálculo:</strong></p>
     * <pre>
     * expiresAt = createdAt + expirationMinutes
     * Ejemplo: 2026-10-22T10:30:00 + 60 min = 2026-10-22T11:30:00
     * </pre>
     *
     * <p><strong>Validación de expiración:</strong></p>
     * <ul>
     *   <li>Si {@code expiresAt < LocalDateTime.now()} → Token expirado</li>
     *   <li>Si {@code expiresAt >= LocalDateTime.now()} → Token válido</li>
     * </ul>
     *
     * <p>Configurable mediante la propiedad:</p>
     * <pre>
     * app.email-verification.expiration-minutes=60
     * </pre>
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // =========================================================================
    // ESTADO DEL TOKEN
    // =========================================================================

    /**
     * Indica si el token ya fue utilizado para verificar el correo electrónico.
     *
     * <p>Un token solo puede usarse una vez. Después de la verificación exitosa,
     * este campo se establece en {@code true} para prevenir reutilización.</p>
     *
     * <p><strong>Ciclo de vida del estado:</strong></p>
     * <ol>
     *   <li>Creación: {@code used = false} (token pendiente de uso)</li>
     *   <li>Verificación exitosa: {@code used = true} (token utilizado)</li>
     *   <li>Intento de reutilización: rechazado por validación de negocio</li>
     * </ol>
     *
     * <p><strong>Validación en el servicio:</strong></p>
     * <pre>
     * if (Boolean.TRUE.equals(token.getUsed())) {
     *     throw new BusinessException("El token ya fue utilizado");
     * }
     * </pre>
     *
     * <p>Valor por defecto: {@code false}</p>
     */
    @Column(name = "used", nullable = false)
    private Boolean used = false;
}