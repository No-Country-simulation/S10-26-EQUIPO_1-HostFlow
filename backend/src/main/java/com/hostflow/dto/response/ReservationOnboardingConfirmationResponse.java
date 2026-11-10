package com.hostflow.dto.response;

import com.hostflow.enums.Currency;
import com.hostflow.enums.PaymentStatus;
import com.hostflow.enums.ReservationStatus;
import com.hostflow.enums.ServicePriceType;
import com.hostflow.model.Reservation;
import com.hostflow.model.ReservationAdditionalService;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen devuelto después de completar correctamente el onboarding de una reserva.
 * Los importes corresponden a los valores persistidos en la reserva y sus snapshots.
 */
@Schema(description = "Confirmación del onboarding y resumen económico de la reserva")
public record ReservationOnboardingConfirmationResponse(
        @Schema(description = "Identificador de la reserva")
        Long reservationId,
        @Schema(description = "Código de confirmación", example = "RES-A1B2C3")
        String confirmationCode,
        Long accommodationId,
        String accommodationName,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer nights,
        Integer guestCount,
        BigDecimal pricePerNight,
        BigDecimal accommodationSubtotal,
        BigDecimal additionalServicesTotal,
        BigDecimal totalAmount,
        Currency currency,
        ReservationStatus reservationStatus,
        PaymentStatus paymentStatus,
        Boolean boatTransferRequested,
        List<SelectedAdditionalService> selectedAdditionalServices,
        List<ReservationOnboardingDetailsResponse.PaymentOption> paymentMethods,
        List<ReservationOnboardingDetailsResponse.CurrencyEquivalent> currencyEquivalents
) {
    @Schema(description = "Servicio adicional contratado con su precio histórico aplicado")
    public record SelectedAdditionalService(
            Long serviceId,
            String name,
            Integer quantity,
            BigDecimal unitPrice,
            ServicePriceType priceType,
            Integer nightsApplied,
            BigDecimal lineTotal,
            Currency currency
    ) {
        public static SelectedAdditionalService from(ReservationAdditionalService service) {
            return new SelectedAdditionalService(
                    service.getAdditionalService() != null
                            ? service.getAdditionalService().getId() : null,
                    service.getServiceNameSnapshot(),
                    service.getQuantity(),
                    service.getUnitPrice(),
                    service.getPriceType(),
                    service.getNightsApplied(),
                    service.getLineTotal(),
                    service.getCurrency()
            );
        }
    }

    /**
     * Construye la respuesta a partir de la reserva persistida.
     * Los métodos de pago deben proceder del alojamiento de esta reserva y estar activos.
     */
    public static ReservationOnboardingConfirmationResponse from(
            Reservation reservation,
            List<ReservationOnboardingDetailsResponse.PaymentOption> paymentMethods,
            List<ReservationOnboardingDetailsResponse.CurrencyEquivalent> currencyEquivalents) {
        List<SelectedAdditionalService> selectedServices = reservation.getAdditionalServices() == null
                ? List.of()
                : reservation.getAdditionalServices().stream()
                        .map(SelectedAdditionalService::from)
                        .toList();

        return new ReservationOnboardingConfirmationResponse(
                reservation.getId(),
                reservation.getConfirmationCode(),
                reservation.getAccommodation().getId(),
                reservation.getAccommodation().getName(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getGuestCount(),
                reservation.getPricePerNight(),
                reservation.getSubtotal(),
                reservation.getAdditionalServicesTotal(),
                reservation.getTotalAmount(),
                reservation.getCurrency(),
                reservation.getStatus(),
                reservation.getPaymentStatus(),
                reservation.getBoatTransferRequested(),
                selectedServices,
                paymentMethods == null ? List.of() : List.copyOf(paymentMethods),
                currencyEquivalents == null ? List.of() : List.copyOf(currencyEquivalents)
        );
    }
}
