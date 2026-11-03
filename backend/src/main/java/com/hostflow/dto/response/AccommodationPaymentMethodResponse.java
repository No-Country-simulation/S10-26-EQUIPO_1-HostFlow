package com.hostflow.dto.response;

import com.hostflow.enums.PaymentMethodType;
import com.hostflow.model.AccommodationPaymentMethod;
import java.time.LocalDateTime;

public record AccommodationPaymentMethodResponse(
        Long id, Long accommodationId, PaymentMethodType paymentType, String displayName,
        String providerName, String bankName, String accountHolder, String accountType,
        String accountNumber, String cbuCvu, String alias, String taxId,
        String userIdentifier, String walletPhone, String walletEmail, String instructions,
        boolean isDefault, boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static AccommodationPaymentMethodResponse from(AccommodationPaymentMethod p) {
        return new AccommodationPaymentMethodResponse(
                p.getId(), p.getAccommodation().getId(), p.getPaymentType(), p.getDisplayName(),
                p.getProviderName(), p.getBankName(), p.getAccountHolder(), p.getAccountType(),
                p.getAccountNumber(), p.getCbuCvu(), p.getAlias(), p.getTaxId(),
                p.getUserIdentifier(), p.getWalletPhone(), p.getWalletEmail(), p.getInstructions(),
                p.isDefault(), p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
