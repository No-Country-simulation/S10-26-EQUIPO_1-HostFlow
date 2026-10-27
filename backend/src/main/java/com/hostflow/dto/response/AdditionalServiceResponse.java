package com.hostflow.dto.response;


import com.hostflow.enums.Currency;
import com.hostflow.model.AdditionalService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de respuesta que representa un servicio adicional del alojamiento
 * con sus precios convertidos a todas las monedas habilitadas.
 *
 * <p>Se utiliza en los endpoints del panel del propietario y en los
 * endpoints públicos para mostrar a los huéspedes los servicios extras
 * disponibles (desayuno, transfer, spa, etc.) con sus precios en ARS,
 * USD y EUR simultáneamente.</p>
 *
 * <p><strong>Ejemplo de uso:</strong> Cuando un huésped consulta
 * {@code GET /public/accommodations/{id}/additional-services}, recibe
 * una lista de este DTO con cada servicio y sus precios en todas las
 * monedas habilitadas por el propietario.</p>
 *
 * <p><strong>Ejemplo de respuesta JSON:</strong></p>
 * <pre>
 * {
 *   "id": 1,
 *   "name": "Desayuno buffet",
 *   "description": "Desayuno continental servido de 8:00 a 10:00 hs",
 *   "baseCurrency": "ARS",
 *   "basePrice": 5000.00,
 *   "convertedPrices": [
 *     { "currency": "ARS", "symbol": "AR$", "displayName": "Peso Argentino", "amount": 5000.00, "formatted": "AR$ 5000.00" },
 *     { "currency": "USD", "symbol": "US$", "displayName": "Dólar Estadounidense", "amount": 4.27, "formatted": "US$ 4.27" },
 *     { "currency": "EUR", "symbol": "€", "displayName": "Euro", "amount": 3.78, "formatted": "€ 3.78" }
 *   ],
 *   "active": true
 * }
 * </pre>
 *
 * @author HostFlow Team
 * @version 1.0.0
 * @see AdditionalService
 * @see ConvertedPriceResponse
 */
@Schema(description = "Servicio adicional del alojamiento con precios convertidos a todas las monedas habilitadas")
public record AdditionalServiceResponse(

        @Schema(description = "Identificador único del servicio adicional",
                example = "1")
        Long id,

        @Schema(description = "Nombre del servicio adicional",
                example = "Desayuno buffet",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Descripción detallada del servicio",
                example = "Desayuno continental servido de 8:00 a 10:00 hs en el comedor principal",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String description,

        @Schema(description = "Moneda base en la que se almacena el precio (generalmente ARS)",
                example = "ARS",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Currency baseCurrency,

        @Schema(description = "Precio del servicio en la moneda base",
                example = "5000.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal basePrice,

        @Schema(description = "Lista de precios convertidos a todas las monedas habilitadas " +
                "por el propietario (ARS, USD, EUR). Cada entrada incluye el monto numérico " +
                "y una versión formateada lista para mostrar en la UI.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<ConvertedPriceResponse> convertedPrices,

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
     * JPA en un DTO enriquecido con los precios en todas las monedas habilitadas.</p>
     *
     * @param s         la entidad AdditionalService con los datos del servicio
     * @param converted la lista de precios convertidos a todas las monedas habilitadas
     * @return el DTO de respuesta completo con todos los campos poblados
     */
    public static AdditionalServiceResponse from(AdditionalService s,
                                                 List<ConvertedPriceResponse> converted) {
        return new AdditionalServiceResponse(
                s.getId(),
                s.getName(),
                s.getDescription(),
                s.getAccommodation().getBaseCurrency(),
                s.getBasePrice(),
                converted,
                s.isActive()
        );
    }
}