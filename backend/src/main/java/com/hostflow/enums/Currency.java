package com.hostflow.enums;


import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Enumeración que define las monedas admitidas por el sistema HostFlow.
 *
 * <p>Cada moneda incluye metadatos útiles para la presentación visual
 * (símbolo, nombre) y para el redondeo correcto de los valores
 * (escala decimal). Actualmente se soportan tres monedas:</p>
 * <ul>
 *   <li>{@link #ARS} - Peso Argentino (moneda base del sistema).</li>
 *   <li>{@link #USD} - Dólar Estadounidense.</li>
 *   <li>{@link #EUR} - Euro.</li>
 * </ul>
 *
 * <p>Este enum se utiliza en múltiples entidades ({@code AcceptedCurrency},
 * {@code ExchangeRateCache}, {@code Accommodation}) para garantizar
 * consistencia de tipos en toda la plataforma.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Schema(description = "Monedas admitidas por el sistema HostFlow")
public enum Currency {

    /** Peso Argentino. Moneda base por defecto del sistema. */
    ARS("Peso Argentino", "AR$", 2),

    /** Dólar Estadounidense. Se obtiene desde DolarAPI.com (dólar blue). */
    USD("Dólar Estadounidense", "US$", 2),

    /** Euro. Se obtiene desde Frankfurter API (Banco Central Europeo). */
    EUR("Euro", "€", 2);

    /** Nombre legible de la moneda (ej: "Peso Argentino"). */
    private final String displayName;

    /** Símbolo de la moneda para presentación (ej: "AR$"). */
    private final String symbol;

    /** Cantidad de decimales por defecto para esta moneda (ej: 2 para ARS/USD/EUR). */
    private final int defaultScale;

    Currency(String displayName, String symbol, int defaultScale) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.defaultScale = defaultScale;
    }

    public String getDisplayName() { return displayName; }
    public String getSymbol() { return symbol; }
    public int getDefaultScale() { return defaultScale; }

    /**
     * Redondea un valor al scale propio de la moneda.
     *
     * <p>Ejemplo: para {@code ARS}, {@code USD} y {@code EUR} redondea a 2 decimales
     * usando {@link RoundingMode#HALF_UP} (redondeo comercial estándar).</p>
     *
     * @param value el valor a redondear
     * @return el valor redondeado, o {@code null} si el input es {@code null}
     */
    public BigDecimal scale(BigDecimal value) {
        return value == null
                ? null
                : value.setScale(defaultScale, RoundingMode.HALF_UP);
    }
}