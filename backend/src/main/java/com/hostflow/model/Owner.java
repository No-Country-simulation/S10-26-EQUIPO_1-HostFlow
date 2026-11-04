package com.hostflow.model;

import jakarta.persistence.*;
        import lombok.*;
        import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa al dueño de un alojamiento en HostFlow.
 *
 * <p>Almacena la información personal del propietario, las credenciales
 * de acceso y la relación con su alojamiento. Esta entidad se mapea
 * directamente con el formulario de registro mostrado en la UI.</p>
 *
 * <p>Campos del formulario de registro:</p>
 * <ul>
 *   <li>Nombre y apellido del dueño</li>
 *   <li>Nombre del alojamiento</li>
 *   <li>Correo electrónico (único)</li>
 *   <li>Contraseña (encriptada con BCrypt)</li>
 *   <li>Aceptación de términos y política de privacidad</li>
 * </ul>
 *
 * @author HostFlow Team
 */
@Entity
@Table(name = "owners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Owner {

    /**
     * Identificador único del dueño (clave primaria).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre completo del dueño del alojamiento.
     * Ejemplo: "Martina Silva"
     */
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /**
     * Nombre comercial del alojamiento.
     * Ejemplo: "Cabañas del Arroyo"
     */
    @Column(name = "accommodation_name", nullable = false, length = 200)
    private String accommodationName;

    /**
     * Correo electrónico del dueño. Debe ser único en el sistema
     * y se utiliza como identificador para el inicio de sesión.
     */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Contraseña encriptada del dueño.
     * Nunca se almacena en texto plano; se utiliza BCrypt
     * para el hashing seguro.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Indicador de aceptación de los términos y la política
     * de privacidad. Debe ser {@code true} para completar el registro.
     */
    @Column(name = "terms_accepted", nullable = false)
    private Boolean termsAccepted = false;

    /**
     * Indica si la cuenta del dueño está activa.
     * Permite al administrador desactivar cuentas sin eliminarlas.
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * Roles asignados al dueño. Por defecto, todo dueño
     * registrado recibe el rol {@code ROLE_OWNER}.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "owner_roles",
            joinColumns = @JoinColumn(name = "owner_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * Fecha y hora de creación del registro.
     * Se establece automáticamente al persistir la entidad.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha y hora de la última actualización del registro.
     * Se actualiza automáticamente en cada modificación.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}