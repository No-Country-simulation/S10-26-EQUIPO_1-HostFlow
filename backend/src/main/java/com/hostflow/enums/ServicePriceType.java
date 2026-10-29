package com.hostflow.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeración que define el tipo de cobro de un servicio adicional.
 *
 * <p>Permite al propietario especificar si el precio del servicio
 * se cobra por día de estadía o como un monto único total.</p>
 *
 * <ul>
 *   <li>{@link #PER_DAY} - Precio por cada día de estadía
 *       (ej: desayuno buffet $5000/día).</li>
 *   <li>{@link #TOTAL} - Precio único total, independiente de la duración
 *       (ej: transfer al aeropuerto $25000 total).</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Schema(description = "Tipo de cobro del servicio adicional")
public enum ServicePriceType {

    /** Precio que se cobra por cada día de estadía del huésped. */
    PER_DAY("Por día"),

    /** Precio único total, independiente de la duración de la estadía. */
    TOTAL("Total");

    /** Descripción legible para mostrar en la UI. */
    private final String displayName;

    ServicePriceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}