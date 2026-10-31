package com.hostflow.dto.response;

import com.hostflow.model.Guest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa un huésped en el sistema.
 *
 * <p>Se utiliza en los endpoints públicos para mostrar la información
 * del huésped sin exponer datos sensibles, incluyendo su nombre, apellido,
 * documento de identidad y datos de contacto.</p>
 *
 * <p><strong>🆕 Cambios en v2.0:</strong></p>
 * <ul>
 *   <li>Se separó {@code fullName} en {@code firstName} y {@code lastName} para mayor flexibilidad.</li>
 *   <li>Se agregó {@code documentNumber} para mostrar el documento de identidad registrado.</li>
 *   <li>Se agregó {@code updatedAt} para auditoría de cambios.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@Schema(description = "Información de un huésped registrado en el sistema")
public record GuestResponse(

        /**
         * Identificador único del huésped en la base de datos.
         */
        @Schema(description = "ID único del huésped", example = "10")
        Long id,

        /**
         * Nombre del huésped.
         */
        @Schema(description = "Nombre del huésped", example = "Carlos Fernando")
        String firstName,

        /**
         * Apellido del huésped.
         */
        @Schema(description = "Apellido del huésped", example = "Palacios")
        String lastName,

        /**
         * Nombre completo concatenado (Nombre + Apellido).
         * Se obtiene mediante el método auxiliar {@link Guest#getFullName()}.
         */
        @Schema(description = "Nombre completo del huésped", example = "Carlos Fernando Palacios")
        String fullName,

        /**
         * Correo electrónico del huésped.
         */
        @Schema(description = "Correo electrónico", example = "cfpalaciwalker@gmail.com")
        String email,

        /**
         * Teléfono de contacto del huésped.
         */
        @Schema(description = "Teléfono de contacto", example = "+54 11 5555 5555")
        String phone,

        /**
         * 🆕 Número de documento de identidad del huésped (DNI, CC, Pasaporte).
         */
        @Schema(description = "Número de documento de identidad (DNI/CC)", example = "12345678")
        String documentNumber,

        /**
         * Estado de activación del huésped en el sistema.
         */
        @Schema(description = "Indica si el huésped está activo", example = "true")
        Boolean active,

        /**
         * Fecha y hora de creación del registro.
         */
        @Schema(description = "Fecha de creación del registro", example = "2026-10-29T15:44:00")
        LocalDateTime createdAt,

        /**
         * 🆕 Fecha y hora de la última modificación del registro.
         */
        @Schema(description = "Fecha de última actualización del registro", example = "2026-10-29T15:44:00")
        LocalDateTime updatedAt
) {

    /**
     * Factory method que construye el DTO desde la entidad {@link Guest}.
     *
     * <p>Este método mapea de forma segura los campos de la entidad JPA
     * al DTO de respuesta, utilizando el método auxiliar {@code getFullName()}
     * de la entidad para mantener la compatibilidad con el formato completo.</p>
     *
     * @param guest la entidad Guest con los datos persistidos
     * @return el DTO de respuesta completo listo para serializar a JSON
     */
    public static GuestResponse from(Guest guest) {
        return new GuestResponse(
                guest.getId(),
                guest.getFirstName(),
                guest.getLastName(),
                guest.getFullName(),       // Usa el método auxiliar de la entidad
                guest.getEmail(),
                guest.getPhone(),
                guest.getDocumentNumber(), // 🆕 Campo agregado
                guest.getActive(),
                guest.getCreatedAt(),
                guest.getUpdatedAt()       // 🆕 Campo agregado
        );
    }
}