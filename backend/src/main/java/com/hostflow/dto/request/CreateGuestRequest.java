package com.hostflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List; // 🆕 Import necesario para la lista de alergias

/**
 * DTO para crear un nuevo huésped en el sistema.
 *
 * <p>Se utiliza en el endpoint {@code POST /public/guests} o durante la creación
 * de una reserva para permitir que los huéspedes se registren o actualicen sus datos.</p>
 *
 * <p><strong>Validaciones:</strong></p>
 * <ul>
 *   <li>Nombre y apellido son obligatorios (2-100 caracteres).</li>
 *   <li>El correo electrónico debe tener un formato válido; puede repetirse entre huéspedes.</li>
 *   <li>El teléfono es opcional (máximo 40 caracteres).</li>
 *   <li>El documento de identidad es opcional (solo números, 5-12 dígitos).</li>
 *   <li>Las preferencias y alergias son opcionales.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.1.0 (Agregado soporte para preferencias y alergias)
 */
@Schema(description = "Datos para crear o actualizar un huésped en el sistema")
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
         * <p>Se utiliza como dato de contacto para las comunicaciones y puede
         * repetirse en distintas reservas. No identifica de forma única al huésped.</p>
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
         */
        @Size(max = 40, message = "El teléfono no puede superar los 40 caracteres")
        @Schema(description = "Teléfono de contacto", example = "+54 11 5555 5555", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String phone,

        /**
         * Número de documento de identidad del huésped (DNI, CC, Pasaporte, etc.).
         *
         * <p>Se utiliza para identificación oficial del huésped durante el proceso
         * de reserva y check-in.</p>
         *
         * <p><strong>Validaciones:</strong></p>
         * <ul>
         *   <li>{@code @Pattern}: Solo se permiten números (0-9).</li>
         *   <li>{@code @Size}: Longitud entre 5 y 12 dígitos.</li>
         *   <li>Campo opcional (puede ser null o vacío).</li>
         * </ul>
         */
        @Pattern(regexp = "^[0-9]{5,12}$", message = "El documento debe contener solo números y tener entre 5 y 12 dígitos")
        @Size(min = 5, max = 12, message = "El documento debe tener entre 5 y 12 dígitos")
        @Schema(
                description = "Número de documento de identidad (DNI/CC). Solo números, mínimo 5, máximo 12 dígitos",
                example = "12345678",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String documentNumber,

        // =========================================================================
        // 🆕 CAMPOS AGREGADOS: PREFERENCIAS Y ALERGIAS
        // =========================================================================

        /**
         * Preferencias o solicitudes especiales del huésped (texto libre).
         *
         * <p><strong>Validaciones:</strong></p>
         * <ul>
         *   <li>{@code @Size}: Longitud máxima de 1000 caracteres.</li>
         *   <li>Campo opcional (puede ser null o vacío).</li>
         * </ul>
         */
        @Size(max = 1000, message = "Las preferencias no pueden superar los 1000 caracteres")
        @Schema(
                description = "Preferencias o solicitudes especiales del huésped",
                example = "Prefiere habitación en piso bajo, lejos del elevador y con vista al jardín",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String preferences,

        /**
         * Lista de alergias o restricciones alimentarias del huésped.
         *
         * <p>Se recibe como un array de cadenas de texto desde el frontend,
         * permitiendo combinar opciones predefinidas (ej: "Gluten") con texto
         * libre ingresado por el cliente (ej: "Alergia al látex").</p>
         *
         * <p><strong>Validaciones:</strong></p>
         * <ul>
         *   <li>Campo opcional (puede ser null o una lista vacía).</li>
         * </ul>
         */
        @Schema(
                description = "Lista de alergias o restricciones alimentarias",
                example = "[\"Gluten\", \"Mariscos\", \"Alergia al látex\"]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        List<String> allergies

) {}