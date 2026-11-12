
package com.hostflow.service;

import com.hostflow.enums.Currency;
import com.hostflow.enums.UsdRateType;
import com.hostflow.exception.BusinessException;
import com.hostflow.model.AcceptedCurrency;
import com.hostflow.model.ExchangeRateCache;
import com.hostflow.repository.AcceptedCurrencyRepository;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.ExchangeRateCacheRepository;
import com.hostflow.repository.OwnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrencyConversionServiceTest {

    private final AcceptedCurrencyRepository acceptedCurrencyRepository =
            mock(AcceptedCurrencyRepository.class);

    private final ExchangeRateCacheRepository exchangeRateCacheRepository =
            mock(ExchangeRateCacheRepository.class);

    private final OwnerRepository ownerRepository =
            mock(OwnerRepository.class);

    private final AccommodationRepository accommodationRepository =
            mock(AccommodationRepository.class);

    private CurrencyConversionService service;

    @BeforeEach
    void setUp() {
        service = new CurrencyConversionService(
                acceptedCurrencyRepository,
                exchangeRateCacheRepository,
                ownerRepository,
                accommodationRepository
        );
    }

    @Test
    void convertAcceptsPositiveCachedUsdRate() {
        configureAutomaticCurrency(Currency.USD, UsdRateType.BLUE);
        configureCachedRate(Currency.USD, UsdRateType.BLUE,
                new BigDecimal("0.001"));

        BigDecimal result = service.convert(
                10L, new BigDecimal("1000"), Currency.USD);

        assertEquals(new BigDecimal("1.00"), result);
    }

    @Test
    void convertRejectsZeroCachedUsdRate() {
        configureAutomaticCurrency(Currency.USD, UsdRateType.BLUE);
        configureCachedRate(Currency.USD, UsdRateType.BLUE,
                BigDecimal.ZERO);

        assertThrows(BusinessException.class, () ->
                service.convert(10L, new BigDecimal("1000"), Currency.USD));
    }

    @Test
    void convertRejectsNegativeCachedEurRate() {
        configureAutomaticCurrency(Currency.EUR, null);
        configureCachedRate(Currency.EUR, null,
                new BigDecimal("-0.001"));

        assertThrows(BusinessException.class, () ->
                service.convert(10L, new BigDecimal("1000"), Currency.EUR));
    }

    @Test
    void convertRejectsNullCachedRate() {
        configureAutomaticCurrency(Currency.EUR, null);
        configureCachedRate(Currency.EUR, null, null);

        assertThrows(BusinessException.class, () ->
                service.convert(10L, new BigDecimal("1000"), Currency.EUR));
    }

    private void configureAutomaticCurrency(
            Currency currency, UsdRateType rateType) {

        AcceptedCurrency config = new AcceptedCurrency();
        config.setEnabled(true);
        config.setUseAutomaticRate(true);
        config.setCurrency(currency);
        config.setUsdRateType(rateType);

        when(acceptedCurrencyRepository
                .findByAccommodation_IdAndCurrency(10L, currency))
                .thenReturn(Optional.of(config));
    }

    private void configureCachedRate(
            Currency currency, UsdRateType rateType, BigDecimal rate) {

        ExchangeRateCache cached = new ExchangeRateCache();
        cached.setFromCurrency(Currency.ARS);
        cached.setToCurrency(currency);
        cached.setUsdRateType(rateType);
        cached.setRate(rate);
        cached.setFetchedAt(LocalDateTime.now());

        if (currency == Currency.USD) {
            when(exchangeRateCacheRepository
                    .findByFromCurrencyAndToCurrencyAndUsdRateType(
                            Currency.ARS, Currency.USD, rateType))
                    .thenReturn(Optional.of(cached));
        } else {
            when(exchangeRateCacheRepository
                    .findByFromCurrencyAndToCurrencyAndUsdRateTypeIsNull(
                            Currency.ARS, currency))
                    .thenReturn(Optional.of(cached));
        }
    }
}
