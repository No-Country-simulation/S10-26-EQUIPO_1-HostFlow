package com.hostflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa un enlace único de confirmación de reserva.
 *
 * <p>Cuando un propietario crea una reserva para un huésped, se genera
 * un enlace único con un token que el huésped debe usar para confirmar
 * su reserva. Este enlace tiene una validez de 24 horas por defecto,
 * pero puede ser reactivado si expira.</p>
 *
 * <p><strong>Características de seguridad:</strong></p>
 * <ul>
 *   <li>Token UUID v4 de 128 bits (imposible de adivinar).</li>
 *   <li>Expiración configurable (por defecto 24 horas).</li>
 *   <li>Un solo uso: se invalida después de la confirmación.</li>
 *   <li>Puede ser reactivado generando un nuevo token.</li>
 * </ul>
 *
 * <p><strong>Ciclo de vida:</strong></p>
 * <ol>
 *   <li>Owner crea reserva → se genera enlace con token único.</li>
 *   <li>Enlace enviado al huésped por email.</li>
 *   <li>Huésped hace clic en el enlace → confirma la reserva.</li>
 *   <li>Token marcado como usado ({@code used = true}).</li>
 *   <li>Si expira sin usarse, puede reactivarse.</li>
 * </ol>
 *
 * @author HostFlow Team
 * @version 1.0.0
 * @see Reservation
 */
@Entity
@Table(name = "reservation_links",
        indexes = {
                @Index(name = "idx_reservation_links_token", columnList = "token", unique = true),
                @Index(name = "idx_reservation_links_reservation", columnList = "reservation_id"),
                @Index(name = "idx_reservation_links_expires", columnList = "expires_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationLink {

    /** Identificador único del enlace. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reserva asociada a este enlace.
     * Relación OneToOne: un enlace pertenece a una única reserva.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_reservation_links_reservation"))
    private Reservation reservation;

    /**
     * Token único del enlace (UUID v4).
     * Se incluye en la URL de confirmación que recibe el huésped.
     *
     * <p>Ejemplo de URL:</p>
     * <pre>
     * https://hostflow.com/reservations/confirm?token=a1b2c3d4-e5f6-7890-abcd-ef1234567890
     * </pre>
     */
    @Column(name = "token", nullable = false, unique = true, length = 100)
    private String token;

    /**
     * Fecha y hora en que se generó el enlace.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha y hora en que el enlace expira.
     * Por defecto: 24 horas después de la creación.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Indica si el enlace ya fue utilizado para confirmar la reserva.
     * Un enlace solo puede usarse una vez.
     */
    @Column(name = "used", nullable = false)
    @Builder.Default
    private Boolean used = false;

    /**
     * Fecha y hora en que el enlace fue utilizado.
     * {@code null} si aún no se ha usado.
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * Número de veces que el enlace ha sido reactivado.
     * Útil para auditoría y control.
     */
    @Column(name = "reactivation_count", nullable = false)
    @Builder.Default
    private Integer reactivationCount = 0;

    /**
     * Fecha y hora de la última reactivación.
     * {@code null} si nunca se ha reactivado.
     */
    @Column(name = "last_reactivated_at")
    private LocalDateTime lastReactivatedAt;

    /**
     * URL completa de confirmación generada.
     * Se construye concatenando la URL base del frontend con el token.
     */
    @Column(name = "confirmation_url", length = 2048)
    private String confirmationUrl;

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Verifica si el enlace está vigente (no expirado y no usado).
     *
     * @return {@code true} si el enlace puede usarse para confirmar
     */
    public boolean isValid() {
        return !Boolean.TRUE.equals(used)
                && expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Verifica si el enlace ha expirado.
     *
     * @return {@code true} si la fecha actual es posterior a {@code expiresAt}
     */
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Marca el enlace como utilizado.
     * Registra la fecha y hora de uso.
     */
    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }

    /**
     * Reactiva el enlace generando un nuevo token y extendiendo la expiración.
     *
     * @param expirationHours horas de validez desde el momento de reactivación
     * @return el nuevo token generado
     */
    public String reactivate(int expirationHours) {
        this.token = UUID.randomUUID().toString();
        this.expiresAt = LocalDateTime.now().plusHours(expirationHours);
        this.used = false;
        this.usedAt = null;
        this.reactivationCount++;
        this.lastReactivatedAt = LocalDateTime.now();
        return this.token;
    }
}