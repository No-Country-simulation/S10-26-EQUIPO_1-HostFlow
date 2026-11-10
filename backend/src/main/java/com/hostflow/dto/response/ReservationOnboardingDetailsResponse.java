package com.hostflow.dto.response;

import com.hostflow.enums.Currency;
import com.hostflow.enums.PaymentMethodType;
import com.hostflow.enums.ServicePriceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Información pública mínima para mostrar el resumen y completar el onboarding. */
public record ReservationOnboardingDetailsResponse(
        String confirmationCode,
        String accommodationName,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        Integer nights,
        Integer guestCount,
        BigDecimal pricePerNight,
        BigDecimal accommodationSubtotal,
        BigDecimal additionalServicesTotal,
        BigDecimal totalAmount,
        Currency currency,
        String logoUrl,
        String primaryColor,
        String accentColor,
        String tertiaryColor,
        String backgroundPattern,
        String welcomeMessage,
        List<PaymentOption> paymentMethods,
        List<AdditionalServiceOption> additionalServices,
        List<CurrencyEquivalent> currencyEquivalents
) {
    /** Equivalencias informativas; los importes persistidos siguen en la moneda base. */
    public record CurrencyEquivalent(Currency currency, BigDecimal pricePerNight,
                                     BigDecimal accommodationSubtotal,
                                     BigDecimal additionalServicesTotal, BigDecimal totalAmount) {}
    public record PaymentOption(Long id, PaymentMethodType type, String displayName,
                                Currency currency, String instructions, String bankName,
                                String accountHolder, String accountNumber, String cbuCvu,
                                String alias, String providerName, String userIdentifier,
                                String walletPhone, String walletEmail) {}
    public record AdditionalServiceOption(Long id, String name, String description,
                                          BigDecimal basePrice, ServicePriceType priceType,
                                          Currency currency) {}
}
