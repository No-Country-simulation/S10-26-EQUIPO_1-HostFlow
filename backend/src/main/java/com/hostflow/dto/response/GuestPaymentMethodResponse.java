package com.hostflow.dto.response;

import com.hostflow.enums.PaymentMethodType;
import com.hostflow.model.GuestPaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa un método de pago de un huésped.
 *
 * <p>Se utiliza en los endpoints de reservas para mostrar al propietario
 * o al huésped los métodos de pago disponibles para confirmar una reserva.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 * @see GuestPaymentMethod
 */
@Schema(description = "Método de pago de un huésped")
public record GuestPaymentMethodResponse(

        @Schema(description = "ID del método de pago", example = "5")
        Long id,

        @Schema(description = "Tipo de método de pago", example = "CARD")
        PaymentMethodType paymentType,

        @Schema(description = "Nombre descriptivo del método", example = "Visa terminada en 1234")
        String displayName,

        @Schema(description = "Proveedor del método", example = "Visa")
        String providerName,

        @Schema(description = "Número de cuenta/tarjeta (ofuscado)", example = "**** **** **** 1234")
        String accountNumber,

        @Schema(description = "Titular de la cuenta", example = "Juan Carlos Pérez")
        String accountHolder,

        @Schema(description = "Fecha de vencimiento (MM/YY)", example = "12/28")
        String expirationDate,

        @Schema(description = "Indica si es el método predeterminado", example = "true")
        boolean isDefault,

        @Schema(description = "Indica si el método está activo", example = "true")
        boolean isActive,

        @Schema(description = "Fecha de creación", example = "2026-10-29T10:30:00")
        LocalDateTime createdAt
) {

    /**
     * Factory method que construye el DTO desde la entidad {@link GuestPaymentMethod}.
     *
     * @param method la entidad GuestPaymentMethod
     * @return el DTO de respuesta completo
     */
    public static GuestPaymentMethodResponse from(GuestPaymentMethod method) {
        return new GuestPaymentMethodResponse(
                method.getId(),
                method.getPaymentType(),
                method.getDisplayName(),
                method.getProviderName(),
                method.getAccountNumber(),
                method.getAccountHolder(),
                method.getExpirationDate(),
                method.isDefault(),
                method.isActive(),
                method.getCreatedAt()
        );
    }
}