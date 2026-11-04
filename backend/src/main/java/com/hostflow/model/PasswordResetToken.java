package com.hostflow.model;


import jakarta.persistence.*;
        import lombok.*;
        import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un token de recuperación de contraseña.
 *
 * <p>Cuando un usuario solicita recuperar su contraseña, se genera
 * un token único de 6 dígitos que se almacena en esta tabla junto
 * con su fecha de expiración. El token es de un solo uso y expira
 * después de un tiempo configurable (por defecto 1 hora).</p>
 *
 * <p>Campos almacenados:</p>
 * <ul>
 *   <li>Token numérico de 6 dígitos</li>
 *   <li>Correo electrónico del usuario solicitante</li>
 *   <li>Fecha y hora de creación</li>
 *   <li>Fecha y hora de expiración</li>
 *   <li>Estado de uso (utilizado o pendiente)</li>
 * </ul>
 *
 * @author HostFlow Team
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    /**
     * Identificador único del token de recuperación.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Token numérico de 6 dígitos generado para la recuperación.
     * Ejemplo: "482916"
     */
    @Column(name = "token", nullable = false, length = 10)
    private String token;

    /**
     * Correo electrónico del usuario que solicitó la recuperación.
     * Se utiliza para identificar al propietario del token.
     */
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    /**
     * Fecha y hora en que se generó el token.
     * Se establece automáticamente al persistir la entidad.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha y hora en que el token expira y deja de ser válido.
     * Por defecto: 1 hora después de la creación.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Indica si el token ya fue utilizado para cambiar la contraseña.
     * Un token solo puede usarse una vez.
     */
    @Column(name = "used", nullable = false)
    private Boolean used = false;
}