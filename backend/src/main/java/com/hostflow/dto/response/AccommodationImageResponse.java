package com.hostflow.dto.response;

import com.hostflow.model.AccommodationImage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "AccommodationImageResponse", description = "Imagen registrada en la galería como una referencia URL.")
public record AccommodationImageResponse(
        @Schema(description = "Identificador de la imagen.", example = "12") Long id,
        @Schema(description = "URL HTTPS de la imagen.", example = "https://example.com/images/cabin-exterior.jpg") String url,
        @Schema(description = "Fecha de registro de la imagen.") LocalDateTime createdAt
) {
    public static AccommodationImageResponse from(AccommodationImage image) {
        return new AccommodationImageResponse(image.getId(), image.getImageUrl(), image.getCreatedAt());
    }
}
