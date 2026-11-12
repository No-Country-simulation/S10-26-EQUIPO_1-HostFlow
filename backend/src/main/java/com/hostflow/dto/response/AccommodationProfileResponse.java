package com.hostflow.dto.response;

import com.hostflow.model.Accommodation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta que representa el perfil completo del alojamiento.
 *
 * <p>Proporciona toda la información relevante del alojamiento incluyendo:
 * datos básicos, capacidad, precios, identidad visual y galería de imágenes.</p>
 *
 * <p><strong>Campos incluidos:</strong></p>
 * <ul>
 *   <li>Datos básicos: nombre, ubicación, contacto.</li>
 *   <li>Capacidad: habitaciones, baños, huéspedes.</li>
 *   <li>Identidad visual: logo, colores, patrón de fondo.</li>
 *   <li>Galería: lista de imágenes del alojamiento.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@Schema(name = "AccommodationProfileResponse", description = "Perfil completo del alojamiento con su galería de imágenes y configuración visual.")
public record AccommodationProfileResponse(
        @Schema(description = "Identificador único del alojamiento.", example = "3") Long id,
        @Schema(description = "Identificador del propietario asociado.", example = "15") Long ownerId,
        @Schema(description = "Nombre comercial del alojamiento.", example = "Cabañas del Río") String name,
        @Schema(description = "Cantidad total de habitaciones.", example = "8") Integer roomCount,
        @Schema(description = "Cantidad total de baños.", example = "4") Integer bathroomCount,
        @Schema(description = "Capacidad máxima de huéspedes.", example = "20") Integer guestCapacity,
        @Schema(description = "Precio base por noche.", example = "85000.00") BigDecimal basePrice,
        @Schema(description = "URL HTTPS del logotipo del alojamiento.", example = "https://example.com/logo.png") String logoUrl,
        @Schema(description = "ID público del logotipo en Cloudinary.", example = "hostflow/logos/accommodation-15") String logoPublicId,
        @Schema(description = "Color principal en formato hexadecimal.", example = "#0fa3a3") String primaryColor,
        @Schema(description = "Color de acento en formato hexadecimal.", example = "#d4f53c") String accentColor,
        @Schema(description = "Color terciario en formato hexadecimal.", example = "#f4b8c4") String tertiaryColor,
        @Schema(description = "Identificador del patrón de fondo.", example = "none") String backgroundPattern,
        @Schema(description = "Mensaje de bienvenida personalizado.") String welcomeMessage,
        @Schema(description = "Correo electrónico de contacto.", example = "reservas@example.com") String email,
        @Schema(description = "Teléfono de contacto.", example = "+54 11 5555 5555") String phone,
        @Schema(description = "Dirección física del alojamiento.", example = "Delta del Tigre") String address,
        @Schema(description = "Lista de imágenes de la galería del alojamiento.") List<AccommodationImageResponse> images,
        @Schema(description = "Fecha y hora de creación del perfil.") LocalDateTime createdAt,
        @Schema(description = "Fecha y hora de la última actualización.") LocalDateTime updatedAt
) {

    /**
     * Convierte una entidad {@link Accommodation} en su DTO de respuesta correspondiente.
     *
     * <p>Este método mapea todos los campos de la entidad al record de respuesta,
     * incluyendo la transformación de la lista de imágenes a sus respectivos DTOs.</p>
     *
     * @param accommodation la entidad Accommodation a convertir
     * @return el DTO AccommodationProfileResponse con los datos mapeados
     */
    public static AccommodationProfileResponse from(Accommodation accommodation) {
        return new AccommodationProfileResponse(
                accommodation.getId(),
                accommodation.getOwner().getId(),
                accommodation.getName(),
                accommodation.getRoomCount(),
                accommodation.getBathroomCount(),
                accommodation.getGuestCapacity(),
                accommodation.getBasePrice(),
                accommodation.getLogoUrl(),
                accommodation.getLogoPublicId(),
                accommodation.getPrimaryColor(),
                accommodation.getAccentColor(),
                accommodation.getTertiaryColor(),
                accommodation.getBackgroundPattern(),
                accommodation.getWelcomeMessage(),
                accommodation.getEmail(),
                accommodation.getPhone(),
                accommodation.getAddress(),
                accommodation.getImages().stream()
                        .map(AccommodationImageResponse::from)
                        .toList(),
                accommodation.getCreatedAt(),
                accommodation.getUpdatedAt()
        );
    }
}