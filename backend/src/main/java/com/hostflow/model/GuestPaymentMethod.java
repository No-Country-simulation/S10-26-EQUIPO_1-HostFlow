package com.hostflow.model;

import com.hostflow.enums.PaymentMethodType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un método de pago registrado por un huésped en HostFlow.
 *
 * <p>Permite almacenar múltiples métodos de pago por huésped
 * (tarjetas de crédito/débito, transferencias bancarias, billeteras digitales, etc.)
 * y gestionar cuál de ellos se utiliza como predeterminado para agilizar el proceso de reserva.</p>
 *
 * <p><strong>Restricciones de negocio:</strong></p>
 * <ul>
 *   <li>Un huésped puede registrar múltiples métodos de pago.</li>
 *   <li>Solo un método puede estar marcado como predeterminado ({@code isDefault = true}) por huésped.
 *       Esta validación se gestiona a nivel de servicio.</li>
 *   <li>El método debe estar activo ({@code active = true}) para poder ser seleccionado en nuevas reservas.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
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

    // =========================================================================
    // IDENTIFICADOR
    // =========================================================================

    /**
     * Identificador único y clave primaria del método de pago.
     *
     * <p>Se utiliza {@link GenerationType#IDENTITY} para delegar la generación
     * del valor al motor de la base de datos (AUTO_INCREMENT en PostgreSQL),
     * lo cual es la práctica más eficiente y portable.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // RELACIONES
    // =========================================================================

    /**
     * Huésped propietario de este método de pago.
     *
     * <p>Relación {@code ManyToOne} obligatoria. Un método de pago siempre
     * pertenece a un único huésped. Se utiliza {@code FetchType.LAZY} para
     * optimizar el rendimiento, cargando el huésped solo cuando sea explícitamente requerido.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_guest_payment_methods_guest"))
    private Guest guest;

    // =========================================================================
    // INFORMACIÓN DEL MÉTODO DE PAGO
    // =========================================================================

    /**
     * Tipo general del método de pago.
     *
     * <p>Define la categoría del instrumento (ej. {@code BANK_TRANSFER},
     * {@code DIGITAL_WALLET}, {@code CARD}). Se almacena como {@code STRING}
     * en la base de datos para garantizar legibilidad y robustez ante cambios
     * en el orden del enum.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private PaymentMethodType paymentType;

    /**
     * Nombre descriptivo y amigable del método de pago para el huésped.
     *
     * <p>Ejemplos: "Visa terminada en 1234", "Cuenta principal de Mercado Pago".
     * Este es el texto que se mostrará en la interfaz de usuario durante el checkout.</p>
     *
     * <p>Restricciones:</p>
     * <ul>
     *   <li>No puede ser nulo ni vacío.</li>
     *   <li>Longitud máxima: 120 caracteres.</li>
     * </ul>
     */
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    /**
     * Nombre del proveedor o emisor del instrumento de pago.
     *
     * <p>Ejemplos: "Visa", "Mastercard", "Mercado Pago", "PayPal", "Banco Santander".</p>
     *
     * <p>Restricciones:</p>
     * <ul>
     *   <li>Campo opcional.</li>
     *   <li>Longitud máxima: 100 caracteres.</li>
     * </ul>
     */
    @Column(name = "provider_name", length = 100)
    private String providerName;

    /**
     * Número de tarjeta o cuenta, almacenado de forma ofuscada por seguridad.
     *
     * <p><strong>Nota de seguridad:</strong> NUNCA se deben almacenar números
     * de tarjetas de crédito completos (PAN) en texto plano. Este campo debe
     * contener solo los últimos 4 dígitos o una representación tokenizada
     * (ej. "**** **** **** 1234").</p>
     *
     * <p>Restricciones:</p>
     * <ul>
     *   <li>Campo opcional.</li>
     *   <li>Longitud máxima: 100 caracteres.</li>
     * </ul>
     */
    @Column(name = "account_number", length = 100)
    private String accountNumber;

    /**
     * Nombre completo del titular de la cuenta o tarjeta.
     *
     * <p>Se utiliza para validación visual o procesos de verificación con la pasarela de pago.</p>
     *
     * <p>Restricciones:</p>
     * <ul>
     *   <li>Campo opcional.</li>
     *   <li>Longitud máxima: 200 caracteres.</li>
     * </ul>
     */
    @Column(name = "account_holder", length = 200)
    private String accountHolder;

    /**
     * 🆕 Fecha de vencimiento del instrumento de pago (aplica principalmente a tarjetas).
     *
     * <p>Formato recomendado: "MM/YY" (ej. "12/28" para diciembre de 2028).
     * Se almacena como {@code String} para mantener la simplicidad, ya que
     * no se requieren operaciones aritméticas de fechas con este campo,
     * solo validación de formato y comparación de cadenas.</p>
     *
     * <p>Restricciones:</p>
     * <ul>
     *   <li>Campo opcional (no aplica a transferencias o efectivo).</li>
     *   <li>Longitud máxima: 10 caracteres.</li>
     * </ul>
     */
    @Column(name = "expiration_date", length = 10)
    private String expirationDate;

    // =========================================================================
    // ESTADO Y CONFIGURACIÓN
    // =========================================================================

    /**
     * Indica si este es el método de pago predeterminado del huésped.
     *
     * <p>Cuando un huésped inicia un proceso de reserva, el sistema debe
     * pre-seleccionar automáticamente el método donde este campo sea {@code true}.
     * La lógica de negocio en el servicio garantiza que solo exista
     * <strong>un</strong> método predeterminado por huésped.</p>
     *
     * <p>Valor por defecto: {@code false}</p>
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean defaultMethod = false;

    /**
     * Indica si el método de pago está activo y disponible para su uso.
     *
     * <p>Un método inactivo ({@code active = false}) no debe aparecer como
     * opción seleccionable en el checkout, pero se mantiene en la base de datos
     * para preservar el historial de reservas pasadas donde pudo haber sido utilizado.</p>
     *
     * <p>Valor por defecto: {@code true}</p>
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    // =========================================================================
    // METADATOS DE AUDITORÍA
    // =========================================================================

    /**
     * Fecha y hora de creación del registro.
     * Se establece automáticamente mediante {@link CreationTimestamp}.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha y hora de la última modificación.
     * Se actualiza automáticamente mediante {@link UpdateTimestamp}.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =========================================================================
    // MÉTODOS AUXILIARES (Domain Driven Design)
    // =========================================================================

    /**
     * Método de conveniencia para verificar si el método es predeterminado.
     *
     * <p>Mejora la legibilidad del código en comparación con usar
     * {@code isDefaultMethod()} generado por Lombok, permitiendo escribir
     * {@code paymentMethod.isDefault()} que es más natural en el dominio.</p>
     *
     * @return {@code true} si es el método predeterminado, {@code false} en caso contrario
     */
    public boolean isDefault() {
        return this.defaultMethod;
    }

    /**
     * Método de conveniencia para establecer el estado de predeterminado.
     *
     * @param value el nuevo estado ({@code true} para hacerlo predeterminado)
     */
    public void setDefault(boolean value) {
        this.defaultMethod = value;
    }
}