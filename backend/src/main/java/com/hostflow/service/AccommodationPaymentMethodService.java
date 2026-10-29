package com.hostflow.service;

import com.hostflow.dto.request.AccommodationPaymentMethodRequest;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.AccommodationPaymentMethod;
import com.hostflow.enums.PaymentMethodType;
import com.hostflow.repository.AccommodationPaymentMethodRepository;
import com.hostflow.repository.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccommodationPaymentMethodService {
    private final AccommodationRepository accommodationRepository;
    private final AccommodationPaymentMethodRepository paymentMethodRepository;

    @Transactional(readOnly = true)
    public List<AccommodationPaymentMethod> list(Long ownerId) {
        Accommodation accommodation = getAccommodation(ownerId);
        return paymentMethodRepository.findAllByAccommodation_IdOrderByDefaultMethodDescCreatedAtAscIdAsc(accommodation.getId());
    }

    @Transactional
    public AccommodationPaymentMethod create(Long ownerId, AccommodationPaymentMethodRequest request) {
        if (request.getPaymentType() == null || request.getDisplayName() == null || request.getDisplayName().isBlank()) {
            throw new BusinessException("paymentType y displayName son obligatorios.");
        }
        Accommodation accommodation = getAccommodation(ownerId);
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault())
                || !paymentMethodRepository.existsByAccommodation_IdAndDefaultMethodTrue(accommodation.getId());
        if (makeDefault) paymentMethodRepository.clearDefaultForAccommodation(accommodation.getId());

        AccommodationPaymentMethod method = new AccommodationPaymentMethod();
        method.setAccommodation(accommodation);
        method.setPaymentType(request.getPaymentType());
        method.setDisplayName(request.getDisplayName().trim());
        applyOptionalFields(method, request);
        method.setDefault(makeDefault);
        method.setActive(request.getIsActive() == null || request.getIsActive());
        validateRequiredFieldsByType(method);
        return paymentMethodRepository.save(method);
    }

    @Transactional
    public AccommodationPaymentMethod update(Long ownerId, Long methodId, AccommodationPaymentMethodRequest request) {
        if (isEmptyPatch(request)) throw new BusinessException("Debe enviar al menos un campo para actualizar.");
        Accommodation accommodation = getAccommodation(ownerId);
        AccommodationPaymentMethod method = paymentMethodRepository.findByIdAndAccommodation_Id(methodId, accommodation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ese método de pago en tu alojamiento."));
        if (request.getDisplayName() != null && request.getDisplayName().isBlank()) {
            throw new BusinessException("displayName no puede estar vacío.");
        }
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            paymentMethodRepository.clearDefaultForAccommodation(accommodation.getId());
            method.setDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault())) {
            method.setDefault(false);
        }
        if (request.getPaymentType() != null) method.setPaymentType(request.getPaymentType());
        if (request.getDisplayName() != null) method.setDisplayName(request.getDisplayName().trim());
        if (request.getProviderName() != null) method.setProviderName(blankToNull(request.getProviderName()));
        if (request.getBankName() != null) method.setBankName(blankToNull(request.getBankName()));
        if (request.getAccountHolder() != null) method.setAccountHolder(blankToNull(request.getAccountHolder()));
        if (request.getAccountType() != null) method.setAccountType(blankToNull(request.getAccountType()));
        if (request.getAccountNumber() != null) method.setAccountNumber(blankToNull(request.getAccountNumber()));
        if (request.getCbuCvu() != null) method.setCbuCvu(blankToNull(request.getCbuCvu()));
        if (request.getAlias() != null) method.setAlias(blankToNull(request.getAlias()));
        if (request.getTaxId() != null) method.setTaxId(blankToNull(request.getTaxId()));
        if (request.getUserIdentifier() != null) method.setUserIdentifier(blankToNull(request.getUserIdentifier()));
        if (request.getWalletPhone() != null) method.setWalletPhone(blankToNull(request.getWalletPhone()));
        if (request.getWalletEmail() != null) method.setWalletEmail(blankToNull(request.getWalletEmail()));
        if (request.getInstructions() != null) method.setInstructions(blankToNull(request.getInstructions()));
        if (request.getIsActive() != null) method.setActive(request.getIsActive());
        validateRequiredFieldsByType(method);
        return paymentMethodRepository.save(method);
    }

    @Transactional
    public void delete(Long ownerId, Long methodId) {
        Accommodation accommodation = getAccommodation(ownerId);
        AccommodationPaymentMethod method = paymentMethodRepository.findByIdAndAccommodation_Id(methodId, accommodation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ese método de pago en tu alojamiento."));
        boolean wasDefault = method.isDefault();
        paymentMethodRepository.delete(method);
        if (wasDefault) {
            List<AccommodationPaymentMethod> remaining = paymentMethodRepository
                    .findAllByAccommodation_IdOrderByDefaultMethodDescCreatedAtAscIdAsc(accommodation.getId());
            if (!remaining.isEmpty()) {
                AccommodationPaymentMethod nextDefault = remaining.get(0);
                nextDefault.setDefault(true);
                paymentMethodRepository.save(nextDefault);
            }
        }
    }

    private Accommodation getAccommodation(Long ownerId) {
        return accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Registra primero los datos generales del alojamiento."));
    }

    private boolean isEmptyPatch(AccommodationPaymentMethodRequest r) {
        return r.getPaymentType() == null && r.getDisplayName() == null && r.getProviderName() == null
                && r.getBankName() == null && r.getAccountHolder() == null && r.getAccountType() == null
                && r.getAccountNumber() == null && r.getCbuCvu() == null && r.getAlias() == null && r.getTaxId() == null
                && r.getUserIdentifier() == null && r.getWalletPhone() == null && r.getWalletEmail() == null
                && r.getInstructions() == null && r.getIsDefault() == null && r.getIsActive() == null;
    }

    private void applyOptionalFields(AccommodationPaymentMethod m, AccommodationPaymentMethodRequest r) {
        m.setProviderName(blankToNull(r.getProviderName()));
        m.setBankName(blankToNull(r.getBankName()));
        m.setAccountHolder(blankToNull(r.getAccountHolder()));
        m.setAccountType(blankToNull(r.getAccountType()));
        m.setAccountNumber(blankToNull(r.getAccountNumber()));
        m.setCbuCvu(blankToNull(r.getCbuCvu()));
        m.setAlias(blankToNull(r.getAlias()));
        m.setTaxId(blankToNull(r.getTaxId()));
        m.setUserIdentifier(blankToNull(r.getUserIdentifier()));
        m.setWalletPhone(blankToNull(r.getWalletPhone()));
        m.setWalletEmail(blankToNull(r.getWalletEmail()));
        m.setInstructions(blankToNull(r.getInstructions()));
    }

    /**
     * Valida los datos obligatorios según el tipo del método de pago.
     * Se ejecuta después de aplicar los campos para que PATCH valide el estado final.
     */
    private void validateRequiredFieldsByType(AccommodationPaymentMethod method) {
        if (method.getPaymentType() == PaymentMethodType.BANK_TRANSFER) {
            requireField(method.getBankName(), "bankName", "BANK_TRANSFER");
            requireField(method.getAccountHolder(), "accountHolder", "BANK_TRANSFER");
            requireField(method.getAccountType(), "accountType", "BANK_TRANSFER");
            requireField(method.getAccountNumber(), "accountNumber", "BANK_TRANSFER");
            requireField(method.getCbuCvu(), "cbuCvu", "BANK_TRANSFER");
            requireField(method.getAlias(), "alias", "BANK_TRANSFER");
        } else if (method.getPaymentType() == PaymentMethodType.DIGITAL_WALLET) {
            requireField(method.getAccountHolder(), "accountHolder", "DIGITAL_WALLET");
            requireField(method.getUserIdentifier(), "userIdentifier", "DIGITAL_WALLET");
            requireField(method.getWalletPhone(), "walletPhone", "DIGITAL_WALLET");
            requireField(method.getWalletEmail(), "walletEmail", "DIGITAL_WALLET");
            if (!isValidEmail(method.getWalletEmail())) {
                throw new BusinessException("walletEmail debe ser un correo electrónico válido para DIGITAL_WALLET.");
            }
        }
    }

    private void requireField(String value, String fieldName, String paymentType) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(fieldName + " es obligatorio para " + paymentType + ".");
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
