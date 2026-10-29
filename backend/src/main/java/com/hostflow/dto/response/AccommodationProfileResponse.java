package com.hostflow.dto.response;

import com.hostflow.model.Accommodation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "AccommodationProfileResponse", description = "Perfil persistido del alojamiento, con las referencias de su galería.")
public record AccommodationProfileResponse(
        @Schema(description = "Identificador del alojamiento.", example = "3") Long id,
        @Schema(description = "Identificador del propietario asociado.", example = "15") Long ownerId,
        @Schema(description = "Nombre comercial.", example = "Cabañas del Río") String name,
        @Schema(description = "Cantidad de habitaciones.", example = "8") Integer roomCount,
        @Schema(description = "Cantidad de baños.", example = "4") Integer bathroomCount,
        @Schema(description = "Capacidad de huéspedes.", example = "20") Integer guestCapacity,
        @Schema(description = "Precio base.", example = "85000.00") BigDecimal basePrice,
        @Schema(description = "URL HTTPS del logotipo.", example = "https://example.com/logo.png") String logoUrl,
        @Schema(description = "Color principal hexadecimal.", example = "#0fa3a3") String primaryColor,
        @Schema(description = "Color de acento hexadecimal.", example = "#d4f53c") String accentColor,
        @Schema(description = "Color terciario hexadecimal.", example = "#f4b8c4") String tertiaryColor,
        @Schema(description = "Identificador del patrón de fondo.", example = "none") String backgroundPattern,
        @Schema(description = "Mensaje de bienvenida.") String welcomeMessage,
        @Schema(description = "Correo de contacto.", example = "reservas@example.com") String email,
        @Schema(description = "Teléfono de contacto.", example = "+54 11 5555 5555") String phone,
        @Schema(description = "Dirección del alojamiento.", example = "Delta del Tigre") String address,
        @Schema(description = "Lista de imágenes de la galería.") List<AccommodationImageResponse> images,
        @Schema(description = "Fecha de creación del perfil.") LocalDateTime createdAt,
        @Schema(description = "Fecha de última actualización.") LocalDateTime updatedAt
) {
    public static AccommodationProfileResponse from(Accommodation a) {
        return new AccommodationProfileResponse(a.getId(), a.getOwner().getId(), a.getName(),
                a.getRoomCount(), a.getBathroomCount(), a.getGuestCapacity(), a.getBasePrice(),
                a.getLogoUrl(), a.getPrimaryColor(), a.getAccentColor(), a.getTertiaryColor(),
                a.getBackgroundPattern(), a.getWelcomeMessage(), a.getEmail(), a.getPhone(),
                a.getAddress(),
                a.getImages().stream().map(AccommodationImageResponse::from).toList(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
