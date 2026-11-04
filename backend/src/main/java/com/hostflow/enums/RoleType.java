package com.hostflow.enums;

/**
 * Enumeración que define los roles disponibles en el sistema HostFlow.
 *
 * <p>Los roles determinan los permisos y privilegios de cada usuario
 * dentro de la plataforma:</p>
 * <ul>
 *   <li>{@code ROLE_ADMIN}: Super usuario con privilegios totales.</li>
 *   <li>{@code ROLE_OWNER}: Dueño de alojamiento con acceso limitado.</li>
 * </ul>
 *
 * @author HostFlow Team
 */
public enum RoleType {

    /**
     * Administrador del sistema. Tiene acceso total a todas
     * las funcionalidades y puede gestionar todos los recursos.
     */
    ROLE_ADMIN,

    /**
     * Dueño de un alojamiento. Puede gestionar únicamente
     * su propio alojamiento y la información asociada.
     */
    ROLE_OWNER
}