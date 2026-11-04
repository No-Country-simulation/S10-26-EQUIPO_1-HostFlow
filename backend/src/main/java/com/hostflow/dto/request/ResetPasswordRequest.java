package com.hostflow.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para la solicitud de restablecimiento de contraseña.
 *
 * <p>Representa los datos que el frontend envía cuando el usuario
 * ingresa el código de recuperación y su nueva contraseña.</p>
 *
 * @author HostFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    /**
     * Correo electrónico del usuario.
     */
    @NotBlank(message = "El correo electrónico es obligatorio")
    private String email;

    /**
     * Código de recuperación de 6 dígitos recibido por email.
     */
    @NotBlank(message = "El código de recuperación es obligatorio")
    @Size(min = 6, max = 6, message = "El código de recuperación debe tener 6 dígitos")
    private String code;

    /**
     * Nueva contraseña del usuario.
     * Debe tener mínimo 8 caracteres.
     */
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    private String newPassword;
}