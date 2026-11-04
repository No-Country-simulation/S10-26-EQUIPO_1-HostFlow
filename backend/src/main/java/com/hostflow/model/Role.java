package com.hostflow.model;

import com.hostflow.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad que representa un rol de seguridad en el sistema HostFlow.
 *
 * <p>Esta clase mapea la tabla {@code roles} en la base de datos.
 * Los roles definen el nivel de acceso y los permisos que tendrá
 * un usuario (dueño de alojamiento o administrador) dentro de la plataforma.</p>
 *
 * <p>Relaciones:</p>
 * <ul>
 *   <li>Tiene una relación {@code ManyToMany} con la entidad {@link Owner}.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    /**
     * Identificador único y clave primaria del rol.
     *
     * <p>Se utiliza {@link GenerationType#IDENTITY} para delegar la generación
     * del valor al motor de la base de datos (ej. AUTO_INCREMENT en H2/PostgreSQL
     * o SERIAL en PostgreSQL), lo cual es la práctica más eficiente y portable.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre del rol, almacenado como un Enum en Java pero como VARCHAR en la BD.
     *
     * <p>Configuración de la columna:</p>
     * <ul>
     *   <li>{@code nullable = false}: Garantiza la integridad referencial; un rol debe tener nombre.</li>
     *   <li>{@code unique = true}: Evita la duplicidad de roles en el sistema.</li>
     *   <li>{@code length = 50}: Optimiza el espacio en la base de datos (ej. "ROLE_ADMIN").</li>
     * </ul>
     *
     * <p>Se utiliza {@link EnumType#STRING} en lugar de {@link EnumType#ORDINAL}
     * para que los cambios en el orden del enum no corrompan los datos existentes
     * en la base de datos, haciendo el sistema más robusto ante refactorizaciones.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private RoleType roleName;

    /**
     * Constructor de conveniencia para crear un rol rápidamente
     * especificando únicamente su tipo.
     *
     * <p>Este constructor es especialmente útil en el {@code DataInitializer}
     * o en servicios donde necesitamos instanciar un rol sin tener que
     * usar el patrón Builder para un objeto tan simple.</p>
     *
     * @param roleName el tipo de rol a asignar (ej. RoleType.ROLE_ADMIN)
     */
    public Role(RoleType roleName) {
        this.roleName = roleName;
    }
}