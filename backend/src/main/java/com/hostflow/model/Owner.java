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
 * de acceso y la relación con sus roles dentro del sistema. Esta entidad
 * se mapea directamente con la tabla {@code owners} en la base de datos.</p>
 *
 * <p><strong>Políticas de Seguridad Implementadas:</strong></p>
 * <ul>
 *   <li><strong>Verificación de Email:</strong> Toda cuenta nueva se crea con
 *       {@code emailVerified = false}. El usuario no puede iniciar sesión hasta
 *       verificar su correo electrónico mediante un token único enviado por email.</li>
 *   <li><strong>Bloqueo por Intentos Fallidos:</strong> Tras 5 intentos fallidos
 *       de login consecutivos, la cuenta se bloquea automáticamente por 1 hora
 *       mediante el campo {@code blockedUntil}.</li>
 *   <li><strong>Activación/Desactivación:</strong> El administrador puede desactivar
 *       cuentas sin eliminarlas mediante el campo {@code enabled}, revocando
 *       inmediatamente el acceso al sistema y registrando la fecha del evento.</li>
 * </ul>
 *
 * <p><strong>Relaciones:</strong></p>
 * <ul>
 *   <li>Tiene una relación {@code ManyToMany} con la entidad {@link Role}
 *       a través de la tabla intermedia {@code owner_roles}.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 3.1.0
 */
