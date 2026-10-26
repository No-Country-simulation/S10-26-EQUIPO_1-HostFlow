package com.hostflow.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO para la solicitud de recuperación de contraseña.
 *
 * <p>Representa los datos que el frontend envía cuando el usuario
 * olvida su contraseña y solicita un código de recuperación.</p>
 *
 * @author HostFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

    /**
     * Correo electrónico del usuario que solicita la recuperación.
     * Debe estar registrado en el sistema.
     */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;
}