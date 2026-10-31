package com.hostflow.model;

import com.hostflow.enums.Currency;
import com.hostflow.enums.UsdRateType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa una moneda habilitada para un alojamiento específico.
 *
 * <p>Cada alojamiento puede tener múltiples monedas habilitadas (ARS, USD, EUR),
 * y para cada una se configura:</p>
 * <ul>
 *   <li>Si está habilitada para cobro ({@code enabled}).</li>
 *   <li>Si usa tasa automática vía API externa ({@code useAutomaticRate}).</li>
 *   <li>Una tasa manual de respaldo ({@code manualRate}).</li>
 *   <li>Para USD: el tipo de cotización a usar (blue, oficial, MEP, etc.).</li>
 * </ul>
 *
 * <p><strong>Restricciones de negocio:</strong></p>
 * <ul>
 *   <li>Un alojamiento solo puede tener UNA configuración por moneda (único).</li>
 *   <li>La moneda base del alojamiento (ARS) siempre está habilitada con tasa 1.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Entity
@Table(name = "accepted_currencies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_accepted_currencies_accommodation_currency",
                columnNames = {"accommodation_id", "currency"}),
        indexes = @Index(name = "idx_accepted_currencies_accommodation",
                columnList = "accommodation_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AcceptedCurrency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Alojamiento al que pertenece esta configuración de moneda. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_accepted_currencies_accommodation"))
    private Accommodation accommodation;

    /** Moneda configurada (ARS, USD, EUR). */
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency;

    /** Si la moneda está habilitada para cobro. */
    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    /**
     * Si es {@code true}, se usa la tasa obtenida automáticamente desde la API externa.
     * Si es {@code false}, se usa el valor de {@link #manualRate}.
     */
    @Column(name = "use_automatic_rate", nullable = false)
    private boolean useAutomaticRate;

    /**
     * Tasa de conversión manual (de respaldo).
     * Representa cuántas unidades de esta moneda equivalen a 1 ARS.
     * Ejemplo: si 1 ARS = 0.00085 USD, entonces {@code manualRate = 0.00085}.
     */
    @Column(name = "manual_rate", precision = 20, scale = 10)
    private BigDecimal manualRate;

    /**
     * Tipo de cotización del dólar a usar (solo aplica cuando {@code currency = USD}).
     * Para EUR u otras monedas este campo se ignora.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "usd_rate_type", length = 20)
    private UsdRateType usdRateType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}