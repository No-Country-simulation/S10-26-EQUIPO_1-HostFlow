package com.hostflow.dto.response;

import com.hostflow.enums.ReservationStatus;
import com.hostflow.enums.PaymentStatus;
import com.hostflow.model.Reservation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta con la información de una reserva.
 *
 * <p>El estado operativo y el estado de pago son independientes.
 * No incluye método de pago del huésped (fuera de alcance).</p>
 *
 * @author HostFlow Team
 * @version 2.0.0
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

        @Schema(description = "Fecha de check-in", example = "2026-12-15")
        LocalDate checkIn,

        @Schema(description = "Fecha de check-out", example = "2026-12-18")
        LocalDate checkOut,

        @Schema(description = "Número de noches", example = "3")
        Integer nights,

        @Schema(description = "Precio por noche")
        BigDecimal pricePerNight,

        @Schema(description = "Subtotal (noches × precio)")
        BigDecimal subtotal,

        @Schema(description = "Impuestos (si aplica)", example = "0")
        BigDecimal taxes,

        @Schema(description = "Importe total")
        BigDecimal totalAmount,

        @Schema(description = "Estado operativo de la reserva (independiente del pago). CONFIRMED = onboarding completado.", example = "PENDING")
        ReservationStatus status,

        @Schema(description = "Estado del pago", example = "PENDING")
        PaymentStatus paymentStatus,

        @Schema(description = "Fecha y hora en que el propietario registró el pago")
        LocalDateTime paidAt,

        @Schema(description = "Número de huéspedes", example = "2")
        Integer guestCount,

        @Schema(description = "Solicitudes especiales")
        String specialRequests,

        @Schema(description = "Enlace de onboarding (solo si aplica)")
        ReservationLinkResponse confirmationLink,

        @Schema(description = "Fecha de creación", example = "2026-10-29T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Fecha de última actualización", example = "2026-10-29T10:30:00")
        LocalDateTime updatedAt
) {

    public static ReservationResponse from(Reservation r, ReservationLinkResponse link) {
        return new ReservationResponse(
                r.getId(),
                r.getConfirmationCode(),
                r.getAccommodation().getId(),
                r.getAccommodation().getName(),
                r.getGuest() != null ? r.getGuest().getId() : null,
                r.getGuest() != null ? r.getGuest().getFullName() : r.getGuestName(),
                r.getGuest() != null ? r.getGuest().getEmail() : r.getGuestEmail(),
                r.getCheckIn(),
                r.getCheckOut(),
                r.getNights(),
                r.getPricePerNight(),
                r.getSubtotal(),
                BigDecimal.ZERO,
                r.getTotalAmount(),
                r.getStatus(),
                r.getPaymentStatus(),
                r.getPaidAt(),
                r.getGuestCount(),
                r.getSpecialRequests(),
                link,
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
