package com.hostflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para confirmar una reserva usando el enlace de confirmación.
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Schema(description = "Datos para confirmar una reserva")
public record ConfirmReservationRequest(

        @NotNull(message = "El ID del método de pago es obligatorio")
        @Schema(description = "ID del método de pago a utilizar para esta reserva", example = "5")
        Long paymentMethodId
) {}