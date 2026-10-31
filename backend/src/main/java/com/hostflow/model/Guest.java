package com.hostflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa a un huésped (cliente) que realiza reservas
 * en los alojamientos gestionados por HostFlow.
 *
 * <p><strong>Características clave:</strong></p>
 * <ul>
 *   <li>Un huésped puede tener múltiples reservas en diferentes alojamientos.</li>
 *   <li>Se identifica de forma única por su correo electrónico.</li>
 *   <li>Almacena información de contacto y documento de identidad para verificación.</li>
 *   <li>Mantiene un historial completo de reservas y métodos de pago asociados.</li>
 * </ul>
 *
 * <p><strong>Restricciones de negocio:</strong></p>
 * <ul>
 *   <li>El correo electrónico debe ser único en todo el sistema.</li>
 *   <li>El documento de identidad (DNI/CC) debe ser único si se proporciona.</li>
 *   <li>Nombre y apellido son obligatorios.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 * @see Reservation
 * @see GuestPaymentMethod
 */
@Entity
@Table(name = "guests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_guests_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_guests_document", columnNames = "document_number")
        },
        indexes = {
                @Index(name = "idx_guests_email", columnList = "email"),
                @Index(name = "idx_guests_document", columnList = "document_number")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Guest {

    /**
     * Identificador único del huésped (clave primaria).
     * Se utiliza {@link GenerationType#IDENTITY} para delegar la generación
     * al motor de base de datos (AUTO_INCREMENT en PostgreSQL).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre del huésped.
     *
     * <p>Restricciones:</p>
     * <ul>
     *   <li>No puede ser nulo.</li>
     *   <li>Longitud máxima: 100 caracteres.</li>
     * </ul>
     */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * Apellido del huésped.
     *
     * <p>Restricciones:</p>
     * <ul>
     *   <li>No puede ser nulo.</li>
     *   <li>Longitud máxima: 100 caracteres.</li>
     * </ul>
     */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Correo electrónico del huésped.
     * Debe ser único y se utiliza como identificador principal
     * para notificaciones, recuperación de reservas y autenticación.
     *
     * <p>Restricciones:</p>
     * <ul>
     *   <li>No puede ser nulo.</li>
     *   <li>Debe ser único en la tabla.</li>
     *   <li>Longitud máxima: 150 caracteres.</li>
     * </ul>
     */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Número de teléfono del huésped (opcional).
     * Utilizado para contacto de emergencia o confirmaciones de reserva.
     *
     * <p>Restricciones:</p>
     * <ul>
     *   <li>Puede ser nulo.</li>
     *   <li>Longitud máxima: 40 caracteres.</li>
     * </ul>
     */
    @Column(name = "phone", length = 40)
    private String phone;

    /**
     * 🆕 Número de documento de identidad del huésped (DNI, CC, Pasaporte).
     * Se almacena como String para soportar diferentes formatos internacionales,
     * aunque la validación en el DTO restringe la entrada a solo números (5-12 dígitos).
     *
     * <p>Restricciones:</p>
     * <ul>
     *   <li>Puede ser nulo (campo opcional en el registro).</li>
     *   <li>Debe ser único en el sistema si se proporciona (evita duplicados).</li>
     *   <li>Longitud máxima: 20 caracteres.</li>
     * </ul>
     */
    @Column(name = "document_number", length = 20, unique = true)
    private String documentNumber;

    /**
     * Indica si el huésped está activo en el sistema.
     * Un huésped inactivo no puede realizar nuevas reservas.
     *
     * <p>Valor por defecto: {@code true}</p>
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    // =========================================================================
    // RELACIONES
    // =========================================================================

    /**
     * Lista de reservas realizadas por este huésped.
     * Relación bidireccional con {@link Reservation}.
     *
     * <p>Configuración:</p>
     * <ul>
     *   <li>Cascade: ALL (operaciones en cascada).</li>
     *   <li>OrphanRemoval: true (eliminar reservas huérfanas).</li>
     *   <li>Fetch: LAZY (carga diferida para optimizar rendimiento).</li>
     * </ul>
     */
    @OneToMany(mappedBy = "guest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();

    /**
     * Lista de métodos de pago registrados por este huésped.
     * Relación bidireccional con {@link GuestPaymentMethod}.
     *
     * <p>Un huésped puede tener múltiples métodos de pago
     * (tarjetas, transferencias, billeteras digitales, etc.)</p>
     */
    @OneToMany(mappedBy = "guest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GuestPaymentMethod> paymentMethods = new ArrayList<>();

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
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Obtiene el nombre completo del huésped concatenando nombre y apellido.
     *
     * <p>Este método se proporciona para mantener la compatibilidad con
     * cualquier parte del sistema (logs, emails, DTOs) que espere un
     * método {@code getFullName()} en lugar de acceder a los campos por separado.</p>
     *
     * @return Nombre completo formateado (ej: "Juan Carlos Pérez")
     */
    public String getFullName() {
        return (this.firstName != null ? this.firstName : "") + " " +
                (this.lastName != null ? this.lastName : "");
    }
}