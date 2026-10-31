package com.hostflow.model;

import com.hostflow.enums.PaymentMethodType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Entidad que representa un método de pago registrado por un huésped.
 *
 * <p>Permite almacenar múltiples métodos de pago por huésped
 * (tarjetas de crédito/débito, transferencias bancarias, billeteras
 * digitales, etc.) y marcar uno como predeterminado.</p>
 *
 * <p><strong>Restricciones de negocio:</strong></p>
 * <ul>
 *   <li>Un huésped puede tener múltiples métodos de pago.</li>
 *   <li>Solo un método puede estar marcado como predeterminado.</li>
 *   <li>El método debe estar activo para poder usarse en reservas.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 * @see Guest
 * @see Reservation
 */
@Entity
@Table(name = "guest_payment_methods",
        indexes = {
                @Index(name = "idx_guest_payment_methods_guest", columnList = "guest_id"),
                @Index(name = "idx_guest_payment_methods_default", columnList = "guest_id, is_default")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestPaymentMethod {

    /** Identificador único del método de pago. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Huésped propietario de este método de pago.
     * Relación ManyToOne obligatoria.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_guest_payment_methods_guest"))
    private Guest guest;

    /**
     * Tipo de método de pago (tarjeta, transferencia, billetera digital, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private PaymentMethodType paymentType;

    /**
     * Nombre descriptivo del método de pago.
     * Ejemplo: "Visa terminada en 1234", "Cuenta Mercado Pago"
     */
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    /**
     * Nombre del proveedor del método de pago.
     * Ejemplo: "Visa", "Mastercard", "Mercado Pago", "PayPal"
     */
    @Column(name = "provider_name", length = 100)
    private String providerName;

    /**
     * Número de tarjeta o cuenta (ofuscado para seguridad).
     * Ejemplo: "**** **** **** 1234"
     */
    @Column(name = "account_number", length = 100)
    private String accountNumber;

    /**
     * Nombre del titular de la cuenta/tarjeta.
     */
    @Column(name = "account_holder", length = 200)
    private String accountHolder;

    /**
     * Indica si este es el método de pago predeterminado del huésped.
     * Solo un método por huésped puede ser predeterminado.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean defaultMethod = false;

    /**
     * Indica si el método de pago está activo y puede usarse.
     * Un método inactivo no puede seleccionarse para nuevas reservas.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Fecha de vencimiento (para tarjetas de crédito/débito).
     * Formato: MM/YY
     */
    @Column(name = "expiration_date", length = 10)
    private String expirationDate;

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

    public boolean isDefault() { return defaultMethod; }
    public void setDefault(boolean value) { this.defaultMethod = value; }
}