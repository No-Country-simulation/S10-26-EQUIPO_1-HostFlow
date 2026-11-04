package com.hostflow.dto.response;


import lombok.*;

/**
 * DTO para la respuesta de operaciones de recuperación de contraseña.
 *
 * <p>Contiene el mensaje de confirmación y datos adicionales
 * sobre la operación de recuperación realizada.</p>
 *
 * @author HostFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetResponse {

    /**
     * Mensaje descriptivo del resultado de la operación.
     */
    private String message;

    /**
     * Correo electrónico al que se envió el código (ofuscado para seguridad).
     */
    private String email;

    /**
     * Tiempo de expiración del token en minutos.
     */
    private Integer expirationMinutes;
}