package com.hostflow.service;

import com.hostflow.dto.response.AcceptedCurrencyResponse;
import com.hostflow.enums.Currency;
import com.hostflow.enums.UsdRateType;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.AcceptedCurrency;
import com.hostflow.model.Accommodation;
import com.hostflow.model.Owner;
import com.hostflow.repository.AcceptedCurrencyRepository;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio para gestionar las monedas habilitadas de un alojamiento.
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AcceptedCurrencyService {

    private final AcceptedCurrencyRepository repository;
    private final AccommodationRepository accommodationRepository;
    private final OwnerRepository ownerRepository;
    private final CurrencyConversionService conversionService;

    // =========================================================================
    // 🆕 MÉTODOS QUE ACEPTAN EMAIL (Los que necesita el controlador)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AcceptedCurrencyResponse> listByEmail(String ownerEmail) {
        Accommodation acc = getAccommodationByEmail(ownerEmail);
        return repository.findAllByAccommodation_IdOrderByCurrencyAsc(acc.getId()).stream()
                .map(c -> {
                    BigDecimal effective = getSafeRate(acc.getId(), c.getCurrency());
                    return AcceptedCurrencyResponse.from(c, effective);
                })
                .toList();
    }

    @Transactional
    public AcceptedCurrencyResponse upsertByEmail(String ownerEmail, Currency currency, boolean enabled,
                                                  boolean useAutomaticRate, BigDecimal manualRate,
                                                  UsdRateType usdRateType) {
        Accommodation acc = getAccommodationByEmail(ownerEmail);

        if (currency == acc.getBaseCurrency()) {
            enabled = true;
            useAutomaticRate = false;
            manualRate = BigDecimal.ONE;
        }

        if (!useAutomaticRate && currency != acc.getBaseCurrency()) {
            if (manualRate == null || manualRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Debe indicar una tasa manual válida mayor a 0.");
            }
        }

        AcceptedCurrency entity = repository
                .findByAccommodation_IdAndCurrency(acc.getId(), currency)
                .orElse(AcceptedCurrency.builder()
                        .accommodation(acc)
                        .currency(currency)
                        .build());

        entity.setEnabled(enabled);
        entity.setUseAutomaticRate(useAutomaticRate);

        if (useAutomaticRate) {
            entity.setManualRate(null);
            if (currency == Currency.USD) {
                entity.setUsdRateType(usdRateType != null ? usdRateType : UsdRateType.BLUE);
            } else {
                entity.setUsdRateType(null);
            }
        } else {
            entity.setManualRate(manualRate);
            entity.setUsdRateType(null);
        }

        AcceptedCurrency saved = repository.save(entity);
        log.info("💱 Moneda {} {} para alojamiento {} (Email Owner: {})",
                currency, enabled ? "habilitada" : "deshabilitada", acc.getId(), ownerEmail);

        BigDecimal effective = getSafeRate(acc.getId(), saved.getCurrency());
        return AcceptedCurrencyResponse.from(saved, effective);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private Accommodation getAccommodationByEmail(String ownerEmail) {
        Owner owner = ownerRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el propietario con email: " + ownerEmail));

        return accommodationRepository.findByOwner_Id(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registra primero los datos generales del alojamiento."));
    }

    private BigDecimal getSafeRate(Long accommodationId, Currency currency) {
        try {
            return conversionService.resolveEffectiveRate(accommodationId, currency);
        } catch (RuntimeException e) {
            log.warn("No se pudo resolver la tasa para la moneda {}: {}", currency, e.getMessage());
            return null;
        }
    }
}