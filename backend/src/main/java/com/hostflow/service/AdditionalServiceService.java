package com.hostflow.service;

import com.hostflow.dto.request.AdditionalServiceRequest;
import com.hostflow.dto.response.AdditionalServiceResponse;
import com.hostflow.dto.response.ConvertedPriceResponse;
import com.hostflow.enums.Currency;
import com.hostflow.enums.ServicePriceType;
import com.hostflow.exception.BusinessException;
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
 * Servicio para gestionar los servicios adicionales de un alojamiento.
 *
 * <p> Versión 2.1: Corrección de UnexpectedRollbackException al separar
 * la transacción de persistencia de la lógica de conversión de moneda.</p>
 *
 * <p>Los servicios adicionales se almacenan con precio en la moneda base (ARS).
 * La conversión a otras monedas se realiza en tiempo real al consultarlos
 * mediante {@link CurrencyConversionService}.</p>
 *
 * <p><strong>Responsabilidades:</strong></p>
 * <ul>
 *   <li>CRUD de servicios adicionales (crear, listar, actualizar, eliminar).</li>
 *   <li>Validación de reglas de negocio (nombre obligatorio, precio > 0).</li>
 *   <li>Construcción de DTOs de respuesta con precios convertidos a todas
 *       las monedas habilitadas y lista de imágenes asociadas.</li>
 *   <li>Manejo tolerante a fallos en la conversión de monedas para evitar
 *       rollbacks de transacciones completas.</li>
 * </ul>
 *
 * <p><strong>Patrón de Diseño:</strong> Este servicio implementa el patrón
 * Service Layer, separando la lógica de negocio de los controladores y
 * coordinando las operaciones de repositorio y conversión de datos.</p>
 *
 * @author HostFlow Team
 * @version 2.1.0
 * @see AdditionalServiceRepository
 * @see CurrencyConversionService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdditionalServiceService {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS (Inmutables gracias a @RequiredArgsConstructor)
    // =========================================================================

    /**
     * Repositorio para operaciones CRUD de servicios adicionales.
     * Proporciona métodos personalizados con @EntityGraph para cargar
     * las imágenes asociadas de manera eficiente (evitando N+1 queries).
     */
    private final AdditionalServiceRepository serviceRepository;

    /**
     * Repositorio para acceder a los datos del alojamiento del propietario.
     * Se utiliza para validar que el propietario tenga un alojamiento registrado
     * antes de permitir operaciones sobre servicios adicionales.
     */
    private final AccommodationRepository accommodationRepository;

    /**
     * Servicio de conversión de monedas.
     * Se utiliza para convertir el precio base (ARS) a todas las monedas
     * habilitadas por el propietario (USD, EUR) en tiempo real.
     */
    private final CurrencyConversionService conversionService;

    // =========================================================================
    // OPERACIONES DE LECTURA (READ)
    // =========================================================================

    /**
     * Lista todos los servicios adicionales con precios convertidos a todas
     * las monedas habilitadas e imágenes asociadas.
     *
     * <p>Este método es de solo lectura ({@code readOnly = true}) para optimizar
     * el rendimiento y evitar bloqueos innecesarios en la base de datos.</p>
     *
     * <p>Flujo de ejecución:</p>
     * <ol>
     *   <li>Obtiene el alojamiento del propietario.</li>
     *   <li>Consulta todos los servicios ordenados por fecha de creación.</li>
     *   <li>Para cada servicio, construye un DTO con:
     *       <ul>
     *         <li>Datos básicos del servicio</li>
     *         <li>Precios convertidos a ARS, USD y EUR</li>
     *         <li>Lista de imágenes (cargadas vía @EntityGraph)</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * @param ownerId el identificador único del propietario
     * @return lista de servicios con precios convertidos e imágenes
     * @throws ResourceNotFoundException si el propietario no tiene alojamiento registrado
     */
    @Transactional(readOnly = true)
    public List<AdditionalServiceResponse> listWithConvertedPrices(Long ownerId) {
        log.debug("📋 Listando servicios adicionales para owner ID: {}", ownerId);

        Accommodation acc = getAccommodation(ownerId);

        List<AdditionalServiceResponse> response = serviceRepository
                .findAllByAccommodation_IdOrderByCreatedAtAsc(acc.getId())
                .stream()
                .map(s -> toResponse(acc, s))
                .toList();

        log.info("✅ Se listaron {} servicios adicionales para alojamiento ID: {}",
                response.size(), acc.getId());

        return response;
    }

    // =========================================================================
    // OPERACIONES DE ESCRITURA (CREATE/UPDATE/DELETE)
    // =========================================================================

    /**
     * Crea un nuevo servicio adicional y retorna el DTO con precios convertidos.
     *
     * <p><strong>🆕 Importante:</strong> Este método NO es @Transactional.
     * Delega la creación en el método create() (que SÍ lo es).
     * Esto permite que la conversión de moneda se ejecute FUERA de la transacción,
     * evitando el UnexpectedRollbackException si la conversión falla y es atrapada.</p>
     *
     * <p>Flujo de ejecución:</p>
     * <ol>
     *   <li>Valida que el propietario tenga un alojamiento registrado.</li>
     *   <li>Valida los datos de entrada (nombre no vacío, precio > 0).</li>
     *   <li>Determina el tipo de precio (PER_DAY o TOTAL, default: TOTAL).</li>
     *   <li>Crea y persiste la entidad AdditionalService (en transacción separada).</li>
     *   <li>Construye el DTO de respuesta con precios convertidos (fuera de transacción).</li>
     * </ol>
     *
     * @param ownerId el identificador único del propietario
     * @param request los datos del servicio a crear
     * @return el servicio creado con precios convertidos e imágenes (vacía al inicio)
     * @throws BusinessException si los datos de entrada son inválidos
     * @throws ResourceNotFoundException si el propietario no tiene alojamiento registrado
     */
    //  NO es @Transactional - permite que toResponse se ejecute fuera de la transacción
    public AdditionalServiceResponse createWithConvertedPrices(Long ownerId, AdditionalServiceRequest request) {
        log.info(" Creando nuevo servicio adicional para owner ID: {}", ownerId);

        // 1. La creación se ejecuta en su propia transacción y hace commit
        AdditionalService saved = create(ownerId, request);

        // 2. La conversión se ejecuta fuera de la transacción. Si falla, no hace rollback de la creación.
        Accommodation acc = getAccommodation(ownerId);
        AdditionalServiceResponse response = toResponse(acc, saved);

        log.info("✅ Servicio adicional creado exitosamente: ID={}, Name='{}', PriceType={}",
                saved.getId(), saved.getName(), saved.getPriceType());

        return response;
    }

    /**
     * Actualiza un servicio adicional y retorna el DTO con precios convertidos.
     *
     * <p><strong> Importante:</strong> Este método NO es @Transactional.
     * Delega la actualización en el método update() (que SÍ lo es).
     * Esto permite que la conversión de moneda se ejecute FUERA de la transacción,
     * evitando el UnexpectedRollbackException si la conversión falla y es atrapada.</p>
     *
     * <p>Este método realiza una actualización parcial (PATCH): solo modifica
     * los campos que vienen con valor no nulo en el {@code request}.</p>
     *
     * <p>Flujo de ejecución:</p>
     * <ol>
     *   <li>Busca el servicio por ID, validando que pertenezca al alojamiento del propietario.</li>
     *   <li>Actualiza solo los campos no nulos del request (en transacción separada).</li>
     *   <li>Construye el DTO actualizado con precios convertidos (fuera de transacción).</li>
     * </ol>
     *
     * @param ownerId   el identificador único del propietario
     *   <li>Valida que el nombre no esté vacío si se va a actualizar.</li>
     *   <li>Valida que el precio sea > 0 si se va a actualizar.</li>
     *   <li>Persiste los cambios.</li>
     *   <li>Retorna el DTO actualizado con precios convertidos.</li>
     * </ol>
     *
     * @param ownerId   el identificador único del propietario
     * @param serviceId el identificador del servicio a actualizar
     * @param request   los datos a actualizar (solo campos no nulos)
     * @return el servicio actualizado con precios convertidos e imágenes
     * @throws BusinessException si los datos de entrada son inválidos
     * @throws ResourceNotFoundException si el servicio no existe o no pertenece al propietario
     */
    // 🆕 NO es @Transactional - permite que toResponse se ejecute fuera de la transacción
    public AdditionalServiceResponse updateWithConvertedPrices(Long ownerId, Long serviceId, AdditionalServiceRequest request) {
        log.info("✏️ Actualizando servicio adicional ID: {} para owner ID: {}", serviceId, ownerId);

        // 1. La actualización se ejecuta en su propia transacción y hace commit
        AdditionalService updated = update(ownerId, serviceId, request);

        // 2. La conversión se ejecuta fuera de la transacción. Si falla, no hace rollback de la actualización.
        Accommodation acc = getAccommodation(ownerId);
        AdditionalServiceResponse response = toResponse(acc, updated);

        log.info("✅ Servicio adicional actualizado exitosamente: ID={}", updated.getId());

        return response;
    }

    /**
     * Elimina un servicio adicional y todas sus imágenes asociadas.
     *
     * <p>Gracias a {@code cascade = CascadeType.ALL, orphanRemoval = true}
     * en la entidad {@link AdditionalService}, las imágenes se eliminan
     * automáticamente de la base de datos cuando se elimina el servicio padre.</p>
     *
     * <p><strong>Nota sobre Cloudinary:</strong> La eliminación física de los
     * archivos en Cloudinary se maneja en el servicio
     * {@link AdditionalServiceImageService#deleteImage}, no aquí.</p>
     *
     * @param ownerId   el identificador único del propietario
     * @param serviceId el identificador del servicio a eliminar
     * @throws ResourceNotFoundException si el servicio no existe o no pertenece al propietario
     */
    @Transactional
    public void delete(Long ownerId, Long serviceId) {
        log.info("🗑️ Eliminando servicio adicional ID: {} para owner ID: {}", serviceId, ownerId);

        Accommodation acc = getAccommodation(ownerId);

        AdditionalService entity = serviceRepository
                .findByIdAndAccommodation_Id(serviceId, acc.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ese servicio en tu alojamiento."));

        String serviceName = entity.getName();
        serviceRepository.delete(entity);

        log.info("✅ Servicio adicional eliminado exitosamente: '{}' (ID={}) del alojamiento ID: {}",
                serviceName, serviceId, acc.getId());
    }

    // =========================================================================
    // MÉTODOS PRIVADOS DE CREACIÓN Y ACTUALIZACIÓN (TRANSACTIONALES)
    // =========================================================================

    /**
     * Crea un nuevo servicio adicional (método interno transaccional).
     *
     * <p>Este método es privado, transaccional y solo es llamado por métodos
     * públicos. Realiza las validaciones de negocio y construye la entidad.</p>
     *
     * @param ownerId el identificador único del propietario
     * @param request los datos del servicio a crear
     * @return la entidad persistida
     * @throws BusinessException si el nombre es vacío o el precio es <= 0
     */
    @Transactional
    private AdditionalService create(Long ownerId, AdditionalServiceRequest request) {
        Accommodation acc = getAccommodation(ownerId);

        // Validación de nombre obligatorio
        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException("El nombre del servicio es obligatorio.");
        }

        // Validación de precio mayor a 0
        if (request.basePrice() == null || request.basePrice().signum() <= 0) {
            throw new BusinessException("El precio base debe ser mayor a 0.");
        }

        // 🆕 Determinar el tipo de precio (por defecto TOTAL si no se especifica)
        // Esto permite que el frontend omita el campo y el sistema asuma TOTAL
        ServicePriceType priceType = request.priceType() != null
                ? request.priceType()
                : ServicePriceType.TOTAL;

        // Construcción de la entidad usando Builder pattern
        AdditionalService entity = AdditionalService.builder()
                .accommodation(acc)
                .name(request.name().trim())
                .description(request.description())
                .basePrice(request.basePrice())
                .priceType(priceType)
                .active(request.active() == null || request.active())
                .build();

        AdditionalService saved = serviceRepository.save(entity);

        log.debug(" Servicio persistido: ID={}, Name='{}', PriceType={}, BasePrice={}",
                saved.getId(), saved.getName(), priceType, saved.getBasePrice());

        return saved;
    }

    /**
     * Actualiza parcialmente un servicio adicional (método interno transaccional).
     *
     * <p>Solo actualiza los campos que vienen con valor no nulo en el {@code request}.
     * Esto permite actualizaciones parciales sin necesidad de enviar todos los campos.</p>
     *
     * <p>Ejemplo de uso:</p>
     * <pre>
     * PATCH /additional-services/5
     * Body: { "basePrice": 6000.00 }
     * → Solo actualiza el precio, mantiene el resto de campos sin cambios
     * </pre>
     *
     * @param ownerId   el identificador único del propietario
     * @param serviceId el identificador del servicio a actualizar
     * @param request   los datos a actualizar
     * @return la entidad actualizada y persistida
     * @throws BusinessException si el nombre es vacío o el precio es <= 0
     * @throws ResourceNotFoundException si el servicio no existe
     */
    @Transactional
    private AdditionalService update(Long ownerId, Long serviceId, AdditionalServiceRequest request) {
        Accommodation acc = getAccommodation(ownerId);

        // Buscar el servicio, validando que pertenezca al alojamiento del propietario
        AdditionalService entity = serviceRepository
                .findByIdAndAccommodation_Id(serviceId, acc.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ese servicio en tu alojamiento."));

        // Actualizar nombre si se proporciona
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BusinessException("El nombre no puede estar vacío.");
            }
            entity.setName(request.name().trim());
        }

        // Actualizar descripción si se proporciona
        if (request.description() != null) {
            entity.setDescription(request.description());
        }

        // Actualizar precio base si se proporciona
        if (request.basePrice() != null) {
            if (request.basePrice().signum() <= 0) {
                throw new BusinessException("El precio base debe ser mayor a 0.");
            }
            entity.setBasePrice(request.basePrice());
        }

        // 🆕 Actualizar tipo de precio si se proporciona
        if (request.priceType() != null) {
            entity.setPriceType(request.priceType());
        }

        // Actualizar estado activo/inactivo si se proporciona
        if (request.active() != null) {
            entity.setActive(request.active());
        }

        AdditionalService updated = serviceRepository.save(entity);

        log.debug(" Servicio actualizado: ID={}, Name='{}', PriceType={}, BasePrice={}",
                updated.getId(), updated.getName(), updated.getPriceType(), updated.getBasePrice());

        return updated;
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE MAPEO Y CONVERSIÓN
    // =========================================================================

    /**
     * Construye el DTO de respuesta con precios convertidos a todas las monedas habilitadas.
     *
     * <p><strong> Manejo tolerante a fallos:</strong> Si la conversión de una moneda
     * falla (por ejemplo, si la moneda no está habilitada o no hay tasa disponible),
     * simplemente se omite esa moneda en lugar de lanzar una excepción que cause
     * rollback de toda la transacción.</p>
     *
     * <p>Este método delega la construcción del DTO al método estático
     * {@link AdditionalServiceResponse#from(AdditionalService, List)}, el cual
     * también se encarga de mapear las imágenes asociadas al servicio
     * (gracias a que el repositorio las carga mediante {@code @EntityGraph}).</p>
     *
     * @param acc el alojamiento al que pertenece el servicio
     * @param s   el servicio adicional a mapear
     * @return el DTO completo con precios convertidos e imágenes
     */
    private AdditionalServiceResponse toResponse(Accommodation acc, AdditionalService s) {
        List<ConvertedPriceResponse> converted = new ArrayList<>();

        // Precio en la moneda base (siempre incluido - nunca falla)
        converted.add(ConvertedPriceResponse.of(acc.getBaseCurrency(), s.getBasePrice()));

        // Precios en las demás monedas habilitadas (USD, EUR)
        //  Manejo tolerante a fallos: si una conversión falla, solo se omite esa moneda
        for (Currency c : Currency.values()) {
            if (c == acc.getBaseCurrency()) continue;

            try {
                BigDecimal convertedPrice = conversionService.convert(
                        acc.getId(), s.getBasePrice(), c);

                if (convertedPrice != null) {
                    converted.add(ConvertedPriceResponse.of(c, convertedPrice));
                }
            } catch (RuntimeException e) {
                // Si la conversión falla (moneda no habilitada o sin tasa),
                // simplemente omitimos esa moneda en lugar de fallar toda la operación
                log.debug("⚠️ No se pudo convertir a {}: {}", c, e.getMessage());
            }
        }

        return AdditionalServiceResponse.from(s, acc.getBaseCurrency(), converted);
    }

    /**
     * Obtiene el alojamiento asociado al propietario o lanza una excepción si no existe.
     *
     * <p>Este método es un helper común que valida que el propietario tenga
     * un alojamiento registrado antes de permitir cualquier operación sobre
     * servicios adicionales.</p>
     *
     * @param ownerId el identificador único del propietario
     * @return la entidad {@link Accommodation}
     * @throws ResourceNotFoundException si el propietario no tiene un alojamiento registrado
     */
    private Accommodation getAccommodation(Long ownerId) {
        return accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registra primero los datos generales del alojamiento."));
    }
}