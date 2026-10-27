package com.hostflow.service;


import com.hostflow.dto.request.AdditionalServiceRequest;
import com.hostflow.dto.response.AdditionalServiceResponse;
import com.hostflow.dto.response.ConvertedPriceResponse;
import com.hostflow.enums.Currency;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestionar los servicios adicionales de un alojamiento.
 *
 * <p>Los servicios adicionales se almacenan con precio en la moneda base (ARS).
 * La conversión a otras monedas se realiza en tiempo real al consultarlos.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdditionalServiceService {

    private final AdditionalServiceRepository serviceRepository;
    private final AccommodationRepository accommodationRepository;
    private final CurrencyConversionService conversionService;

    /**
     * Lista todos los servicios adicionales con precios convertidos a todas las monedas habilitadas.
     *
     * @param ownerId el identificador único del propietario
     * @return lista de servicios con precios convertidos
     */
    @Transactional(readOnly = true)
    public List<AdditionalServiceResponse> listWithConvertedPrices(Long ownerId) {
        Accommodation acc = getAccommodation(ownerId);
        return serviceRepository.findAllByAccommodation_IdOrderByCreatedAtAsc(acc.getId()).stream()
                .map(s -> toResponse(acc, s))
                .toList();
    }

    /**
     * Crea un nuevo servicio adicional y retorna el DTO con precios convertidos.
     *
     * @param ownerId el identificador único del propietario
     * @param request los datos del servicio a crear
     * @return el servicio creado con precios convertidos
     */
    @Transactional
    public AdditionalServiceResponse createWithConvertedPrices(Long ownerId, AdditionalServiceRequest request) {
        Accommodation acc = getAccommodation(ownerId);
        AdditionalService saved = create(ownerId, request);
        return toResponse(acc, saved);
    }

    /**
     * Actualiza un servicio adicional y retorna el DTO con precios convertidos.
     *
     * @param ownerId   el identificador único del propietario
     * @param serviceId el identificador del servicio a actualizar
     * @param request   los datos a actualizar
     * @return el servicio actualizado con precios convertidos
     */
    @Transactional
    public AdditionalServiceResponse updateWithConvertedPrices(Long ownerId, Long serviceId, AdditionalServiceRequest request) {
        Accommodation acc = getAccommodation(ownerId);
        AdditionalService updated = update(ownerId, serviceId, request);
        return toResponse(acc, updated);
    }

    /**
     * Elimina un servicio adicional.
     *
     * @param ownerId   el identificador único del propietario
     * @param serviceId el identificador del servicio a eliminar
     */
    @Transactional
    public void delete(Long ownerId, Long serviceId) {
        Accommodation acc = getAccommodation(ownerId);
        AdditionalService entity = serviceRepository
                .findByIdAndAccommodation_Id(serviceId, acc.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ese servicio en tu alojamiento."));
        serviceRepository.delete(entity);
        log.info("🗑️ Servicio adicional eliminado: '{}' del alojamiento {}",
                entity.getName(), acc.getId());
    }

    /**
     * Crea un nuevo servicio adicional (método interno).
     */
    private AdditionalService create(Long ownerId, AdditionalServiceRequest request) {
        Accommodation acc = getAccommodation(ownerId);

        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException("El nombre del servicio es obligatorio.");
        }
        if (request.basePrice() == null || request.basePrice().signum() <= 0) {
            throw new BusinessException("El precio base debe ser mayor a 0.");
        }

        AdditionalService entity = AdditionalService.builder()
                .accommodation(acc)
                .name(request.name().trim())
                .description(request.description())
                .basePrice(request.basePrice())
                .active(request.active() == null || request.active())
                .build();

        AdditionalService saved = serviceRepository.save(entity);
        log.info("➕ Servicio adicional creado: '{}' ({} ARS) para alojamiento {}",
                saved.getName(), saved.getBasePrice(), acc.getId());
        return saved;
    }

    /**
     * Actualiza parcialmente un servicio adicional (método interno).
     */
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
        if (request.description() != null) entity.setDescription(request.description());
        if (request.basePrice() != null) {
            if (request.basePrice().signum() <= 0) {
                throw new BusinessException("El precio base debe ser mayor a 0.");
            }
            entity.setBasePrice(request.basePrice());
        }
        if (request.active() != null) entity.setActive(request.active());

        return serviceRepository.save(entity);
    }

    /**
     * Construye el DTO de respuesta con precios convertidos a todas las monedas habilitadas.
     */
    private AdditionalServiceResponse toResponse(Accommodation acc, AdditionalService s) {
        List<ConvertedPriceResponse> converted = new ArrayList<>();

        // Precio en la moneda base (siempre incluido)
        converted.add(ConvertedPriceResponse.of(acc.getBaseCurrency(), s.getBasePrice()));

        // Precios en las demás monedas habilitadas
        for (Currency c : Currency.values()) {
            if (c == acc.getBaseCurrency()) continue;
            try {
                converted.add(ConvertedPriceResponse.of(
                        c, conversionService.convert(acc.getId(), s.getBasePrice(), c)));
            } catch (RuntimeException ignored) {
                // Moneda no habilitada o sin tasa: se omite
            }
        }

        return AdditionalServiceResponse.from(s, converted);
    }

    /**
     * Obtiene el alojamiento asociado al propietario o lanza una excepción si no existe.
     */
    private Accommodation getAccommodation(Long ownerId) {
        return accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registra primero los datos generales del alojamiento."));
    }
}