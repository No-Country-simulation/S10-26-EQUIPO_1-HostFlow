package com.hostflow.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeración que define los posibles estados de una reserva.
 *
 * <p>Ciclo de vida típico de una reserva:</p>
 * <pre>
 * PENDING → CONFIRMED → COMPLETED
 *    ↓          ↓
 * EXPIRED    CANCELLED
 * </pre>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Schema(description = "Estados posibles de una reserva")
public enum ReservationStatus {

    /** Reserva creada, pendiente de confirmación por el huésped. */
    PENDING("Pendiente"),

    /** Reserva confirmada por el huésped y pago procesado. */
    CONFIRMED("Confirmada"),

    /** Reserva cancelada por el huésped o el propietario. */
    CANCELLED("Cancelada"),

    /** El enlace de confirmación expiró sin que el huésped confirmara. */
    EXPIRED("Expirada"),

    /** Estadía finalizada exitosamente. */
    COMPLETED("Completada");

    private final String displayName;

    ReservationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}