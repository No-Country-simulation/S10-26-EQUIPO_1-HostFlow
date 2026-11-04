package com.hostflow.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para la solicitud de registro de un nuevo dueño de alojamiento.
 *
 * <p>Este objeto representa los datos que el frontend envía al endpoint
 * de registro, mapeados directamente desde el formulario de la UI:</p>
 * <ul>
 *   <li>Nombre y apellido</li>
 *   <li>Nombre del alojamiento</li>
 *   <li>Correo electrónico</li>
 *   <li>Contraseña (mínimo 8 caracteres)</li>
 *   <li>Aceptación de términos y política de privacidad</li>
 * </ul>
 *
 * <p>Incluye validaciones de Bean Validation para garantizar
 * la integridad de los datos antes de procesarlos.</p>
 *
 * @author HostFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerRegisterRequest {

    /**
     * Nombre completo del dueño.
     * Ejemplo: "Martina Silva"
     */
    @NotBlank(message = "El nombre y apellido es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String fullName;

    /**
     * Nombre comercial del alojamiento.
     * Ejemplo: "Cabañas del Arroyo"
     */
    @NotBlank(message = "El nombre del alojamiento es obligatorio")
    @Size(min = 3, max = 200, message = "El nombre del alojamiento debe tener entre 3 y 200 caracteres")
    private String accommodationName;

    /**
     * Correo electrónico del dueño. Debe ser válido y único.
     * Ejemplo: "martina@correo.com"
     */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    /**
     * Contraseña del dueño. Debe tener mínimo 8 caracteres.
     * Se encriptará con BCrypt antes de almacenarse.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    private String password;

    /**
     * Indica si el dueño acepta los términos y la política de privacidad.
     * Debe ser {@code true} para completar el registro.
     */
    @NotNull(message = "Debe aceptar los términos y la política de privacidad")
    private Boolean termsAccepted;
}