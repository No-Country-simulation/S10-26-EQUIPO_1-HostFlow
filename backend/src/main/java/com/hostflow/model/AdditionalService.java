package com.hostflow.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa un servicio adicional ofrecido por un alojamiento.
 *
 * <p>Ejemplos de servicios adicionales:</p>
 * <ul>
 *   <li>Desayuno buffet</li>
 *   <li>Transfer al aeropuerto</li>
 *   <li>Spa / masajes</li>
 *   <li>Estacionamiento</li>
 *   <li>Mascotas</li>
 *   <li>Early check-in / Late check-out</li>
 * </ul>
 *
 * <p>El precio se almacena en la moneda base del alojamiento (ARS).
 * La conversión a otras monedas se calcula en tiempo real mediante
 * {@code CurrencyConversionService} al momento de mostrar el servicio
 * al huésped.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Entity
@Table(name = "additional_services",
        indexes = @Index(name = "idx_additional_services_accommodation",
                columnList = "accommodation_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdditionalService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Alojamiento que ofrece este servicio. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_additional_services_accommodation"))
    private Accommodation accommodation;

    /** Nombre del servicio (ej: "Desayuno buffet"). */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Descripción opcional del servicio. */
    @Column(name = "description", length = 1000)
    private String description;

    /** Precio del servicio en la moneda base del alojamiento (ARS). */
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    /** Si el servicio está activo y visible para los huéspedes. */
    @Column(name = "is_active", nullable = false)
    private boolean active;

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