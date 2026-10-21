package com.hostflow.dto.response;

import lombok.*;
import java.util.List;

/**
 * DTO para la respuesta de autenticación.
 *
 * <p>Se devuelve al cliente después de un inicio de sesión exitoso.
 * Contiene el token JWT y la información del usuario autenticado.</p>
 *
 * @author HostFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * Token JWT de acceso. Se debe incluir en el header
     * {@code Authorization: Bearer <token>} en las peticiones posteriores.
     */
    private String token;

    /**
     * Tipo de token (siempre "Bearer").
     */
    private String tokenType;

    /**
     * Identificador único del usuario autenticado.
     */
    private Long userId;

    /**
     * Correo electrónico del usuario autenticado.
     */
    private String email;

    /**
     * Nombre completo del usuario autenticado.
     */
    private String fullName;

    /**
     * Lista de roles asignados al usuario.
     */
    private List<String> roles;

    /**
     * NUEVOS CAMPOS para el control de inactividad
     */
    private Long inactivityTimeoutMs;      // Tiempo total de inactividad permitido
    private Long warningBeforeMs;          // Tiempo antes del cierre para avisar
}