package com.hostflow.dto.request;

import com.hostflow.enums.Currency;
import com.hostflow.enums.UsdRateType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO para crear o actualizar la configuración de una moneda en un alojamiento.
 *
 * <p>Se utiliza en el endpoint {@code PUT /owners/accommodation/currencies}
 * para que el propietario habilite/deshabilite monedas y configure sus tasas
 * de conversión (automáticas o manuales).</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Schema(description = "Configuración de una moneda habilitada para el alojamiento")
public record AcceptedCurrencyRequest(

        @NotNull(message = "currency es obligatorio")
        @Schema(description = "Moneda a configurar (ARS, USD, EUR)",
                example = "USD",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Currency currency,

        @NotNull(message = "enabled es obligatorio")
        @Schema(description = "Si la moneda está habilitada para cobro",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean enabled,

        @NotNull(message = "useAutomaticRate es obligatorio")
        @Schema(description = "Si se usa tasa automática desde API externa " +
                "(true) o tasa manual (false)",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean useAutomaticRate,

        @DecimalMin(value = "0.0000000001",
                message = "manualRate debe ser mayor a 0 cuando se usa tasa manual")
        @Schema(description = "Tasa manual de conversión (solo obligatorio si " +
                "useAutomaticRate=false). Representa cuántas unidades de esta " +
                "moneda equivalen a 1 ARS. Ejemplo: 0.00085 significa que 1 ARS = 0.00085 USD",
                example = "0.00085",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal manualRate,

        @Schema(description = "Tipo de cotización del dólar (solo aplica para USD). " +
                "Opciones: BLUE, OFICIAL, MEP, CRYPTO, TARJETA, MAYORISTA",
                example = "BLUE",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        UsdRateType usdRateType
) {}