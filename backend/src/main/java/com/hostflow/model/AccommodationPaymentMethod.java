package com.hostflow.model;

import com.hostflow.enums.Currency;           // 🆕 Import necesario para el enum de moneda
import com.hostflow.enums.PaymentMethodType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "accommodation_payment_methods", indexes = {
        @Index(name = "idx_payment_methods_accommodation", columnList = "accommodation_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccommodationPaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payment_methods_accommodation"))
    private Accommodation accommodation;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private PaymentMethodType paymentType;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "provider_name", length = 150)
    private String providerName;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "account_holder", length = 200)
    private String accountHolder;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(name = "cbu_cvu", length = 100)
    private String cbuCvu;

    @Column(name = "alias", length = 100)
    private String alias;

    @Column(name = "tax_id", length = 30)
    private String taxId;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "is_default", nullable = false)
    private boolean defaultMethod;

    public boolean isDefault() { return defaultMethod; }
    public void setDefault(boolean value) { this.defaultMethod = value; }

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // =========================================================================
    // 🆕 CONFIGURACIÓN MULTIMONEDA
    // =========================================================================

    /**
     * 🆕 Moneda en la que se recibe el pago a través de este método.
     * Permite que un mismo alojamiento tenga métodos de pago en distintas monedas
     * (ej: Transferencia bancaria en ARS, PayPal en USD, Stripe en EUR).
     * Por defecto es ARS para mantener compatibilidad con registros existentes.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    @ColumnDefault("'ARS'")
    @Builder.Default
    private Currency currency = Currency.ARS;

    // =========================================================================
    // METADATOS DE AUDITORÍA
    // =========================================================================

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