package com.hostflow.service;

import com.hostflow.enums.Currency;
import com.hostflow.enums.UsdRateType;
import com.hostflow.model.ExchangeRateCache;
import com.hostflow.repository.ExchangeRateCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Servicio que obtiene tasas de cambio REALES desde APIs externas y las cachea en la base de datos.
 *
 * <p><strong>APIs utilizadas (100% gratuitas, sin API Key):</strong></p>
 * <ul>
 *   <li><strong>DolarAPI.com:</strong> Para el dólar argentino. Obtiene múltiples cotizaciones
 *       (blue, oficial, MEP, cripto, tarjeta, mayorista) en una sola llamada.</li>
 *   <li><strong>open.er-api.com:</strong> Para el Euro y otras monedas. Soporta ARS como moneda base.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.2.0 (Agregado método getConversionRate para soporte multi-moneda en emails)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateProviderService {

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

    private final ExchangeRateCacheRepository repository;
    private final RestTemplate restTemplate;

    @Value("${app.exchange-rate.dolarapi.url:https://dolarapi.com/v1/dolares}")
    private String dolarApiUrl;

    /** API gratuita que soporta ARS como base y devuelve tasas para EUR, USD, etc. */
    @Value("${app.exchange-rate.eras-api.url:https://open.er-api.com/v6/latest/ARS}")
    private String erasApiUrl;

    // =========================================================================
    // TAREAS PROGRAMADAS (SCHEDULING)
    // =========================================================================

    @Scheduled(fixedDelay = 60 * 60 * 1000, initialDelay = 30_000)
    @Transactional
    public void refreshAllRates() {
        log.info("🔄 Iniciando actualización de tasas de cambio desde APIs externas...");

        // 1. USD: obtener todas las cotizaciones desde DolarAPI.com
        try {
            refreshUsdRatesFromDolarApi();
        } catch (Exception e) {
            log.error("❌ Error obteniendo USD de DolarAPI.com: {}", e.getMessage());
        }

        // 2. EUR: obtener desde open.er-api.com (que sí soporta ARS como base)
        try {
            refreshEurRateFromErasApi();
        } catch (Exception e) {
            log.error("❌ Error obteniendo EUR de open.er-api.com: {}", e.getMessage());
        }

        log.info("✅ Proceso de actualización de tasas finalizado.");
    }

    // =========================================================================
    // LÓGICA DE CONVERSIÓN DE MONEDAS (Requerido por EmailService)
    // =========================================================================

    /**
     * Obtiene la tasa de conversión entre dos monedas.
     * Utiliza las tasas cacheadas en la base de datos (todas con base ARS).
     *
     * @param fromCurrency Moneda de origen (ej: "USD", "ARS", "EUR")
     * @param toCurrency   Moneda de destino (ej: "ARS", "USD", "EUR")
     * @return La tasa de conversión (cuántas unidades de toCurrency equivalen a 1 unidad de fromCurrency).
     *         Devuelve BigDecimal.ONE si las monedas son iguales o si hay un error, para no romper el flujo.
     */
    public BigDecimal getConversionRate(String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null) {
            return BigDecimal.ONE;
        }

        String from = fromCurrency.trim().toUpperCase();
        String to = toCurrency.trim().toUpperCase();

        if (from.equals(to)) {
            return BigDecimal.ONE;
        }

        try {
            // Caso 1: ARS a USD (Usamos la tasa Oficial como referencia estándar para estimaciones)
            if (from.equals("ARS") && to.equals("USD")) {
                return repository.findByFromCurrencyAndToCurrencyAndUsdRateType(
                                Currency.ARS, Currency.USD, UsdRateType.OFICIAL)
                        .map(ExchangeRateCache::getRate)
                        .orElse(BigDecimal.ONE);
            }

            // Caso 2: USD a ARS (Invertimos la tasa ARS a USD Oficial)
            if (from.equals("USD") && to.equals("ARS")) {
                BigDecimal arsToUsd = repository.findByFromCurrencyAndToCurrencyAndUsdRateType(
                                Currency.ARS, Currency.USD, UsdRateType.OFICIAL)
                        .map(ExchangeRateCache::getRate)
                        .orElse(BigDecimal.ONE);
                return BigDecimal.ONE.divide(arsToUsd, MC);
            }

            // Caso 3: ARS a EUR
            if (from.equals("ARS") && to.equals("EUR")) {
                return repository.findByFromCurrencyAndToCurrencyAndUsdRateTypeIsNull(Currency.ARS, Currency.EUR)
                        .map(ExchangeRateCache::getRate)
                        .orElse(BigDecimal.ONE);
            }

            // Caso 4: EUR a ARS (Invertimos la tasa ARS a EUR)
            if (from.equals("EUR") && to.equals("ARS")) {
                BigDecimal arsToEur = repository.findByFromCurrencyAndToCurrencyAndUsdRateTypeIsNull(Currency.ARS, Currency.EUR)
                        .map(ExchangeRateCache::getRate)
                        .orElse(BigDecimal.ONE);
                return BigDecimal.ONE.divide(arsToEur, MC);
            }

            // Caso 5: USD a EUR (Tasa cruzada: USD -> ARS -> EUR)
            if (from.equals("USD") && to.equals("EUR")) {
                BigDecimal usdToArs = getConversionRate("USD", "ARS");
                BigDecimal arsToEur = getConversionRate("ARS", "EUR");
                return usdToArs.multiply(arsToEur, MC);
            }

            // Caso 6: EUR a USD (Tasa cruzada: EUR -> ARS -> USD)
            if (from.equals("EUR") && to.equals("USD")) {
                BigDecimal eurToArs = getConversionRate("EUR", "ARS");
                BigDecimal arsToUsd = getConversionRate("ARS", "USD");
                return eurToArs.multiply(arsToUsd, MC);
            }

            // Fallback para monedas no soportadas directamente en el caché
            log.warn("⚠️ No se encontró tasa de conversión para {} a {}. Se devuelve 1.", from, to);
            return BigDecimal.ONE;

        } catch (Exception e) {
            log.error("❌ Error calculando la tasa de conversión de {} a {}: {}", from, to, e.getMessage());
            return BigDecimal.ONE;
        }
    }

    @Transactional
    public void forceRefresh() {
        refreshAllRates();
    }

    // =========================================================================
    // MÉTODOS PRIVADOS DE ACTUALIZACIÓN DE CACHE
    // =========================================================================

    private void refreshUsdRatesFromDolarApi() {
        log.debug("📡 Consultando DolarAPI.com: {}", dolarApiUrl);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> response = restTemplate.getForObject(dolarApiUrl, List.class);

        if (response == null || response.isEmpty()) {
            log.warn("⚠️ DolarAPI.com devolvió una respuesta vacía.");
            return;
        }

        int persistedCount = 0;
        for (Map<String, Object> item : response) {
            String casa = (String) item.get("casa");
            UsdRateType rateType = findUsdRateTypeByCasa(casa);

            if (rateType == null) {
                log.debug("Ignorando cotización desconocida de DolarAPI: {}", casa);
                continue;
            }

            Object venta = item.get("venta");
            BigDecimal arsPerUsd = toBigDecimal(venta);

            if (arsPerUsd == null || arsPerUsd.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("⚠️ Precio de venta inválido para {}: {}", casa, venta);
                continue;
            }

            BigDecimal usdPerArs = BigDecimal.ONE.divide(arsPerUsd, MC);
            persistRate(Currency.ARS, Currency.USD, rateType, usdPerArs);
            persistedCount++;

            log.info("💵 USD ({}) actualizado: 1 USD = {} ARS | 1 ARS = {} USD",
                    rateType.getDisplayName(), arsPerUsd.toPlainString(), usdPerArs.toPlainString());
        }

        log.info("✅ {} cotizaciones de USD persistidas desde DolarAPI.com", persistedCount);
    }

    /**
     * Obtiene la tasa del euro desde open.er-api.com.
     * Esta API devuelve directamente cuántos EUR vale 1 ARS.
     */
    private void refreshEurRateFromErasApi() {
        log.debug("📡 Consultando open.er-api.com: {}", erasApiUrl);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(erasApiUrl, Map.class);

        if (response == null || !"success".equals(response.get("result"))) {
            log.warn("⚠️ open.er-api.com devolvió una respuesta inválida.");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> rates = (Map<String, Object>) response.get("rates");
        if (rates == null || rates.get("EUR") == null) {
            log.warn("⚠️ open.er-api.com no incluyó la tasa EUR.");
            return;
        }

        BigDecimal eurPerArs = toBigDecimal(rates.get("EUR"));
        if (eurPerArs == null || eurPerArs.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("⚠️ Tasa EUR inválida desde open.er-api.com.");
            return;
        }

        persistRate(Currency.ARS, Currency.EUR, null, eurPerArs);
        log.info("💶 EUR actualizado: 1 ARS = {} EUR", eurPerArs.toPlainString());
    }

    private void persistRate(Currency from, Currency to, UsdRateType usdRateType, BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) return;

        ExchangeRateCache entity;
        if (usdRateType != null) {
            entity = repository
                    .findByFromCurrencyAndToCurrencyAndUsdRateType(from, to, usdRateType)
                    .orElse(ExchangeRateCache.builder()
                            .fromCurrency(from)
                            .toCurrency(to)
                            .usdRateType(usdRateType)
                            .build());
        } else {
            entity = repository
                    .findByFromCurrencyAndToCurrencyAndUsdRateTypeIsNull(from, to)
                    .orElse(ExchangeRateCache.builder()
                            .fromCurrency(from)
                            .toCurrency(to)
                            .build());
        }

        entity.setRate(rate);
        entity.setFetchedAt(LocalDateTime.now());
        repository.save(entity);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    private UsdRateType findUsdRateTypeByCasa(String casa) {
        if (casa == null) return null;
        for (UsdRateType type : UsdRateType.values()) {
            if (type.getCasa().equalsIgnoreCase(casa)) {
                return type;
            }
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}