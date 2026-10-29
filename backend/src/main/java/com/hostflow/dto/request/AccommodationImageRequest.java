package com.hostflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@Schema(name = "AccommodationImageRequest", description = "Referencia HTTPS a una imagen de la galería. No representa una carga de archivo.")
public record AccommodationImageRequest(
        @Schema(description = "URL HTTPS de la imagen que se desea añadir.", example = "https://example.com/images/cabin-exterior.jpg", maxLength = 2048, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @URL(protocol = "https", message = "url debe ser una URL HTTPS válida") @Size(max = 2048) String url
) {}
