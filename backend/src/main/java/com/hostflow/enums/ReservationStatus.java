package com.hostflow.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estados operativos de una reserva en HostFlow.
 *
 * <p>El estado de la reserva es independiente del estado de pago ({@code PaymentStatus})
 * y del ciclo de vida del enlace de onboarding.</p>
 *
 * <p><strong>Ciclo de vida típico:</strong></p>
 * <pre>
 * PENDING → CONFIRMED → COMPLETED
 *    ↓          ↓
 * CANCELLED  CANCELLED
 * </pre>
 *
 * <ul>
 *   <li>{@code PENDING}: el propietario creó la reserva; falta el onboarding del huésped.</li>
 *   <li>{@code CONFIRMED}: el huésped completó el onboarding. <strong>No implica pago recibido.</strong></li>
 *   <li>{@code CANCELLED}: el propietario canceló; deja de bloquear fechas.</li>
 *   <li>{@code COMPLETED}: estadía finalizada según las reglas operativas del dashboard.</li>
 *   <li>{@code EXPIRED}: valor legado. La caducidad del enlace <strong>no</strong> cambia el estado
 *       de la reserva ni libera fechas. No usar en lógica nueva.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@Schema(description = "Estados operativos de una reserva (independientes del estado de pago)")
public enum ReservationStatus {

    /**
     * Reserva creada por el propietario; pendiente de onboarding del huésped.
     * Bloquea fechas aunque el enlace expire.
     */
    PENDING("Pendiente"),

    /**
     * Onboarding del huésped completado.
     * El pago se gestiona por separado mediante {@code PaymentStatus}.
     */
    CONFIRMED("Confirmada"),

    /**
     * Reserva cancelada por el propietario.
     * Invalida el enlace y deja de bloquear fechas.
     */
    CANCELLED("Cancelada"),

    /**
     * Valor legado. No representa la caducidad del enlace de onboarding.
     * Conservado solo por compatibilidad con datos históricos.
     */
    EXPIRED("Expirada"),

    /**
     * Estadía finalizada (típicamente después del check-out).
     */
    COMPLETED("Completada");

    private final String displayName;

    ReservationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
