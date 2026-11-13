package com.hostflow.model;

import com.hostflow.enums.Currency;
import com.hostflow.enums.ServicePriceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Servicio adicional contratado para una reserva, con precio histórico aplicado. */
@Entity
@Table(name = "reservation_additional_services",
        indexes = @Index(name = "idx_reservation_additional_services_reservation", columnList = "reservation_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationAdditionalService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservation_additional_services_reservation"))
    private Reservation reservation;

    /** Referencia opcional para conservar el detalle aunque el servicio del catálogo se elimine. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "additional_service_id",
            foreignKey = @ForeignKey(name = "fk_reservation_additional_services_catalog"))
    private AdditionalService additionalService;

    @Column(name = "service_name_snapshot", nullable = false, length = 150)
    private String serviceNameSnapshot;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type", nullable = false, length = 20)
    private ServicePriceType priceType;

    /** Noches usadas en el cálculo; null cuando el tipo de precio no depende de noches. */
    @Column(name = "nights_applied")
    private Integer nightsApplied;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private Currency currency = Currency.ARS;
}
