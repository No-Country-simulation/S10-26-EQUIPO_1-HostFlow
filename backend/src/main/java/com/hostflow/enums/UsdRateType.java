package com.hostflow.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeración que define los tipos de dólar disponibles en Argentina
 * según DolarAPI.com (https://dolarapi.com).
 *
 * <p>El campo {@code casa} debe coincidir EXACTAMENTE con el valor
 * devuelto por DolarAPI.com en el campo "casa" de su respuesta JSON.</p>
 *
 * <p><strong>Valores devueltos por DolarAPI.com:</strong></p>
 * <ul>
 *   <li>{@code oficial} - Dólar oficial del Banco Nación</li>
 *   <li>{@code blue} - Dólar paralelo/informal (el más usado por turistas)</li>
 *   <li>{@code bolsa} - Dólar Bolsa/MEP (Mercado Electrónico de Pagos)</li>
 *   <li>{@code contadoconliqui} - Dólar Contado con Liquidación (CCL)</li>
 *   <li>{@code mayorista} - Dólar mayorista</li>
 *   <li>{@code cripto} - Dólar cripto (USDT/USDC)</li>
 *   <li>{@code tarjeta} - Dólar tarjeta (con impuestos incluidos, 30% PAIS + 45% Ganancias)</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@Schema(description = "Tipo de cotización del dólar argentino a utilizar")
public enum UsdRateType {

    OFICIAL("oficial", "Dólar Oficial"),
    BLUE("blue", "Dólar Blue"),
    BOLSA("bolsa", "Dólar Bolsa/MEP"),
    CONTADOCONLIQUI("contadoconliqui", "Dólar Contado con Liquidación (CCL)"),
    MAYORISTA("mayorista", "Dólar Mayorista"),
    CRIPTO("cripto", "Dólar Cripto"),
    TARJETA("tarjeta", "Dólar Tarjeta");

    /** Identificador usado por DolarAPI.com en la URL y en el campo "casa". */
    private final String casa;

    /** Nombre legible para mostrar en la UI. */
    private final String displayName;

    UsdRateType(String casa, String displayName) {
        this.casa = casa;
        this.displayName = displayName;
    }

    public String getCasa() { return casa; }
    public String getDisplayName() { return displayName; }
}