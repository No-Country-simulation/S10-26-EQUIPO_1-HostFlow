package com.hostflow.dto.response;

import com.hostflow.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * DTO de respuesta que representa un precio convertido a una moneda específica.
 *
 * <p>Se utiliza en los endpoints públicos y privados para mostrar al huésped
 * (o al propietario) el mismo valor monetario expresado en diferentes monedas
 * (ARS, USD, EUR), incluyendo el símbolo y un formato legible listo para
 * presentar en la interfaz de usuario.</p>
 *
 * <p><strong>Ejemplo de uso:</strong> Cuando un huésped consulta el precio base
 * de un alojamiento, el sistema retorna una lista de este DTO con el precio
 * en ARS, USD (dólar blue) y EUR simultáneamente.</p>
 *
 * <p><strong>Ejemplo de respuesta JSON:</strong></p>
 * <pre>
 * {
 *   "currency": "USD",
 *   "symbol": "US$",
 *   "amount": 72.56,
 *   "formatted": "US$ 72.56"
 * }
 * </pre>
 *
 * @author HostFlow Team
 * @version 1.0.0
 * @see Currency
 * @see com.hostflow.service.CurrencyConversionService
 */
@Schema(description = "Precio convertido a una moneda específica, con su símbolo y formato legible")
public record ConvertedPriceResponse(

        @Schema(description = "Código de la moneda (ARS, USD, EUR)",
                example = "USD",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Currency currency,

        @Schema(description = "Símbolo de la moneda para presentación visual",
                example = "US$",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String symbol,

        @Schema(description = "Nombre legible de la moneda",
                example = "Dólar Estadounidense",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String displayName,

        @Schema(description = "Monto numérico convertido, redondeado según la escala de la moneda",
                example = "72.56",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Schema(description = "Representación formateada del precio lista para mostrar en la UI " +
                "(ej: 'US$ 72.56', 'AR$ 85000.00', '€ 64.26')",
                example = "US$ 72.56",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String formatted
) {

    /**
     * Factory method que construye el DTO a partir de una moneda y un monto.
     *
     * <p>Genera automáticamente el campo {@code formatted} combinando el símbolo
     * de la moneda con el monto en formato plano (sin notación científica).</p>
     *
     * @param c      la moneda destino de la conversión
     * @param amount el monto ya convertido y redondeado
     * @return el DTO de respuesta completo con todos los campos poblados
     */
    public static ConvertedPriceResponse of(Currency c, BigDecimal amount) {
        String formatted = c.getSymbol() + " " + amount.toPlainString();
        return new ConvertedPriceResponse(
                c,
                c.getSymbol(),
                c.getDisplayName(),
                amount,
                formatted
        );
    }
}