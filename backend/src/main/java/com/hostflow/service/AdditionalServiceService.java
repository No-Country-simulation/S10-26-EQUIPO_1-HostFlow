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
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.AdditionalServiceRepository;
import com.hostflow.repository.OwnerRepository;
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
 * <p><strong>🆕 Mejora de arquitectura (v2.2.0):</strong></p>
 * <p>Se agregaron métodos que aceptan el {@code ownerEmail} en lugar del {@code ownerId}.
 * Esto permite que los controladores deleguen la resolución del Owner al servicio,
 * eliminando la necesidad de inyectar repositorios en la capa web y cumpliendo
 * estrictamente con el principio de Separación de Responsabilidades.</p>
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
 * @author HostFlow Team
 * @version 2.2.0
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

    private final AdditionalServiceRepository serviceRepository;
    private final AccommodationRepository accommodationRepository;
    private final CurrencyConversionService conversionService;

    /** 🆕 Repositorio agregado para resolver el propietario a partir del email del token JWT. */
    private final OwnerRepository ownerRepository;

    // =========================================================================
    // 🆕 OPERACIONES DE LECTURA (READ) - BASADAS EN EMAIL
    // =========================================================================

    /**
     * Lista todos los servicios adicionales con precios convertidos,
     * resolviendo internamente el alojamiento a partir del email del propietario.
     *
     * @param ownerEmail el email del propietario autenticado
     * @return lista de servicios con precios convertidos e imágenes
     */
    @Transactional(readOnly = true)
    public List<AdditionalServiceResponse> listWithConvertedPricesByEmail(String ownerEmail) {
        return listWithConvertedPrices(getOwnerIdByEmail(ownerEmail));
    }

    // =========================================================================
    // 🆕 OPERACIONES DE ESCRITURA (CREATE/UPDATE/DELETE) - BASADAS EN EMAIL
    // =========================================================================

    /**
     * Crea un nuevo servicio adicional, resolviendo el propietario por email.
     *
     * @param ownerEmail el email del propietario autenticado
     * @param request    los datos del servicio a crear
     * @return el servicio creado con precios convertidos
     */
    public AdditionalServiceResponse createWithConvertedPricesByEmail(String ownerEmail, AdditionalServiceRequest request) {
        return createWithConvertedPrices(getOwnerIdByEmail(ownerEmail), request);
    }

    /**
     * Actualiza un servicio adicional, resolviendo el propietario por email.
     *
     * @param ownerEmail el email del propietario autenticado
     * @param serviceId  el identificador del servicio a actualizar
     * @param request    los datos a actualizar (solo campos no nulos)
     * @return el servicio actualizado con precios convertidos
     */
    public AdditionalServiceResponse updateWithConvertedPricesByEmail(String ownerEmail, Long serviceId, AdditionalServiceRequest request) {
        return updateWithConvertedPrices(getOwnerIdByEmail(ownerEmail), serviceId, request);
    }

    /**
     * Elimina un servicio adicional, resolviendo el propietario por email.
     *
     * @param ownerEmail el email del propietario autenticado
     * @param serviceId  el identificador del servicio a eliminar
     */
    @Transactional
    public void deleteByEmail(String ownerEmail, Long serviceId) {
        delete(getOwnerIdByEmail(ownerEmail), serviceId);
    }

    // =========================================================================
    // MÉTODOS ORIGINALES (Se mantienen para compatibilidad con Tests existentes)
    // =========================================================================

    /**
     * Lista todos los servicios adicionales con precios convertidos a todas
     * las monedas habilitadas e imágenes asociadas.
     *
     * @param ownerId el identificador único del propietario
     * @return lista de servicios con precios convertidos e imágenes
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

    /**
     * Crea un nuevo servicio adicional y retorna el DTO con precios convertidos.
     *
     * <p><strong>Importante:</strong> Este método NO es @Transactional.
     * Delega la creación en el método {@link #create} (que SÍ lo es).
     * Esto permite que la conversión de moneda se ejecute FUERA de la transacción,
     * evitando el {@code UnexpectedRollbackException} si la conversión falla.</p>
     *
     * @param ownerId el identificador único del propietario
     * @param request los datos del servicio a crear
     * @return el servicio creado con precios convertidos
     */
    public AdditionalServiceResponse createWithConvertedPrices(Long ownerId, AdditionalServiceRequest request) {
        log.info("🆕 Creando nuevo servicio adicional para owner ID: {}", ownerId);

        // 1. La creación se ejecuta en su propia transacción y hace commit
        AdditionalService saved = create(ownerId, request);

        // 2. La conversión se ejecuta fuera de la transacción. Si falla, no hace rollback.
        Accommodation acc = getAccommodation(ownerId);
        AdditionalServiceResponse response = toResponse(acc, saved);

        log.info("✅ Servicio adicional creado exitosamente: ID={}, Name='{}'", saved.getId(), saved.getName());
        return response;
    }

    /**
     * Actualiza un servicio adicional y retorna el DTO con precios convertidos.
     *
     * <p><strong>Importante:</strong> Este método NO es @Transactional.
     * Delega la actualización en el método {@link #update} (que SÍ lo es),
     * protegiendo la transacción de fallos en la conversión de moneda.</p>
     *
     * @param ownerId   el identificador único del propietario
     * @param serviceId el identificador del servicio a actualizar
     * @param request   los datos a actualizar (solo campos no nulos)
     * @return el servicio actualizado con precios convertidos
     */
    public AdditionalServiceResponse updateWithConvertedPrices(Long ownerId, Long serviceId, AdditionalServiceRequest request) {
        log.info("✏️ Actualizando servicio adicional ID: {} para owner ID: {}", serviceId, ownerId);

        // 1. La actualización se ejecuta en su propia transacción y hace commit
        AdditionalService updated = update(ownerId, serviceId, request);

        // 2. La conversión se ejecuta fuera de la transacción.
        Accommodation acc = getAccommodation(ownerId);
        AdditionalServiceResponse response = toResponse(acc, updated);

        log.info("✅ Servicio adicional actualizado exitosamente: ID={}", updated.getId());
        return response;
    }

    /**
     * Elimina un servicio adicional y todas sus imágenes asociadas.
     *
     * <p>Gracias a {@code cascade = CascadeType.ALL, orphanRemoval = true}
     * en la entidad, las imágenes se eliminan automáticamente de la BD.</p>
     *
     * @param ownerId   el identificador único del propietario
     * @param serviceId el identificador del servicio a eliminar
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

    @Transactional
    private AdditionalService create(Long ownerId, AdditionalServiceRequest request) {
        Accommodation acc = getAccommodation(ownerId);

        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException("El nombre del servicio es obligatorio.");
        }
        if (request.basePrice() == null || request.basePrice().signum() <= 0) {
            throw new BusinessException("El precio base debe ser mayor a 0.");
        }

        // Determinar el tipo de precio (por defecto TOTAL si no se especifica)
        ServicePriceType priceType = request.priceType() != null ? request.priceType() : ServicePriceType.TOTAL;

        AdditionalService entity = AdditionalService.builder()
                .accommodation(acc)
                .name(request.name().trim())
                .description(request.description())
                .basePrice(request.basePrice())
                .priceType(priceType)
                .active(request.active() == null || request.active())
                .build();

        AdditionalService saved = serviceRepository.save(entity);
        log.debug("💾 Servicio persistido: ID={}, Name='{}', PriceType={}", saved.getId(), saved.getName(), priceType);

        return saved;
    }

    @Transactional
    private AdditionalService update(Long ownerId, Long serviceId, AdditionalServiceRequest request) {
        Accommodation acc = getAccommodation(ownerId);

        AdditionalService entity = serviceRepository
                .findByIdAndAccommodation_Id(serviceId, acc.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ese servicio en tu alojamiento."));

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BusinessException("El nombre no puede estar vacío.");
            }
            entity.setName(request.name().trim());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.basePrice() != null) {
            if (request.basePrice().signum() <= 0) {
                throw new BusinessException("El precio base debe ser mayor a 0.");
            }
            entity.setBasePrice(request.basePrice());
        }
        if (request.priceType() != null) {
            entity.setPriceType(request.priceType());
        }
        if (request.active() != null) {
            entity.setActive(request.active());
        }

        AdditionalService updated = serviceRepository.save(entity);
        log.debug("💾 Servicio actualizado: ID={}, Name='{}'", updated.getId(), updated.getName());

        return updated;
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE MAPEO Y RESOLUCIÓN
    // =========================================================================

    /**
     * Construye el DTO de respuesta con precios convertidos a todas las monedas habilitadas.
     *
     * <p><strong>Manejo tolerante a fallos:</strong> Si la conversión de una moneda falla,
     * simplemente se omite esa moneda en lugar de lanzar una excepción que cause rollback.</p>
     *
     * @param acc el alojamiento al que pertenece el servicio
     * @param s   el servicio adicional a mapear
     * @return el DTO completo con precios convertidos e imágenes
     */
    private AdditionalServiceResponse toResponse(Accommodation acc, AdditionalService s) {
        List<ConvertedPriceResponse> converted = new ArrayList<>();

        // Precio en la moneda base (siempre incluido)
        converted.add(ConvertedPriceResponse.of(acc.getBaseCurrency(), s.getBasePrice()));

        // Precios en las demás monedas habilitadas (USD, EUR)
        for (Currency c : Currency.values()) {
            if (c == acc.getBaseCurrency()) continue;

            try {
                BigDecimal convertedPrice = conversionService.convert(acc.getId(), s.getBasePrice(), c);
                if (convertedPrice != null) {
                    converted.add(ConvertedPriceResponse.of(c, convertedPrice));
                }
            } catch (RuntimeException e) {
                log.debug("⚠️ No se pudo convertir a {}: {}", c, e.getMessage());
            }
        }

        return AdditionalServiceResponse.from(s, acc.getBaseCurrency(), converted);
    }

    /**
     * 🆕 Obtiene el ID del propietario a partir de su email.
     * Centraliza la lógica de búsqueda para evitar duplicación en los controladores.
     *
     * @param ownerEmail el email del propietario autenticado
     * @return el ID del propietario
     * @throws ResourceNotFoundException si no se encuentra el propietario
     */
    private Long getOwnerIdByEmail(String ownerEmail) {
        return ownerRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el propietario con email: " + ownerEmail))
                .getId();
    }

    /**
     * Obtiene el alojamiento asociado al propietario.
     *
     * @param ownerId el identificador único del propietario
     * @return la entidad {@link Accommodation}
     * @throws ResourceNotFoundException si el propietario no tiene un alojamiento registrado
     */
    private Accommodation getAccommodation(Long ownerId) {
        return accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registra primero losiggins datos generales del alojamiento."));
    }
}