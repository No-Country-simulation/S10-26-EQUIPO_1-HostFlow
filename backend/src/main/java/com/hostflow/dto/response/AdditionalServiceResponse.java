package com.hostflow.dto.response;

import com.hostflow.enums.Currency;
import com.hostflow.enums.ServicePriceType;
import com.hostflow.model.AdditionalService;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de respuesta que representa un servicio adicional del alojamiento
 * con sus precios convertidos a todas las monedas habilitadas y sus imágenes asociadas.
 *
 * <p>Se utiliza en los endpoints del panel del propietario y en los
 * endpoints públicos para mostrar a los huéspedes los servicios extras
 * disponibles (desayuno, transfer, spa, etc.) con sus precios en ARS,
 * USD y EUR simultáneamente, junto con la galería de imágenes del servicio.</p>
 *
 * <p><strong>Ejemplo de uso:</strong> Cuando un huésped consulta
 * {@code GET /public/accommodations/{id}/additional-services}, recibe
 * una lista de este DTO con cada servicio, sus precios en todas las
 * monedas habilitadas y sus imágenes.</p>
 *
 * <p><strong>Ejemplo de respuesta JSON:</strong></p>
 * <pre>
 * {
 *   "id": 1,
 *   "name": "Desayuno buffet",
 *   "description": "Desayuno continental servido de 8:00 a 10:00 hs",
 *   "baseCurrency": "ARS",
 *   "basePrice": 5000.00,
 *   "priceType": "PER_DAY",
 *   "convertedPrices": [
 *     { "currency": "ARS", "symbol": "AR$", "displayName": "Peso Argentino", "amount": 5000.00, "formatted": "AR$ 5000.00" },
 *     { "currency": "USD", "symbol": "US$", "displayName": "Dólar Estadounidense", "amount": 4.27, "formatted": "US$ 4.27" },
 *     { "currency": "EUR", "symbol": "€", "displayName": "Euro", "amount": 3.78, "formatted": "€ 3.78" }
 *   ],
 *   "images": [
 *     { "id": 10, "imageUrl": "https://res.cloudinary.com/...", "orderIndex": 0 }
 *   ],
 *   "active": true
 * }
 * </pre>
 *
 * @author HostFlow Team
 * @version 2.0.0
 * @see AdditionalService
 * @see ConvertedPriceResponse
 * @see AdditionalServiceImageResponse
 */
@Schema(description = "Servicio adicional del alojamiento con precios convertidos, tipo de cobro e imágenes")
public record AdditionalServiceResponse(

        /**
         * Identificador único del servicio adicional en la base de datos.
         */
        @Schema(description = "Identificador único del servicio adicional",
                example = "1")
        Long id,

        /**
         * Nombre comercial o descriptivo del servicio.
         */
        @Schema(description = "Nombre del servicio adicional",
                example = "Desayuno buffet",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        /**
         * Descripción detallada del servicio, horarios o condiciones.
         */
        @Schema(description = "Descripción detallada del servicio",
                example = "Desayuno continental servido de 8:00 a 10:00 hs en el comedor principal",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String description,

        /**
         * Moneda base en la que se almacena el precio (generalmente ARS).
         */
        @Schema(description = "Moneda base en la que se almacena el precio (generalmente ARS)",
                example = "ARS",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Currency baseCurrency,

        /**
         * Precio del servicio en la moneda base.
         */
        @Schema(description = "Precio del servicio en la moneda base",
                example = "5000.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal basePrice,

        /**
         * 🆕 Tipo de cobro aplicado a este servicio.
         * Define si el precio se multiplica por los días de estadía (PER_DAY)
         * o es un monto único independiente de la duración (TOTAL).
         */
        @Schema(description = "Tipo de cobro: PER_DAY (por cada día de estadía) o TOTAL (monto único)",
                example = "PER_DAY",
                requiredMode = Schema.RequiredMode.REQUIRED)
        ServicePriceType priceType,

        /**
         * Lista de precios convertidos a todas las monedas habilitadas por el propietario.
         */
        @Schema(description = "Lista de precios convertidos a todas las monedas habilitadas " +
                "por el propietario (ARS, USD, EUR). Cada entrada incluye el monto numérico " +
                "y una versión formateada lista para mostrar en la UI.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<ConvertedPriceResponse> convertedPrices,

        /**
         * 🆕 Lista de imágenes asociadas al servicio (máximo 3).
         * Almacenadas en Cloudinary, incluyen URL, tamaño y orden de visualización.
         */
        @Schema(description = "Lista de imágenes asociadas al servicio (máximo 3), " +
                "almacenadas en Cloudinary. Incluye URL, tamaño y orden de visualización.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<AdditionalServiceImageResponse> images,

        /**
         * Estado de visibilidad del servicio para los huéspedes.
         */
        @Schema(description = "Indica si el servicio está activo y visible para los huéspedes",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean active
) {

    /**
     * Factory method que construye el DTO de respuesta a partir de la entidad
     * {@link AdditionalService} y la lista de precios ya convertidos.
     *
     * <p>Este método se utiliza en los controladores para transformar la entidad
     * JPA en un DTO enriquecido con los precios en todas las monedas habilitadas
     * y las imágenes asociadas (las cuales ya vienen cargadas en memoria gracias
     * al {@code @EntityGraph} configurado en el repositorio, evitando el problema N+1).</p>
     *
     * @param s         la entidad AdditionalService con los datos del servicio
     * @param converted la lista de precios convertidos a todas las monedas habilitadas
     * @return el DTO de respuesta completo con todos los campos poblados
     */
    public static AdditionalServiceResponse from(AdditionalService s,
                                                 List<ConvertedPriceResponse> converted) {

        // 🆕 Mapear las imágenes de la entidad a sus respectivos DTOs.
        // Si la lista es nula (no debería ocurrir gracias a @EntityGraph),
        // se retorna una lista vacía para evitar NullPointerException en el frontend.
        List<AdditionalServiceImageResponse> imageResponses = s.getImages() != null
                ? s.getImages().stream()
                .map(AdditionalServiceImageResponse::from)
                .toList()
                : List.of();

        return new AdditionalServiceResponse(
                s.getId(),
                s.getName(),
                s.getDescription(),
                s.getAccommodation().getBaseCurrency(),
                s.getBasePrice(),
                s.getPriceType(),          // 🆕 Campo agregado
                converted,
                imageResponses,            // 🆕 Campo agregado
                s.isActive()
        );
    }
}