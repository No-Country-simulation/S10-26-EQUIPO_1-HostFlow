package com.hostflow.model;

import com.hostflow.enums.ServicePriceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un servicio adicional ofrecido por un alojamiento.
 *
 * <p>Ejemplos de servicios adicionales:</p>
 * <ul>
 *   <li>Desayuno buffet (precio por día)</li>
 *   <li>Transfer al aeropuerto (precio total)</li>
 *   <li>Spa / masajes (precio total)</li>
 *   <li>Estacionamiento (precio por día)</li>
 *   <li>Mascotas (precio por día)</li>
 *   <li>Early check-in / Late check-out (precio total)</li>
 * </ul>
 *
 * <p><strong>🆕 Campos agregados en v2.0:</strong></p>
 * <ul>
 *   <li>{@link #priceType}: define si el precio es por día o total.</li>
 *   <li>{@link #images}: lista de hasta 3 imágenes del servicio,
 *       almacenadas en Cloudinary.</li>
 * </ul>
 *
 * <p>El precio se almacena en la moneda base del alojamiento (ARS).
 * La conversión a otras monedas se calcula en tiempo real mediante
 * {@code CurrencyConversionService} al momento de mostrar el servicio
 * al huésped.</p>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@Entity
@Table(name = "additional_services",
        indexes = @Index(name = "idx_additional_services_accommodation",
                columnList = "accommodation_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    /**
     * 🆕 Tipo de cobro del servicio.
     *
     * <ul>
     *   <li>{@link ServicePriceType#PER_DAY}: se cobra por cada día de estadía.</li>
     *   <li>{@link ServicePriceType#TOTAL}: monto único independiente de la duración.</li>
     * </ul>
     *
     * <p>Valor por defecto: {@link ServicePriceType#TOTAL}.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "price_type", nullable = false, length = 20)
    @Builder.Default
    private ServicePriceType priceType = ServicePriceType.TOTAL;

    /** Si el servicio está activo y visible para los huéspedes. */
    @Column(name = "is_active", nullable = false)
    private boolean active;

    /**
     * 🆕 Lista de imágenes del servicio (máximo 3).
     *
     * <p>Las imágenes se almacenan en Cloudinary y se referencian
     * mediante la entidad {@link AdditionalServiceImage}.</p>
     *
     * <p><strong>Restricciones:</strong></p>
     * <ul>
     *   <li>Máximo 3 imágenes por servicio (validado en el servicio).</li>
     *   <li>Tamaño: entre 500 KB y 2 MB por imagen.</li>
     *   <li>Se eliminan en cascada si se elimina el servicio.</li>
     * </ul>
     *
     * @see AdditionalServiceImage
     */
    @OneToMany(mappedBy = "additionalService",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("orderIndex ASC, id ASC")
    @Builder.Default
    private List<AdditionalServiceImage> images = new ArrayList<>();

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