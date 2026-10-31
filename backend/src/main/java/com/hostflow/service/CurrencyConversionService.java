package com.hostflow.service;


import com.hostflow.enums.Currency;
import com.hostflow.exception.BusinessException;
import com.hostflow.model.AcceptedCurrency;
import com.hostflow.model.ExchangeRateCache;
import com.hostflow.repository.AcceptedCurrencyRepository;
import com.hostflow.repository.ExchangeRateCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Servicio central para conversión de monedas en HostFlow.
 *
 * <p><strong>Estrategia de resolución de tasas:</strong></p>
 * <ol>
 *   <li>Si la moneda destino es igual a la base → retorna el valor tal cual.</li>
 *   <li>Si la moneda tiene {@code useAutomaticRate=true} → usa la tasa cacheada
 *       obtenida desde la API externa (DolarAPI para USD, Frankfurter para EUR).</li>
 *   <li>Si la moneda tiene {@code useAutomaticRate=false} → usa el {@code manualRate}
 *       configurado por el propietario.</li>
 *   <li>Si no hay tasa disponible → lanza {@link BusinessException}.</li>
 * </ol>
 *
 * <p><strong>Para USD:</strong> respeta el tipo de cotización elegido por el
 * propietario (blue, oficial, MEP, etc.) almacenado en {@code AcceptedCurrency.usdRateType}.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyConversionService {

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

    /** Una tasa automática se considera obsoleta después de este número de horas. */
    private static final long RATE_MAX_AGE_HOURS = 4;

    private final AcceptedCurrencyRepository acceptedCurrencyRepository;
    private final ExchangeRateCacheRepository exchangeRateCacheRepository;

    /**
     * Convierte un monto desde la moneda base del alojamiento a la moneda destino.
     *
     * @param accommodationId ID del alojamiento
     * @param amount          monto en la moneda base
     * @param targetCurrency  moneda destino deseada
     * @return el monto convertido y redondeado según la moneda destino
     * @throws BusinessException si la moneda no está configurada, está deshabilitada
     *                           o no hay tasa disponible
     */
    @Transactional(readOnly = true)
    public BigDecimal convert(Long accommodationId, BigDecimal amount, Currency targetCurrency) {
        if (amount == null) return null;
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El monto no puede ser negativo.");
        }

        // Buscar la configuración de la moneda destino para este alojamiento
        AcceptedCurrency cfg = acceptedCurrencyRepository
                .findByAccommodation_IdAndCurrency(accommodationId, targetCurrency)
                .orElseThrow(() -> new BusinessException(
                        "La moneda " + targetCurrency + " no está configurada para este alojamiento."));

        // Validar que la moneda esté habilitada
        if (!cfg.isEnabled()) {
            throw new BusinessException(
                    "La moneda " + targetCurrency + " está deshabilitada para este alojamiento.");
        }

        // Resolver la tasa efectiva (automática o manual)
        BigDecimal rate = resolveRate(cfg, targetCurrency);

        // Aplicar la conversión y redondear al scale de la moneda destino
        return targetCurrency.scale(amount.multiply(rate, MC));
    }

    /**
     * Devuelve la tasa efectiva (automática o manual) para una moneda destino.
     *
     * <p>Útil para mostrar al propietario la tasa que se está aplicando actualmente.</p>
     *
     * @param accommodationId ID del alojamiento
     * @param targetCurrency  moneda destino
     * @return la tasa efectiva, o {@code null} si no se puede resolver
     */
    @Transactional(readOnly = true)
    public BigDecimal resolveEffectiveRate(Long accommodationId, Currency targetCurrency) {
        AcceptedCurrency cfg = acceptedCurrencyRepository
                .findByAccommodation_IdAndCurrency(accommodationId, targetCurrency)
                .orElse(null);
        if (cfg == null) return null;
        try {
            return resolveRate(cfg, targetCurrency);
        } catch (RuntimeException e) {
            log.warn("No se pudo resolver la tasa para {}: {}", targetCurrency, e.getMessage());
            return null;
        }
    }

    /**
     * Resuelve la tasa a aplicar según la configuración de la moneda.
     *
     * <p>Para USD, utiliza el tipo de cotización (blue, oficial, etc.) configurado
     * en {@code AcceptedCurrency.usdRateType}. Para EUR y otras monedas, busca
     * la tasa genérica sin tipo de dólar.</p>
     */
    private BigDecimal resolveRate(AcceptedCurrency cfg, Currency targetCurrency) {
        // Caso 1: Tasa manual configurada por el propietario
        if (!cfg.isUseAutomaticRate()) {
            if (cfg.getManualRate() == null || cfg.getManualRate().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(
                        "La tasa manual para " + targetCurrency + " no es válida. Configure una tasa > 0.");
            }
            return cfg.getManualRate();
        }

        // Caso 2: Tasa automática desde caché (API externa)
        ExchangeRateCache cached;
        if (targetCurrency == Currency.USD && cfg.getUsdRateType() != null) {
            // Para USD: buscar la tasa específica del tipo de dólar elegido
            cached = exchangeRateCacheRepository
                    .findByFromCurrencyAndToCurrencyAndUsdRateType(
                            Currency.ARS, Currency.USD, cfg.getUsdRateType())
                    .orElseThrow(() -> new BusinessException(
                            "No hay tasa de cambio disponible para USD (" + cfg.getUsdRateType().getDisplayName() + ")."));
        } else {
            // Para EUR y otras: buscar la tasa genérica
            cached = exchangeRateCacheRepository
                    .findByFromCurrencyAndToCurrencyAndUsdRateTypeIsNull(Currency.ARS, targetCurrency)
                    .orElseThrow(() -> new BusinessException(
                            "No hay tasa de cambio disponible para " + targetCurrency + ". Actualice las tasas o use tasa manual."));
        }

        // Advertencia si la tasa está desactualizada (pero se usa igual)
        if (cached.getFetchedAt().isBefore(LocalDateTime.now().minusHours(RATE_MAX_AGE_HOURS))) {
            log.warn("⚠️ Tasa de cambio para {} expirada (obtenida el {}). Usando valor cacheado.",
                    targetCurrency, cached.getFetchedAt());
        }

        return cached.getRate();
    }
}