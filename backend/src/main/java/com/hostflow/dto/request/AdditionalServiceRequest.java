package com.hostflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * DTO para crear o actualizar un servicio adicional del alojamiento.
 *
 * <p>Se utiliza en los endpoints de gestión de servicios adicionales
 * para que el propietario pueda registrar servicios extras como:</p>
 * <ul>
 *   <li>Desayuno buffet</li>
 *   <li>Transfer al aeropuerto</li>
 *   <li>Spa / masajes</li>
 *   <li>Estacionamiento</li>
 *   <li>Mascotas</li>
 *   <li>Early check-in / Late check-out</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Schema(description = "Datos de un servicio adicional del alojamiento")
public record AdditionalServiceRequest(

        @NotBlank(message = "El nombre del servicio es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        @Schema(description = "Nombre del servicio adicional",
                example = "Desayuno buffet",
                maxLength = 150,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        @Schema(description = "Descripción detallada del servicio",
                example = "Desayuno continental servido de 8:00 a 10:00 hs en el comedor principal",
                maxLength = 1000,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String description,

        @NotNull(message = "El precio base es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio base debe ser mayor a 0")
        @Schema(description = "Precio del servicio en la moneda base del alojamiento (ARS)",
                example = "5000.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal basePrice,

        @Schema(description = "Indica si el servicio está activo y visible para los huéspedes",
                example = "true",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean active
) {}