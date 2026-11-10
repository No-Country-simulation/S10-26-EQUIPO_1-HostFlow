package com.hostflow.repository;

import com.hostflow.enums.Currency;
import com.hostflow.enums.UsdRateType;
import com.hostflow.model.ExchangeRateCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la gestión de la entidad {@link ExchangeRateCache}.
 *
 * <p>Este repositorio proporciona operaciones de consulta para acceder a la
 * caché de tasas de cambio obtenidas desde APIs externas (DolarAPI.com para
 * USD y Frankfurter API para EUR). Las tasas se almacenan temporalmente en
 * la base de datos para evitar consultas repetidas a las APIs externas.</p>
 *
 * <p><strong>Características clave:</strong></p>
 * <ul>
 *   <li>Almacena tasas de conversión entre pares de monedas (ARS→USD, ARS→EUR).</li>
 *   <li>Para USD, soporta múltiples tipos de cotización (blue, oficial, MEP, etc.).</li>
 *   <li>Incluye timestamp de la última actualización para detectar tasas obsoletas.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 * @see ExchangeRateCache
 * @see com.hostflow.service.ExchangeRateProviderService
 */
@Repository
public interface ExchangeRateCacheRepository extends JpaRepository<ExchangeRateCache, Long> {

    /**
     * Busca una tasa de cambio por par de monedas (sin tipo de dólar específico).
     *
     * <p>Se utiliza principalmente para monedas que no son USD, como EUR,
     * donde no existe el concepto de "tipo de cotización".</p>
     *
     * <p>Ejemplo de uso: obtener la tasa ARS→EUR desde Frankfurter API.</p>
     *
     * @param fromCurrency moneda origen (generalmente ARS)
     * @param toCurrency   moneda destino (ej. EUR)
     * @return un {@link Optional} con la tasa de cambio si existe, o vacío si no hay datos cacheados
     */
    Optional<ExchangeRateCache> findByFromCurrencyAndToCurrencyAndUsdRateTypeIsNull(
            Currency fromCurrency, Currency toCurrency);

    /**
     * Busca una tasa de cambio específica para USD según el tipo de cotización.
     *
     * <p>DolarAPI.com proporciona múltiples cotizaciones del dólar argentino
     * (blue, oficial, MEP, cripto, tarjeta, mayorista). Este método permite
     * obtener la tasa específica según el tipo elegido por el propietario
     * del alojamiento.</p>
     *
     * <p>Ejemplo de uso: obtener la tasa ARS→USD tipo BLUE para un alojamiento
     * que cobra en dólares blue.</p>
     *
     * @param fromCurrency moneda origen (generalmente ARS)
     * @param toCurrency   moneda destino (USD)
     * @param usdRateType  tipo de cotización del dólar (BLUE, OFICIAL, MEP, etc.)
     * @return un {@link Optional} con la tasa de cambio si existe, o vacío si no hay datos cacheados
     */
    Optional<ExchangeRateCache> findByFromCurrencyAndToCurrencyAndUsdRateType(
            Currency fromCurrency, Currency toCurrency, UsdRateType usdRateType);
}