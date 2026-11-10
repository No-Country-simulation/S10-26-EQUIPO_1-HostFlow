package com.hostflow.service;

import com.hostflow.dto.response.AdditionalServiceResponse;
import com.hostflow.dto.response.ConvertedPriceResponse;
import com.hostflow.enums.Currency;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.AdditionalService;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.AdditionalServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio dedicado a la consulta pública de precios del alojamiento.
 *
 * <p>Este servicio encapsula toda la lógica de negocio relacionada con la obtención
 * de precios del alojamiento y sus servicios adicionales en múltiples monedas,
 * incluyendo la conversión automática a monedas habilitadas (ARS, USD, EUR).</p>
 *
 * <p><strong>Responsabilidades:</strong></p>
 * <ul>
 *   <li>Obtener el precio base del alojamiento en todas las monedas habilitadas.</li>
 *   <li>Obtener los servicios adicionales activos con sus precios convertidos.</li>
 *   <li>Manejar la conversión de monedas de forma tolerante a fallos (si una API cae,
 *       se omite esa moneda en lugar de fallar toda la operación).</li>
 *   <li>Mapear entidades JPA a DTOs de respuesta dentro de la transacción, evitando
 *       {@code LazyInitializationException} al serializar a JSON.</li>
 * </ul>
 *
 * <p><strong>Seguridad:</strong> Todos los métodos son de solo lectura ({@code readOnly = true})
 * y están diseñados para ser consumidos por endpoints públicos sin autenticación.</p>
 *
 * <p><strong>🆕 Mejora de arquitectura (v2.0):</strong></p>
 * <ul>
 *   <li>Se extrajo toda la lógica de negocio del controlador {@code PublicPricingController}
 *       a este servicio, respetando el principio de separación de capas
 *       (Controller → Service → Repository).</li>
 *   <li>Se consolidó la lógica de conversión de monedas en un método privado
 *       {@link #buildConvertedPrices}, eliminando código duplicado (principio DRY).</li>
 *   <li>La anotación {@code @Transactional} ahora está en el servicio, no en el controlador.</li>
 *   <li>Se previene {@code LazyInitializationException} pasando {@code baseCurrency}
 *       explícitamente al DTO, evitando accesos LAZY fuera de la transacción.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 * @see com.hostflow.controller.PublicPricingController
 * @see CurrencyConversionService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PublicPricingService {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS (Inmutables gracias a @RequiredArgsConstructor)
    // =========================================================================

    /**
     * Repositorio para consultar los alojamientos registrados en la plataforma.
     *
     * <p>Se utiliza para cargar el alojamiento base (con su moneda, precio y datos
     * de branding) antes de realizar las conversiones de moneda.</p>
     */
    private final AccommodationRepository accommodationRepository;

    /**
     * Repositorio para consultar los servicios adicionales de un alojamiento.
     *
     * <p>Los métodos de este repositorio utilizan {@code @EntityGraph} para cargar
     * las imágenes asociadas en una sola consulta SQL, evitando el problema N+1
     * de Hibernate.</p>
     */
    private final AdditionalServiceRepository serviceRepository;

    /**
     * Servicio encargado de convertir montos entre monedas usando tasas
     * automáticas (APIs externas cacheadas) o manuales configuradas por el propietario.
     *
     * <p>Este servicio resuelve la tasa efectiva según la configuración del alojamiento
     * (tasa automática vs manual) y aplica el redondeo adecuado según la moneda destino.</p>
     */
    private final CurrencyConversionService conversionService;

    // =========================================================================
    // OPERACIONES PÚBLICAS
    // =========================================================================

    /**
     * Obtiene el precio base del alojamiento convertido a todas las monedas habilitadas.
     *
     * <p>El precio en la moneda base (generalmente ARS) siempre se incluye. Las demás
     * monedas (USD, EUR) se agregan solo si están habilitadas y existe una tasa válida.
     * Si una moneda no está configurada, está deshabilitada o la API de tasas está caída,
     * se omite de la respuesta para no mostrar precios incorrectos.</p>
     *
     * <p><strong>Manejo tolerante a fallos:</strong> Este método nunca falla por una
     * moneda no disponible. En el peor caso, retorna únicamente el precio en la moneda
     * base, garantizando que el frontend siempre tenga al menos un precio válido.</p>
     *
     * @param accommodationId el identificador único del alojamiento
     * @return lista de precios convertidos en todas las monedas habilitadas
     *         (mínimo uno: la moneda base)
     * @throws ResourceNotFoundException si no se encuentra el alojamiento con el ID dado
     */
    @Transactional(readOnly = true)
    public List<ConvertedPriceResponse> getBasePrice(Long accommodationId) {
        log.debug("💰 Obteniendo precio base para alojamiento ID: {}", accommodationId);

        // 1. Cargar el alojamiento (falla rápido si no existe)
        Accommodation acc = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el alojamiento con ID: " + accommodationId));

        // 2. Construir la lista de precios convertidos (moneda base + habilitadas)
        // Se reutiliza el método consolidado buildConvertedPrices para evitar duplicación
        List<ConvertedPriceResponse> result = buildConvertedPrices(
                acc.getId(),
                acc.getBaseCurrency(),
                acc.getBasePrice()
        );

        log.info("✅ Precio base obtenido para alojamiento {}: {} monedas disponibles",
                accommodationId, result.size());
        return result;
    }

    /**
     * Obtiene los servicios adicionales activos del alojamiento con sus precios convertidos.
     *
     * <p>Solo se muestran los servicios marcados como activos por el propietario.
     * Cada servicio incluye sus imágenes asociadas (cargadas vía {@code @EntityGraph}
     * en el repositorio para evitar el problema N+1 de Hibernate).</p>
     *
     * <p><strong>Optimización de rendimiento:</strong> Gracias a la anotación
     * {@code @EntityGraph(attributePaths = {"images"})} en el método del repositorio,
     * las imágenes se cargan en la misma consulta SQL que los servicios, evitando
     * consultas adicionales por cada servicio.</p>
     *
     * @param accommodationId el identificador único del alojamiento
     * @return lista de servicios adicionales con precios convertidos e imágenes
     * @throws ResourceNotFoundException si no se encuentra el alojamiento con el ID dado
     */
    @Transactional(readOnly = true)
    public List<AdditionalServiceResponse> getAdditionalServices(Long accommodationId) {
        log.debug("🧾 Obteniendo servicios adicionales para alojamiento ID: {}", accommodationId);

        // 1. Cargar el alojamiento (falla rápido si no existe)
        Accommodation acc = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el alojamiento con ID: " + accommodationId));

        // 2. Obtener solo los servicios activos (ya incluye las imágenes vía @EntityGraph)
        // El repositorio usa @EntityGraph para cargar las imágenes en una sola consulta SQL
        List<AdditionalService> services = serviceRepository
                .findAllByAccommodation_IdAndActiveTrueOrderByCreatedAtAsc(acc.getId());

        // 3. Mapear cada servicio a su DTO con precios convertidos
        // Se usa stream + map para transformar cada entidad en su DTO correspondiente
        List<AdditionalServiceResponse> responses = services.stream()
                .map(service -> toServiceResponse(acc, service))
                .toList();

        log.info("✅ Servicios adicionales obtenidos para alojamiento {}: {} servicios activos",
                accommodationId, responses.size());
        return responses;
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Construye la lista de precios convertidos para un monto dado, incluyendo
     * la moneda base y todas las monedas habilitadas con tasa disponible.
     *
     * <p><strong>🆕 Consolidación de código (v2.0):</strong> Este método unifica
     * la lógica de conversión que antes estaba duplicada en {@code getBasePrice()}
     * y en el antiguo método {@code toResponse()} del controlador. Ahora ambos
     * flujos reutilizan esta implementación única, cumpliendo con el principio DRY
     * (Don't Repeat Yourself).</p>
     *
     * <p><strong>Manejo tolerante a fallos:</strong> Si la conversión de una moneda
     * falla (API caída, moneda deshabilitada, tasa no disponible), simplemente se
     * omite esa moneda en lugar de lanzar una excepción que cause rollback de la
     * transacción completa. Esto garantiza que el endpoint siempre responda con
     * al menos el precio en la moneda base.</p>
     *
     * <p><strong>Algoritmo:</strong></p>
     * <ol>
     *   <li>Agrega el precio en la moneda base (siempre incluido, ej: ARS).</li>
     *   <li>Itera sobre todas las monedas del sistema ({@link Currency#values()}).</li>
     *   <li>Para cada moneda distinta a la base, intenta convertirla.</li>
     *   <li>Si la conversión es exitosa, agrega el precio convertido.</li>
     *   <li>Si falla, registra un log de debug y continúa con la siguiente moneda.</li>
     * </ol>
     *
     * @param accommodationId ID del alojamiento (necesario para resolver la configuración de monedas)
     * @param baseCurrency    moneda base del alojamiento (siempre incluida en la respuesta)
     * @param amount          monto a convertir (en la moneda base)
     * @return lista de precios convertidos (al menos uno: la moneda base)
     */
    private List<ConvertedPriceResponse> buildConvertedPrices(
            Long accommodationId,
            Currency baseCurrency,
            BigDecimal amount) {

        List<ConvertedPriceResponse> result = new ArrayList<>();

        // 1. Precio en la moneda base (siempre incluido, ej: ARS)
        // Se valida que el monto no sea null para evitar NullPointerException
        if (amount != null) {
            result.add(ConvertedPriceResponse.of(baseCurrency, amount));
        }

        // 2. Precios en las demás monedas habilitadas (USD, EUR)
        for (Currency targetCurrency : Currency.values()) {
            // Skip de la moneda base (ya fue agregada arriba)
            if (targetCurrency == baseCurrency) {
                continue;
            }
            try {
                // Intentar convertir el monto a la moneda destino
                BigDecimal converted = conversionService.convert(
                        accommodationId, amount, targetCurrency);
                if (converted != null) {
                    result.add(ConvertedPriceResponse.of(targetCurrency, converted));
                }
            } catch (RuntimeException e) {
                // Moneda no habilitada, sin tasa disponible o API caída:
                // se omite silenciosamente para no romper la respuesta completa
                log.debug("Moneda {} omitida para alojamiento {}: {}",
                        targetCurrency, accommodationId, e.getMessage());
            }
        }

        return result;
    }

    /**
     * Construye el DTO de respuesta para un servicio adicional, incluyendo
     * los precios convertidos a todas las monedas habilitadas y sus imágenes.
     *
     * <p><strong>🆕 Corrección definitiva de LazyInitializationException (v2.0):</strong></p>
     * <p>Se pasa {@code acc.getBaseCurrency()} explícitamente como segundo argumento al método
     * {@code from} del DTO. Esto evita que el DTO intente acceder a
     * {@code service.getAccommodation().getBaseCurrency()} fuera de la transacción de Hibernate,
     * lo cual era la causa raíz del error al serializar la respuesta a JSON en versiones anteriores.</p>
     *
     * <p><strong>Reutilización de código:</strong> Este método delega la construcción de la
     * lista de precios convertidos al método {@link #buildConvertedPrices}, evitando duplicar
     * la lógica de iteración sobre monedas y conversión.</p>
     *
     * @param acc     el alojamiento al que pertenece el servicio (ya cargado en memoria)
     * @param service el servicio adicional a mapear (con imágenes ya cargadas vía @EntityGraph)
     * @return el DTO completo con precios convertidos e imágenes
     */
    private AdditionalServiceResponse toServiceResponse(Accommodation acc, AdditionalService service) {
        // Reutilizamos el método consolidado para obtener los precios convertidos
        // Esto garantiza consistencia con el cálculo del precio base
        List<ConvertedPriceResponse> converted = buildConvertedPrices(
                acc.getId(),
                acc.getBaseCurrency(),
                service.getBasePrice()
        );

        // ✅ PASAMOS acc.getBaseCurrency() explícitamente como segundo argumento
        // para evitar acceso LAZY fuera de la transacción (previene LazyInitializationException)
        return AdditionalServiceResponse.from(service, acc.getBaseCurrency(), converted);
    }
}