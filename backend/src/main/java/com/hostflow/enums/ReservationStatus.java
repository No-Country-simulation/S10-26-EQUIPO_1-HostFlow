package com.hostflow.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeración que define los posibles estados de una reserva en el sistema HostFlow.
 *
 * <p><strong>Ciclo de vida típico de una reserva:</strong></p>
 * <pre>
 * PENDING → CONFIRMED → COMPLETED
 *    ↓          ↓
 * EXPIRED    CANCELLED
 * </pre>
 *
 * <p><strong>Descripción de cada estado:</strong></p>
 * <ul>
 *   <li>{@code PENDING}: Reserva creada, esperando confirmación del huésped.</li>
 *   <li>{@code CONFIRMED}: Huésped confirmó la reserva y el pago fue procesado.</li>
 *   <li>{@code CANCELLED}: Reserva cancelada por el huésped o el propietario.</li>
 *   <li>{@code EXPIRED}: El enlace de confirmación expiró sin que el huésped confirmara.</li>
 *   <li>{@code COMPLETED}: Estadía finalizada exitosamente.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.1.0
 */
@Schema(description = "Estados posibles de una reserva en el sistema")
public enum ReservationStatus {

    /**
     * Reserva creada, pendiente de confirmación por el huésped.
     * Estado inicial de toda nueva reserva.
     */
    PENDING("Pendiente"),

    /**
     * Reserva confirmada por el huésped y pago procesado exitosamente.
     * El huésped ha completado el proceso de confirmación.
     */
    CONFIRMED("Confirmada"),

    /**
     * Reserva cancelada por el huésped o el propietario.
     * Puede incluir un motivo de cancelación en el campo cancellationReason.
     */
    CANCELLED("Cancelada"),

    /**
     * El enlace de confirmación expiró sin que el huésped confirmara la reserva.
     * Ocurre cuando el tiempo límite de confirmación ha pasado.
     */
    EXPIRED("Expirada"),

    /**
     * Estadía finalizada exitosamente.
     * El huésped completó su stay y no hay acciones pendientes.
     */
    COMPLETED("Completada");

    // =========================================================================
    // ATRIBUTOS
    // =========================================================================

    /**
     * Nombre legible en español del estado de la reserva.
     * Utilizado para mostrar información amigable al usuario en la interfaz.
     */
    private final String displayName;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Constructor privado del enum.
     *
     * @param displayName nombre legible en español del estado
     */
    ReservationStatus(String displayName) {
        this.displayName = displayName;
    }

    // =========================================================================
    // MÉTODOS
    // =========================================================================

    /**
     * Obtiene el nombre legible en español del estado de la reserva.
     *
     * @return nombre display del estado (ej: "Pendiente", "Confirmada")
     */
    public String getDisplayName() {
        return displayName;
    }
}