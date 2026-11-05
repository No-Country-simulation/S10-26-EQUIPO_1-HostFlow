package com.hostflow.dto.response;

import com.hostflow.enums.Currency;
import com.hostflow.enums.UsdRateType;
import com.hostflow.model.AcceptedCurrency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa la configuración de una moneda habilitada
 * para un alojamiento, incluyendo su tasa de conversión efectiva actual.
 *
 * <p>Se utiliza en los endpoints del panel del propietario para mostrar:</p>
 * <ul>
 *   <li>Qué monedas están habilitadas (ARS, USD, EUR).</li>
 *   <li>Si usan tasa automática (API externa) o manual.</li>
 *   <li>La tasa efectiva actual que se está aplicando en las conversiones.</li>
 *   <li>Para USD: el tipo de cotización elegido (blue, oficial, MEP, etc.).</li>
 * </ul>
 *
 * <p><strong>Ejemplo de uso:</strong> Cuando el propietario consulta
 * {@code GET /owners/accommodation/currencies}, recibe una lista de este DTO
 * con el estado actual de cada moneda configurada.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 * @see AcceptedCurrency
 */
@Schema(description = "Configuración de una moneda habilitada para el alojamiento, " +
        "incluyendo su tasa de conversión efectiva actual")
public record AcceptedCurrencyResponse(

        @Schema(description = "Identificador único de la configuración de moneda",
                example = "1")
        Long id,

        @Schema(description = "Código de la moneda (ARS, USD, EUR)",
                example = "USD",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Currency currency,

        @Schema(description = "Símbolo de la moneda para presentación visual",
                example = "US$")
        String symbol,

        @Schema(description = "Nombre legible de la moneda",
                example = "Dólar Estadounidense")
        String displayName,

        @Schema(description = "Indica si la moneda está habilitada para cobro a huéspedes",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean enabled,

        @Schema(description = "Si es true, se usa la tasa automática desde API externa " +
                "(DolarAPI para USD, Frankfurter para EUR). " +
                "Si es false, se usa la tasa manual configurada por el propietario.",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean useAutomaticRate,

        @Schema(description = "Tasa manual de conversión configurada por el propietario. " +
                "Solo tiene valor cuando useAutomaticRate=false. " +
                "Representa cuántas unidades de esta moneda equivalen a 1 ARS. " +
                "Ejemplo: 0.00085 significa que 1 ARS = 0.00085 USD",
                example = "0.00085",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal manualRate,

        @Schema(description = "Tipo de cotización del dólar utilizado (solo para USD). " +
                "Opciones: BLUE, OFICIAL, MEP, CRYPTO, TARJETA, MAYORISTA. " +
                "Es null para EUR o cuando se usa tasa manual.",
                example = "BLUE",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UsdRateType usdRateType,

        @Schema(description = "Tasa efectiva actual que se está aplicando en las conversiones. " +
                "Si useAutomaticRate=true, esta es la tasa cacheada desde la API externa. " +
                "Si useAutomaticRate=false, es igual a manualRate. " +
                "Puede ser null si no se pudo resolver la tasa (API caída o no configurada).",
                example = "0.0008547009",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal effectiveRate,

        @Schema(description = "Fecha y hora de la última modificación de esta configuración",
                example = "2026-10-27T15:30:00")
        LocalDateTime updatedAt
) {

    /**
     * Factory method que construye el DTO de respuesta a partir de la entidad
     * {@link AcceptedCurrency} y la tasa efectiva calculada.
     *
     * <p>Este método se utiliza en los controladores para transformar la entidad
     * JPA en un DTO seguro para el cliente, enriquecido con la tasa efectiva
     * actual (que puede provenir de la caché de la API o ser la tasa manual).</p>
     *
     * @param c            la entidad AcceptedCurrency con la configuración de la moneda
     * @param effectiveRate la tasa efectiva actual (automática o manual) que se está aplicando
     * @return el DTO de respuesta completo con todos los campos poblados
     */
    public static AcceptedCurrencyResponse from(AcceptedCurrency c, BigDecimal effectiveRate) {
        return new AcceptedCurrencyResponse(
                c.getId(),
                c.getCurrency(),
                c.getCurrency().getSymbol(),
                c.getCurrency().getDisplayName(),
                c.isEnabled(),
                c.isUseAutomaticRate(),
                c.getManualRate(),
                c.getUsdRateType(),
                effectiveRate,
                c.getUpdatedAt()
        );
    }
}