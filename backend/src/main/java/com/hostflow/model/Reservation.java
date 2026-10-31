package com.hostflow.model;

import com.hostflow.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa una reserva de alojamiento en HostFlow.
 *
 * <p>Una reserva vincula a un huésped con un alojamiento específico
 * durante un período de tiempo determinado (check-in a check-out),
 * e incluye información sobre el estado, precios y método de pago.</p>
 *
 * <p><strong>Estados posibles de una reserva:</strong></p>
 * <ul>
 *   <li>{@code PENDING}: Reserva creada, esperando confirmación del huésped.</li>
 *   <li>{@code CONFIRMED}: Huésped confirmó la reserva y el pago.</li>
 *   <li>{@code CANCELLED}: Reserva cancelada (por huésped o propietario).</li>
 *   <li>{@code EXPIRED}: Enlace de confirmación expiró sin confirmación.</li>
 *   <li>{@code COMPLETED}: Estadía finalizada exitosamente.</li>
 * </ul>
 *
 * <p><strong>Restricciones de negocio:</strong></p>
 * <ul>
 *   <li>No pueden existir reservas overlapping para el mismo alojamiento.</li>
 *   <li>La fecha de check-out debe ser posterior a la de check-in.</li>
 *   <li>El precio total se calcula automáticamente según la duración.</li>
 *   <li>Debe existir un método de pago activo del huésped.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 * @see Guest
 * @see Accommodation
 * @see ReservationLink
 */
@Entity
@Table(name = "reservations",
        indexes = {
                @Index(name = "idx_reservations_accommodation", columnList = "accommodation_id"),
                @Index(name = "idx_reservations_guest", columnList = "guest_id"),
                @Index(name = "idx_reservations_status", columnList = "status"),
                @Index(name = "idx_reservations_dates", columnList = "check_in, check_out"),
                @Index(name = "idx_reservations_confirmation_code", columnList = "confirmation_code")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    /** Identificador único de la reserva. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código único de confirmación de la reserva.
     * Generado automáticamente, formato: RES-XXXXXX (6 caracteres alfanuméricos).
     * Se utiliza para referencia rápida y comunicación con el huésped.
     */
    @Column(name = "confirmation_code", nullable = false, unique = true, length = 20)
    private String confirmationCode;

    // =========================================================================
    // RELACIONES
    // =========================================================================

    /**
     * Alojamiento reservado.
     * Relación ManyToOne obligatoria con la entidad {@link Accommodation}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservations_accommodation"))
    private Accommodation accommodation;

    /**
     * Huésped que realizó la reserva.
     * Relación ManyToOne obligatoria con la entidad {@link Guest}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservations_guest"))
    private Guest guest;

    /**
     * Método de pago utilizado para esta reserva.
     * Relación ManyToOne con {@link GuestPaymentMethod}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id",
            foreignKey = @ForeignKey(name = "fk_reservations_payment_method"))
    private GuestPaymentMethod paymentMethod;

    // =========================================================================
    // FECHAS DE LA ESTADÍA
    // =========================================================================

    /**
     * Fecha y hora de check-in (ingreso al alojamiento).
     * No puede ser en el pasado al momento de crear la reserva.
     */
    @Column(name = "check_in", nullable = false)
    private LocalDateTime checkIn;

    /**
     * Fecha y hora de check-out (salida del alojamiento).
     * Debe ser posterior a {@link #checkIn}.
     */
    @Column(name = "check_out", nullable = false)
    private LocalDateTime checkOut;

    /**
     * Número de noches de la estadía.
     * Calculado automáticamente como la diferencia entre check-out y check-in.
     */
    @Column(name = "nights", nullable = false)
    private Integer nights;

    // =========================================================================
    // INFORMACIÓN FINANCIERA
    // =========================================================================

    /**
     * Precio base por noche en la moneda del alojamiento.
     */
    @Column(name = "price_per_night", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerNight;

    /**
     * Subtotal (precio por noche × número de noches).
     */
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    /**
     * Monto de impuestos aplicados (si corresponde).
     */
    @Column(name = "taxes", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxes = BigDecimal.ZERO;

    /**
     * Monto total de la reserva (subtotal + impuestos).
     */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // =========================================================================
    // ESTADO Y COMENTARIOS
    // =========================================================================

    /**
     * Estado actual de la reserva.
     * Valores: PENDING, CONFIRMED, CANCELLED, EXPIRED, COMPLETED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    /**
     * Número de huéspedes que se hospedarán.
     */
    @Column(name = "guest_count", nullable = false)
    @Builder.Default
    private Integer guestCount = 1;

    /**
     * Comentarios o solicitudes especiales del huésped.
     * Ejemplo: "Necesito cuna para bebé", "Llegada tardía después de las 22hs"
     */
    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    /**
     * Motivo de cancelación (si la reserva fue cancelada).
     */
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    /**
     * Fecha y hora de cancelación (si aplica).
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // =========================================================================
    // METADATOS DE AUDITORÍA
    // =========================================================================

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Calcula el número de noches entre check-in y check-out.
     *
     * @return número de noches
     */
    public int calculateNights() {
        return (int) java.time.Duration.between(checkIn, checkOut).toDays();
    }

    /**
     * Verifica si la reserva está activa (no cancelada ni expirada).
     *
     * @return {@code true} si la reserva está en estado CONFIRMED o PENDING
     */
    public boolean isActive() {
        return status == ReservationStatus.CONFIRMED || status == ReservationStatus.PENDING;
    }

    /**
     * Verifica si las fechas de esta reserva se superponen con un rango dado.
     *
     * @param newCheckIn  fecha de inicio a verificar
     * @param newCheckOut fecha de fin a verificar
     * @return {@code true} si hay superposición
     */
    public boolean overlapsWith(LocalDateTime newCheckIn, LocalDateTime newCheckOut) {
        return checkIn.isBefore(newCheckOut) && checkOut.isAfter(newCheckIn);
    }
}