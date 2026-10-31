package com.hostflow.dto.response;

import com.hostflow.model.ReservationLink;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa un enlace de confirmación de reserva.
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Schema(description = "Información del enlace de confirmación de reserva")
public record ReservationLinkResponse(

        @Schema(description = "ID del enlace", example = "1")
        Long id,

        @Schema(description = "Token único del enlace", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String token,

        @Schema(description = "URL completa de confirmación",
                example = "https://hostflow.com/reservations/confirm?token=a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String confirmationUrl,

        @Schema(description = "Fecha de creación del enlace", example = "2026-10-29T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Fecha de expiración del enlace", example = "2026-10-30T10:30:00")
        LocalDateTime expiresAt,

        @Schema(description = "Indica si el enlace ya fue utilizado", example = "false")
        Boolean used,

        @Schema(description = "Fecha en que fue utilizado", example = "null")
        LocalDateTime usedAt,

        @Schema(description = "Número de veces reactivado", example = "0")
        Integer reactivationCount,

        @Schema(description = "Indica si el enlace está vigente", example = "true")
        Boolean isValid,

        @Schema(description = "Indica si el enlace expiró", example = "false")
        Boolean isExpired
) {

    /**
     * Factory method que construye el DTO desde la entidad ReservationLink.
     */
    public static ReservationLinkResponse from(ReservationLink link) {
        return new ReservationLinkResponse(
                link.getId(),
                link.getToken(),
                link.getConfirmationUrl(),
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.getUsed(),
                link.getUsedAt(),
                link.getReactivationCount(),
                link.isValid(),
                link.isExpired()
        );
    }
}