@Entity
@Table(name = "owners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Owner {

    // =========================================================================
    // IDENTIFICADOR
    // =========================================================================

    /**
     * Identificador único del dueño (clave primaria).
     *
     * <p>Se utiliza {@link GenerationType#IDENTITY} para delegar la generación
     * del valor al motor de la base de datos (ej. SERIAL en PostgreSQL),
     * lo cual es la práctica más eficiente y portable para claves primarias.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // INFORMACIÓN PERSONAL
    // =========================================================================

    /**
     * Nombre completo del dueño del alojamiento.
     *
     * <p>Ejemplo: "Martina Silva"</p>
     *
     * <p>Restricciones de base de datos:</p>
     * <ul>
     *   <li>No puede ser nulo ({@code nullable = false})</li>
     *   <li>Longitud máxima: 150 caracteres</li>
     * </ul>
     */
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /**
     * Nombre comercial del alojamiento.
     *
     * <p>Ejemplo: "Cabañas del Arroyo"</p>
     *
     * <p>Restricciones de base de datos:</p>
     * <ul>
     *   <li>No puede ser nulo ({@code nullable = false})</li>
     *   <li>Longitud máxima: 200 caracteres</li>
     * </ul>
     */
    @Column(name = "accommodation_name", nullable = false, length = 200)
    private String accommodationName;

    // =========================================================================
    // CREDENCIALES DE ACCESO
    // =========================================================================

    /**
     * Correo electrónico del dueño.
     *
     * <p>Debe ser único en todo el sistema y se utiliza como identificador
     * principal para el inicio de sesión. También es el destino del correo
     * de verificación y recuperación de contraseña.</p>
     *
     * <p>Restricciones de base de datos:</p>
     * <ul>
     *   <li>No puede ser nulo ({@code nullable = false})</li>
     *   <li>Debe ser único ({@code unique = true})</li>
     *   <li>Longitud máxima: 150 caracteres</li>
     * </ul>
     */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Contraseña encriptada del dueño.
     *
     * <p>Nunca se almacena ni se transmite en texto plano. Se utiliza el
     * algoritmo BCrypt para el hashing seguro. La encriptación se realiza
     * en el servicio {@code AuthService} antes de persistir la entidad.</p>
     *
     * <p>Restricciones de base de datos:</p>
     * <ul>
     *   <li>No puede ser nula ({@code nullable = false})</li>
     * </ul>
     */
    @Column(name = "password", nullable = false)
    private String password;

    // =========================================================================
    // ESTADO DE LA CUENTA
    // =========================================================================

    /**
     * Indicador de aceptación de los términos y la política de privacidad.
     *
     * <p>Debe ser {@code true} para completar el registro. Esta validación
     * se realiza a nivel de negocio en el servicio {@code AuthService}
     * antes de permitir la persistencia de la entidad.</p>
     *
     * <p>Valor por defecto: {@code false}</p>
     */
    @Column(name = "terms_accepted", nullable = false)
    private Boolean termsAccepted = false;

    /**
     * Indica si la cuenta del dueño está activa.
     *
     * <p>Permite al administrador desactivar cuentas sin eliminarlas
     * físicamente de la base de datos (soft delete). Una cuenta desactivada
     * ({@code enabled = false}) será rechazada inmediatamente durante el
     * proceso de autenticación.</p>
     *
     * <p>Valor por defecto: {@code true} (cuenta activa)</p>
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 🆕 Fecha y hora en que la cuenta fue deshabilitada por el administrador.
     *
     * <p>Se establece automáticamente cuando un administrador desactiva
     * la cuenta de un owner (cambiando {@code enabled} a {@code false}).
     * Se utiliza para informar al usuario, durante el intento de inicio de sesión,
     * exactamente cuándo su cuenta fue deshabilitada, proporcionando transparencia
     * y facilitando el contacto con soporte.</p>
     *
     * <p>Valor {@code null} indica que la cuenta nunca ha sido deshabilitada
     * o que fue reactivada posteriormente por un administrador.</p>
     *
     * <p>Restricciones de base de datos:</p>
     * <ul>
     *   <li>Puede ser nulo ({@code nullable = true})</li>
     * </ul>
     *
     * @see com.hostflow.service.AuthService#login(com.hostflow.dto.request.LoginRequest)
     * @see com.hostflow.service.OwnerService#toggleOwnerStatus(Long, Boolean)
     */
    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;

    /**
     * Indica si el correo electrónico del dueño ha sido verificado.
     *
     * <p>Por defecto es {@code false} al registrarse. El usuario no podrá
     * iniciar sesión hasta que verifique su correo electrónico haciendo
     * clic en el enlace único enviado por email.</p>
     *
     * <p><strong>Flujo de verificación:</strong></p>
     * <ol>
     *   <li>Usuario se registra → cuenta creada con {@code emailVerified = false}.</li>
     *   <li>Se genera un token UUID único válido por 1 hora.</li>
     *   <li>Se envía correo electrónico con el enlace de verificación.</li>
     *   <li>Usuario hace clic en el enlace → {@code emailVerified = true}.</li>
     *   <li>El usuario puede iniciar sesión normalmente.</li>
     * </ol>
     *
     * <p>Restricciones de base de datos:</p>
     * <ul>
     *   <li>No puede ser nulo ({@code nullable = false})</li>
     *   <li>Valor por defecto: {@code false}</li>
     * </ul>
     *
     * @see com.hostflow.model.EmailVerificationToken
     * @see com.hostflow.service.AuthService#verifyEmail(String)
     */
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    /**
     * Fecha y hora hasta cuando está bloqueada la cuenta.
     *
     * <p>Se establece automáticamente tras 5 intentos fallidos de login
     * consecutivos. Durante el bloqueo, cualquier intento de login es
     * rechazado inmediatamente con un mensaje indicando el tiempo restante.</p>
     *
     * <p>El bloqueo es temporal (por defecto 1 hora) y se puede levantar de
     * las siguientes formas:</p>
     * <ul>
     *   <li>Esperando a que expire el tiempo de bloqueo.</li>
     *   <li>Realizando un login exitoso (desbloqueo automático por seguridad).</li>
     *   <li>Recuperando la contraseña mediante el flujo de recuperación.</li>
     * </ul>
     *
     * <p>Valor {@code null} indica que la cuenta no está bloqueada.</p>
     *
     * @see com.hostflow.service.AuthService#login(com.hostflow.dto.request.LoginRequest)
     * @see com.hostflow.service.LoginAttemptService
     */
    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    // =========================================================================
    // RELACIONES
    // =========================================================================

    /**
     * Roles asignados al dueño.
     *
     * <p>Por defecto, todo dueño registrado recibe el rol {@code ROLE_OWNER}.
     * El administrador puede tener ambos roles ({@code ROLE_ADMIN} y {@code ROLE_OWNER})
     * para tener visibilidad total del sistema.</p>
     *
     * <p><strong>Configuración de la relación:</strong></p>
     * <ul>
     *   <li>Tipo: {@code ManyToMany} (un dueño puede tener múltiples roles).</li>
     *   <li>Fetch: {@code EAGER} (los roles se cargan inmediatamente con el dueño
     *       para evitar LazyInitializationException en la serialización).</li>
     *   <li>Tabla intermedia: {@code owner_roles}.</li>
     *   <li>Columna de unión: {@code owner_id} (referencia a esta entidad).</li>
     *   <li>Columna inversa: {@code role_id} (referencia a la entidad Role).</li>
     * </ul>
     *
     * <p>Se utiliza {@code @Builder.Default} para inicializar el Set vacío
     * cuando se usa el patrón Builder, evitando NullPointerException.</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "owner_roles",
            joinColumns = @JoinColumn(name = "owner_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // =========================================================================
    // METADATOS DE AUDITORÍA
    // =========================================================================

    /**
     * Fecha y hora de creación del registro.
     *
     * <p>Se establece automáticamente al persistir la entidad por primera vez
     * mediante la anotación {@link CreationTimestamp} de Hibernate.</p>
     *
     * <p>Restricciones de base de datos:</p>
     * <ul>
     *   <li>No puede ser nula ({@code nullable = false})</li>
     *   <li>No es actualizable ({@code updatable = false})</li>
     * </ul>
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha y hora de la última actualización del registro.
     *
     * <p>Se actualiza automáticamente en cada modificación de la entidad
     * mediante la anotación {@link UpdateTimestamp} de Hibernate.</p>
     *
     * <p>Restricciones de base de datos:</p>
     * <ul>
     *   <li>No puede ser nula ({@code nullable = false})</li>
     * </ul>
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}