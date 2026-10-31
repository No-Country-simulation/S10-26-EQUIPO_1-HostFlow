package com.hostflow.dto.response;

import com.hostflow.enums.ReservationStatus;
import com.hostflow.model.Reservation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una reserva completa con toda su información.
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Schema(description = "Información completa de una reserva")
public record ReservationResponse(

        @Schema(description = "ID único de la reserva", example = "1")
        Long id,

        @Schema(description = "Código de confirmación único", example = "RES-A1B2C3")
        String confirmationCode,

        @Schema(description = "ID del alojamiento reservado", example = "5")
        Long accommodationId,

        @Schema(description = "Nombre del alojamiento", example = "Cabañas del Río")
        String accommodationName,

        @Schema(description = "ID del huésped", example = "10")
        Long guestId,

        @Schema(description = "Nombre completo del huésped", example = "Juan Carlos Pérez")
        String guestFullName,

        @Schema(description = "Correo del huésped", example = "juan.perez@email.com")
        String guestEmail,

        @Schema(description = "Fecha de check-in", example = "2026-12-15T14:00:00")
        LocalDateTime checkIn,

        @Schema(description = "Fecha de check-out", example = "2026-12-18T11:00:00")
        LocalDateTime checkOut,

        @Schema(description = "Número de noches", example = "3")
        Integer nights,

        @Schema(description = "Precio por noche", example = "85000.00")
        BigDecimal pricePerNight,

        @Schema(description = "Subtotal", example = "255000.00")
        BigDecimal subtotal,

        @Schema(description = "Impuestos", example = "25500.00")
        BigDecimal taxes,

        @Schema(description = "Monto total", example = "280500.00")
        BigDecimal totalAmount,

        @Schema(description = "Estado de la reserva", example = "PENDING")
        ReservationStatus status,

        @Schema(description = "Número de huéspedes", example = "2")
        Integer guestCount,

        @Schema(description = "Solicitudes especiales")
        String specialRequests,

        @Schema(description = "Método de pago utilizado")
        GuestPaymentMethodResponse paymentMethod,

        @Schema(description = "Enlace de confirmación (solo si está pendiente)")
        ReservationLinkResponse confirmationLink,

        @Schema(description = "Fecha de creación", example = "2026-10-29T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Fecha de última actualización", example = "2026-10-29T10:30:00")
        LocalDateTime updatedAt
) {

    /**
     * Factory method que construye el DTO desde la entidad Reservation.
     *
     * @param r la entidad Reservation
     * @param link el enlace de confirmación asociado (puede ser null)
     * @return el DTO de respuesta completo
     */
    public static ReservationResponse from(Reservation r, ReservationLinkResponse link) {
        GuestPaymentMethodResponse paymentMethodResponse = r.getPaymentMethod() != null
                ? GuestPaymentMethodResponse.from(r.getPaymentMethod())
                : null;

        return new ReservationResponse(
                r.getId(),
                r.getConfirmationCode(),
                r.getAccommodation().getId(),
                r.getAccommodation().getName(),
                r.getGuest().getId(),
                r.getGuest().getFullName(),
                r.getGuest().getEmail(),
                r.getCheckIn(),
                r.getCheckOut(),
                r.getNights(),
                r.getPricePerNight(),
                r.getSubtotal(),
                r.getTaxes(),
                r.getTotalAmount(),
                r.getStatus(),
                r.getGuestCount(),
                r.getSpecialRequests(),
                paymentMethodResponse,
                link,
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}