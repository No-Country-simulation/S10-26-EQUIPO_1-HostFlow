package com.hostflow.dto.response;

import com.hostflow.model.AdditionalServiceImage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una imagen de un servicio adicional.
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Schema(description = "Imagen de un servicio adicional almacenada en Cloudinary")
public record AdditionalServiceImageResponse(

        @Schema(description = "ID de la imagen", example = "12")
        Long id,

        @Schema(description = "URL HTTPS pública de la imagen",
                example = "https://res.cloudinary.com/vhuspscf/image/upload/hostflow/additional-services/5/img1.jpg")
        String imageUrl,

        @Schema(description = "Identificador público en Cloudinary",
                example = "hostflow/additional-services/5/abc123xyz")
        String cloudinaryPublicId,

        @Schema(description = "Tamaño del archivo en bytes", example = "1048576")
        Long fileSizeBytes,

        @Schema(description = "Tipo MIME del archivo", example = "image/jpeg")
        String mimeType,

        @Schema(description = "Orden de la imagen (0, 1, 2)", example = "0")
        Integer orderIndex,

        @Schema(description = "Fecha de creación")
        LocalDateTime createdAt
) {
    /**
     * Mapea la entidad {@link AdditionalServiceImage} a este DTO.
     */
    public static AdditionalServiceImageResponse from(AdditionalServiceImage img) {
        return new AdditionalServiceImageResponse(
                img.getId(),
                img.getImageUrl(),
                img.getCloudinaryPublicId(),
                img.getFileSizeBytes(),
                img.getMimeType(),
                img.getOrderIndex(),
                img.getCreatedAt()
        );
    }
}