package com.hostflow.dto.response;

import lombok.*;
import java.util.List;

/**
 * DTO para la respuesta de autenticación.
 *
 * <p>Se devuelve al cliente después de un inicio de sesión exitoso.
 * Contiene el token JWT, la información del usuario autenticado
 * y metadatos adicionales para el frontend.</p>
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
     * 🆕 Indica si el correo electrónico del usuario ha sido verificado.
     * {@code true} si el usuario verificó su correo, {@code false} en caso contrario.
     * El usuario no podrá acceder a endpoints protegidos hasta que sea {@code true}.
     */
    private Boolean emailVerified;

    /**
     * Tiempo máximo de inactividad permitido antes de cerrar sesión automáticamente.
     * El valor depende del rol del usuario (en milisegundos).
     */
    private Long inactivityTimeoutMs;

    /**
     * Tiempo antes del cierre automático para mostrar el aviso al usuario.
     * Por defecto: 1 minuto (60000 ms) antes del cierre.
     */
    private Long warningBeforeMs;

    /**
     * Indica si se envió un correo de verificación al registrarse.
     * {@code true} si el usuario debe verificar su email antes de iniciar sesión.
     */
    private Boolean verificationEmailSent;
}