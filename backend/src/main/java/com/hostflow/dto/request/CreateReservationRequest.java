package com.hostflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO para solicitar la creación de una nueva reserva.
 *
 * <p>El propietario envía los datos preliminares de contacto y las fechas de estadía.
 * Alergias, preferencias y servicios adicionales se capturan en el onboarding del huésped,
 * no en este request. La vigencia del enlace de onboarding es fija: 24 horas.</p>
 *
 * @author HostFlow Team
 * @version 2.0.0 (Contrato alineado al flujo de onboarding; sin alergias/preferencias ni expiración configurable)
 */
@Schema(description = "Datos preliminares para que el propietario cree una reserva y genere el enlace de onboarding")
public record CreateReservationRequest(

        @NotBlank(message = "El nombre del huésped es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        @Schema(description = "Nombre del huésped", example = "Juan Carlos")
        String guestFirstName,

        @NotBlank(message = "El apellido del huésped es obligatorio")
        @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
        @Schema(description = "Apellido del huésped", example = "Pérez")
        String guestLastName,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El formato del correo electrónico no es válido")
        @Schema(description = "Correo electrónico del huésped", example = "juan.perez@email.com")
        String guestEmail,

        @Size(max = 40, message = "El teléfono no puede superar los 40 caracteres")
        @Schema(description = "Teléfono de contacto del huésped", example = "+54 11 5555 5555")
        String guestPhone,

        @NotNull(message = "La fecha de check-in es obligatoria")
        @Schema(description = "Fecha de ingreso al alojamiento (YYYY-MM-DD). Se permite el día actual.", example = "2026-12-15")
        LocalDate checkIn,

        @NotNull(message = "La fecha de check-out es obligatoria")
        @Schema(description = "Fecha de salida del alojamiento (YYYY-MM-DD). Debe ser posterior al check-in.", example = "2026-12-18")
        LocalDate checkOut,

        @Min(value = 1, message = "Debe haber al menos 1 huésped")
        @Max(value = 50, message = "No se permiten más de 50 huéspedes")
        @Schema(description = "Número de huéspedes", example = "2")
        Integer guestCount,

        @Size(max = 1000, message = "Los comentarios no pueden superar los 1000 caracteres")
        @Schema(description = "Solicitudes especiales o comentarios del propietario", example = "Necesito cuna para bebé")
        String specialRequests

) {
    /**
     * Obtiene el número de huéspedes, con valor por defecto de 1.
     */
    public int getGuestCountOrDefault() {
        return guestCount != null ? guestCount : 1;
    }
}
