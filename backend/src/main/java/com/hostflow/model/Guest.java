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
 *   <li>Almacena información de contacto, documento de identidad, preferencias y alergias.</li>
 *   <li>Mantiene un historial completo de reservas y métodos de pago asociados.</li>
 * </ul>
 *
 * <p><strong>Restricciones de negocio:</strong></p>
 * <ul>
 *   <li>El correo electrónico debe ser único en todo el sistema.</li>
 *   <li>El documento de identidad (DNI/CC) debe ser único si se proporciona.</li>
 *   <li>Nombre, apellido y nombre completo son obligatorios.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 3.1.0
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

    // =========================================================================
    // IDENTIFICADOR
    // =========================================================================

    /**
     * Identificador único del huésped (clave primaria).
     * Se utiliza {@link GenerationType#IDENTITY} para delegar la generación
     * al motor de base de datos (AUTO_INCREMENT en PostgreSQL).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // INFORMACIÓN PERSONAL Y DE CONTACTO
    // =========================================================================

    /**
     * Nombre del huésped.
     */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * Apellido del huésped.
     */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * ✅ CORRECCIÓN CRÍTICA: Nombre completo del huésped.
     *
     * <p>Se persiste en la base de datos para cumplir con la restricción NOT NULL
     * de la tabla y optimizar las consultas que requieren el nombre completo
     * sin tener que concatenar columnas en tiempo de ejecución.</p>
     *
     * <p>Restricciones:</p>
     * <ul>
     *   <li>No puede ser nulo.</li>
     *   <li>Longitud máxima: 200 caracteres.</li>
     * </ul>
     */
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    /**
     * Correo electrónico del huésped.
     * Se utiliza como identificador principal para notificaciones y recuperación.
     */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Número de teléfono del huésped (opcional).
     */
    @Column(name = "phone", length = 40)
    private String phone;

    /**
     * Número de documento de identidad del huésped (DNI, CC, Pasaporte).
     * Se almacena como String para soportar diferentes formatos internacionales.
     */
    @Column(name = "document_number", length = 20, unique = true)
    private String documentNumber;

    /**
     * Preferencias especiales o solicitudes particulares del huésped.
     * Ejemplos: "Cama king size", "Piso alto", "Cuna para bebé".
     */
    @Column(name = "preferences", length = 1000)
    private String preferences;

    /**
     * Alergias, intolerancias o restricciones alimentarias del huésped.
     * Se almacenan en una tabla separada ({@code guest_allergies}) vinculada
     * al huésped mediante una relación implícita (ElementCollection).
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "guest_allergies",
            joinColumns = @JoinColumn(name = "guest_id")
    )
    @Column(name = "allergy_name", length = 100)
    @Builder.Default
    private List<String> allergies = new ArrayList<>();

    /**
     * Indica si el huésped está activo en el sistema.
     * Un huésped inactivo no puede realizar nuevas reservas.
     * Valor por defecto: {@code true}
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
     */
    @OneToMany(mappedBy = "guest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();

    /**
     * Lista de métodos de pago registrados por este huésped.
     * Relación bidireccional con {@link GuestPaymentMethod}.
     */
    @OneToMany(mappedBy = "guest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GuestPaymentMethod> paymentMethods = new ArrayList<>();

    // =========================================================================
    // METADATOS DE AUDITORÍA
    // =========================================================================

    /**
     * Fecha y hora de creación del registro.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha y hora de la última modificación.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =========================================================================
    // NOTA SOBRE getFullName():
    // El método manual getFullName() ha sido eliminado intencionalmente.
    // La anotación @Getter de Lombok genera automáticamente getFullName(),
    // el cual ahora devolverá el valor persistente de la columna 'full_name',
    // evitando código duplicado y mejorando el rendimiento al no concatenar en memoria.
    // =========================================================================
}