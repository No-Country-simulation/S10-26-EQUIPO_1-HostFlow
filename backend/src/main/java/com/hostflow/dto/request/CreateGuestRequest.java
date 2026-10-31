package com.hostflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear un nuevo huésped en el sistema.
 *
 * <p>Se utiliza en el endpoint {@code POST /public/guests} para permitir
 * que los huéspedes se registren antes de realizar una reserva.</p>
 *
 * <p><strong>Validaciones:</strong></p>
 * <ul>
 *   <li>Nombre y apellido son obligatorios (2-100 caracteres).</li>
 *   <li>El correo electrónico debe ser válido y único.</li>
 *   <li>El teléfono es opcional (máximo 40 caracteres).</li>
 *   <li>El documento de identidad es opcional (solo números, 5-12 dígitos).</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@Schema(description = "Datos para crear un nuevo huésped en el sistema")
public record CreateGuestRequest(

        /**
         * Nombre del huésped.
         *
         * <p><strong>Validaciones:</strong></p>
         * <ul>
         *   <li>{@code @NotBlank}: No puede ser nulo, vacío ni contener solo espacios.</li>
         *   <li>{@code @Size}: Longitud entre 2 y 100 caracteres.</li>
         * </ul>
         */
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        @Schema(description = "Nombre del huésped", example = "Carlos Fernando", requiredMode = Schema.RequiredMode.REQUIRED)
        String firstName,

        /**
         * Apellido del huésped.
         *
         * <p><strong>Validaciones:</strong></p>
         * <ul>
         *   <li>{@code @NotBlank}: No puede ser nulo, vacío ni contener solo espacios.</li>
         *   <li>{@code @Size}: Longitud entre 2 y 100 caracteres.</li>
         * </ul>
         */
        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
        @Schema(description = "Apellido del huésped", example = "Palacios", requiredMode = Schema.RequiredMode.REQUIRED)
        String lastName,

        /**
         * Correo electrónico del huésped.
         *
         * <p>Debe ser único en el sistema y se utiliza como identificador principal
         * para la comunicación y verificación de la cuenta.</p>
         *
         * <p><strong>Validaciones:</strong></p>
         * <ul>
         *   <li>{@code @NotBlank}: No puede ser nulo o vacío.</li>
         *   <li>{@code @Email}: Debe tener un formato válido de correo electrónico.</li>
         * </ul>
         */
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El formato del correo electrónico no es válido")
        @Schema(description = "Correo electrónico del huésped", example = "cfpalaciwalker@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        /**
         * Teléfono de contacto del huésped (opcional).
         *
         * <p>Se utiliza para comunicación directa en caso de emergencias
         * o confirmaciones de reserva.</p>
         *
         * <p><strong>Validaciones:</strong></p>
         * <ul>
         *   <li>{@code @Size}: Longitud máxima de 40 caracteres.</li>
         *   <li>Campo opcional (puede ser null o vacío).</li>
         * </ul>
         *
         * <p>Formatos aceptados:</p>
         * <ul>
         *   <li>+54 11 5555 5555 (internacional)</li>
         *   <li>11 5555 5555 (nacional)</li>
         *   <li>+1 234 567 8900 (EE.UU.)</li>
         * </ul>
         */
        @Size(max = 40, message = "El teléfono no puede superar los 40 caracteres")
        @Schema(description = "Teléfono de contacto", example = "+54 11 5555 5555", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String phone,

        /**
         *  Número de documento de identidad del huésped (DNI, CC, Pasaporte, etc.).
         *
         * <p>Se utiliza para identificación oficial del huésped durante el proceso
         * de reserva y check-in. Es obligatorio en algunos países por regulaciones
         * de hotelería y turismo.</p>
         *
         * <p><strong>Validaciones:</strong></p>
         * <ul>
         *   <li>{@code @Pattern}: Solo se permiten números (0-9).</li>
         *   <li>{@code @Size}: Longitud entre 5 y 12 dígitos.</li>
         *   <li>Campo opcional (puede ser null o vacío).</li>
         * </ul>
         *
         * <p><strong>Ejemplos válidos:</strong></p>
         * <ul>
         *   <li>12345678 (DNI argentino - 8 dígitos)</li>
         *   <li>1234567890 (CC colombiana - 10 dígitos)</li>
         *   <li>123456789 (DNI peruano - 8 dígitos)</li>
         *   <li>A1234567 (Pasaporte - puede incluir letras, pero aquí solo números)</li>
         * </ul>
         *
         * <p><strong>Nota:</strong> Aunque el campo acepta solo números, diferentes
         * países tienen diferentes formatos de documento. Se recomienda validar
         * el formato específico según el país del huésped en una versión futura.</p>
         */
        @Pattern(regexp = "^[0-9]{5,12}$", message = "El documento debe contener solo números y tener entre 5 y 12 dígitos")
        @Size(min = 5, max = 12, message = "El documento debe tener entre 5 y 12 dígitos")
        @Schema(
                description = "Número de documento de identidad (DNI/CC). Solo números, mínimo 5, máximo 12 dígitos",
                example = "12345678",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String documentNumber
) {}