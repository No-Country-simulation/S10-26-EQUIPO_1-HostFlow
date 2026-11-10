package com.hostflow.dto.response;

import com.hostflow.model.AccommodationImage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * DTO para la respuesta de una imagen de la galería del alojamiento.
 *
 * <p>Expone únicamente los campos necesarios para la API REST, ocultando
 * la estructura interna de la entidad JPA {@link AccommodationImage}.</p>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@Schema(name = "AccommodationImageResponse", description = "Imagen registrada en la galería como una referencia URL.")
public record AccommodationImageResponse(
        @Schema(description = "Identificador único de la imagen.", example = "12") Long id,
        @Schema(description = "URL HTTPS segura de la imagen.", example = "https://example.com/images/cabin-exterior.jpg") String url,
        @Schema(description = "ID público del recurso en Cloudinary (si aplica).", example = "hostflow/galeria-4") String cloudinaryPublicId,
        @Schema(description = "Fecha y hora de registro de la imagen.") LocalDateTime createdAt
) {
    /**
     * Mapea la entidad JPA {@link AccommodationImage} a este DTO.
     *
     * @param image la entidad JPA a convertir
     * @return el DTO con los datos mapeados
     */
    public static AccommodationImageResponse from(AccommodationImage image) {
        return new AccommodationImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getCloudinaryPublicId(),
                image.getCreatedAt()
        );
    }
}