package com.hostflow.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuración de tiempos de inactividad por rol.
 *
 * <p>Define cuánto tiempo puede estar inactivo un usuario antes de que
 * su sesión se cierre automáticamente. Los valores se leen desde
 * application.properties y pueden variar según el perfil (dev/prod).</p>
 *
 * <ul>
 *   <li><b>Owner:</b> 10 minutos (600,000 ms)</li>
 *   <li><b>Admin:</b> 15 minutos (900,000 ms)</li>
 *   <li><b>Warning:</b> 1 minuto antes del cierre (60,000 ms)</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0
 */
@Component
@Getter
public class InactivityConfig {

    /**
     * Tiempo máximo de inactividad para usuarios con rol OWNER (en milisegundos).
     * Default: 600000 ms = 10 minutos
     */
    @Value("${app.inactivity.owner-milliseconds:600000}")
    private long ownerInactivityMs;

    /**
     * Tiempo máximo de inactividad para usuarios con rol ADMIN (en milisegundos).
     * Default: 900000 ms = 15 minutos
     */
    @Value("${app.inactivity.admin-milliseconds:900000}")
    private long adminInactivityMs;

    /**
     * Tiempo antes del cierre automático para mostrar el aviso al usuario (en milisegundos).
     * Default: 60000 ms = 1 minuto
     */
    @Value("${app.inactivity.warning-before-milliseconds:60000}")
    private long warningBeforeMs;

    /**
     * Devuelve el tiempo de inactividad según los roles del usuario.
     *
     * <p>Si el usuario tiene el rol {@code ROLE_ADMIN}, se aplica el tiempo
     * de administrador (15 min). En caso contrario, se aplica el tiempo
     * de owner (10 min).</p>
     *
     * @param roles lista de roles del usuario autenticado
     * @return tiempo de inactividad en milisegundos
     */
    public long getInactivityTimeoutForRoles(List<String> roles) {
        boolean isAdmin = roles.stream()
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
        return isAdmin ? adminInactivityMs : ownerInactivityMs;
    }
}