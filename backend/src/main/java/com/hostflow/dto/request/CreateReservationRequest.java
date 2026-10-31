package com.hostflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * DTO para solicitar la creación de una nueva reserva.
 *
 * <p>Contiene todos los datos necesarios para generar una reserva
 * y el enlace de confirmación que se enviará al huésped.</p>
 *
 * <p><strong>Validaciones:</strong></p>
 * <ul>
 *   <li>Nombre y apellido del huésped son obligatorios.</li>
 *   <li>Correo electrónico debe ser válido.</li>
 *   <li>Fechas de check-in y check-out deben ser futuras.</li>
 *   <li>Check-out debe ser posterior a check-in.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Schema(description = "Datos necesarios para crear una nueva reserva")
public record CreateReservationRequest(

        /**
         * Nombre del huésped.
         */
        @NotBlank(message = "El nombre del huésped es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        @Schema(description = "Nombre del huésped", example = "Juan Carlos")
        String guestFirstName,

        /**
         * Apellido del huésped.
         */
        @NotBlank(message = "El apellido del huésped es obligatorio")
        @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
        @Schema(description = "Apellido del huésped", example = "Pérez")
        String guestLastName,

        /**
         * Correo electrónico del huésped.
         * Debe ser válido y único en el sistema.
         */
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El formato del correo electrónico no es válido")
        @Schema(description = "Correo electrónico del huésped", example = "juan.perez@email.com")
        String guestEmail,

        /**
         * Teléfono del huésped (opcional).
         */
        @Size(max = 40, message = "El teléfono no puede superar los 40 caracteres")
        @Schema(description = "Teléfono de contacto del huésped", example = "+54 11 5555 5555")
        String guestPhone,

        /**
         * Fecha y hora de check-in.
         * Debe ser una fecha futura.
         */
        @NotNull(message = "La fecha de check-in es obligatoria")
        @Future(message = "La fecha de check-in debe ser futura")
        @Schema(description = "Fecha y hora de ingreso al alojamiento", example = "2026-12-15T14:00:00")
        LocalDateTime checkIn,

        /**
         * Fecha y hora de check-out.
         * Debe ser posterior al check-in.
         */
        @NotNull(message = "La fecha de check-out es obligatoria")
        @Future(message = "La fecha de check-out debe ser futura")
        @Schema(description = "Fecha y hora de salida del alojamiento", example = "2026-12-18T11:00:00")
        LocalDateTime checkOut,

        /**
         * Número de huéspedes que se hospedarán.
         */
        @Min(value = 1, message = "Debe haber al menos 1 huésped")
        @Max(value = 50, message = "No se permiten más de 50 huéspedes")
        @Schema(description = "Número de huéspedes", example = "2")
        Integer guestCount,

        /**
         * Comentarios o solicitudes especiales (opcional).
         */
        @Size(max = 1000, message = "Los comentarios no pueden superar los 1000 caracteres")
        @Schema(description = "Solicitudes especiales o comentarios", example = "Necesito cuna para bebé")
        String specialRequests,

        /**
         * Horas de validez del enlace de confirmación.
         * Por defecto: 24 horas.
         */
        @Min(value = 1, message = "La validez mínima es de 1 hora")
        @Max(value = 168, message = "La validez máxima es de 168 horas (7 días)")
        @Schema(description = "Horas de validez del enlace de confirmación (default: 24)",
                example = "24")
        Integer linkExpirationHours
) {
    /**
     * Obtiene las horas de validez del enlace, con valor por defecto de 24.
     */
    public int getLinkExpirationHoursOrDefault() {
        return linkExpirationHours != null ? linkExpirationHours : 24;
    }

    /**
     * Obtiene el número de huéspedes, con valor por defecto de 1.
     */
    public int getGuestCountOrDefault() {
        return guestCount != null ? guestCount : 1;
    }
}