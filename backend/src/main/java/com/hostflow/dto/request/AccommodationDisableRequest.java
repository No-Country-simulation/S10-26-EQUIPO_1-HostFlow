package com.hostflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO para solicitar la desactivación temporal de un alojamiento.
 *
 * @author HostFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationDisableRequest {

    /**
     * Motivo de la desactivación temporal.
     */
    @NotBlank(message = "El motivo de la desactivación es obligatorio")
    private String reason;

    /**
     * Fecha y hora de inicio de la desactivación.
     */
    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime disabledFrom;

    /**
     * Fecha y hora estimada de reactivación (opcional).
     */
    private LocalDateTime disabledUntil;
}