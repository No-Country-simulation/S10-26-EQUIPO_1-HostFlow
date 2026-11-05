package com.hostflow.dto.request;

import com.hostflow.enums.PaymentMethodType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear o actualizar un método de pago de un huésped.
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Schema(description = "Datos de un método de pago del huésped")
public record GuestPaymentMethodRequest(

        @NotNull(message = "El tipo de método de pago es obligatorio")
        @Schema(description = "Tipo de método de pago", example = "CARD")
        PaymentMethodType paymentType,

        @NotBlank(message = "El nombre descriptivo es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        @Schema(description = "Nombre descriptivo del método", example = "Visa terminada en 1234")
        String displayName,

        @Size(max = 100)
        @Schema(description = "Proveedor del método", example = "Visa")
        String providerName,

        @Size(max = 100)
        @Schema(description = "Número de cuenta/tarjeta (ofuscado)", example = "**** **** **** 1234")
        String accountNumber,

        @Size(max = 200)
        @Schema(description = "Titular de la cuenta", example = "Juan Carlos Pérez")
        String accountHolder,

        @Size(max = 10)
        @Schema(description = "Fecha de vencimiento (MM/YY)", example = "12/28")
        String expirationDate,

        @Schema(description = "Indica si es el método predeterminado", example = "true")
        Boolean isDefault
) {}