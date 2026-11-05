package com.hostflow.service;

import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.AccommodationTemporaryDisable;
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationTemporaryDisableRepository;
import com.hostflow.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para gestionar la desactivación temporal de alojamientos
 * por parte de sus propietarios.
 *
 * @author HostFlow Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationTemporaryDisableService {

    private final AccommodationTemporaryDisableRepository disableRepository;
    private final OwnerRepository ownerRepository;

    /**
     * Desactiva temporalmente el alojamiento de un owner.
     *
     * @param ownerId      el ID del dueño
     * @param reason       el motivo de la desactivación
     * @param disabledFrom fecha de inicio de la desactivación
     * @param disabledUntil fecha estimada de reactivación (puede ser null)
     * @return el registro de desactivación creado
     */
    @Transactional
    public AccommodationTemporaryDisable disableAccommodation(
            Long ownerId, String reason, LocalDateTime disabledFrom, LocalDateTime disabledUntil) {

        // Verificar que el owner existe
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el dueño con el ID: " + ownerId));

        // Verificar que el owner no tenga una desactivación activa
        if (disableRepository.findByOwnerIdAndIsActiveTrue(ownerId).isPresent()) {
            throw new BusinessException(
                    "Tu alojamiento ya tiene una desactivación temporal activa. " +
                            "Debes reactivarlo antes de crear una nueva desactivación.");
        }

        // Validar fechas
        if (disabledFrom.isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    "La fecha de inicio de la desactivación no puede ser en el pasado.");
        }

        if (disabledUntil != null && disabledUntil.isBefore(disabledFrom)) {
            throw new BusinessException(
                    "La fecha de reactivación debe ser posterior a la fecha de desactivación.");
        }

        // Crear registro de desactivación
        AccommodationTemporaryDisable disable = AccommodationTemporaryDisable.builder()
                .ownerId(ownerId)
                .reason(reason)
                .disabledFrom(disabledFrom)
                .disabledUntil(disabledUntil)
                .isActive(true)
                .build();

        AccommodationTemporaryDisable savedDisable = disableRepository.save(disable);

        log.info(" Alojamiento desactivado temporalmente - Owner: {}, Motivo: {}, Desde: {}, Hasta: {}",
                owner.getEmail(), reason, disabledFrom, disabledUntil);

        return savedDisable;
    }

    /**
     * Reactiva el alojamiento de un owner.
     *
     * @param ownerId el ID del dueño
     * @return el registro de desactivación actualizado
     */
    @Transactional
    public AccommodationTemporaryDisable enableAccommodation(Long ownerId) {
        // Buscar desactivación activa
        AccommodationTemporaryDisable disable = disableRepository.findByOwnerIdAndIsActiveTrue(ownerId)
                .orElseThrow(() -> new BusinessException(
                        "No hay una desactivación temporal activa para tu alojamiento."));

        // Reactivar
        disable.setIsActive(false);
        disable.setUpdatedAt(LocalDateTime.now());

        AccommodationTemporaryDisable updatedDisable = disableRepository.save(disable);

        Owner owner = ownerRepository.findById(ownerId).orElse(null);
        log.info("✅ Alojamiento reactivado - Owner: {}", owner != null ? owner.getEmail() : ownerId);

        return updatedDisable;
    }

    /**
     * Obtiene el estado actual de desactivación del alojamiento de un owner.
     *
     * @param ownerId el ID del dueño
     * @return la desactivación activa actual, o null si no hay ninguna
     */
    @Transactional(readOnly = true)
    public AccommodationTemporaryDisable getActiveDisable(Long ownerId) {
        return disableRepository.findByOwnerIdAndIsActiveTrue(ownerId).orElse(null);
    }

    /**
     * Obtiene el historial de desactivaciones de un owner.
     *
     * @param ownerId el ID del dueño
     * @return lista de desactivaciones ordenadas por fecha (más reciente primero)
     */
    @Transactional(readOnly = true)
    public List<AccommodationTemporaryDisable> getDisableHistory(Long ownerId) {
        return disableRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    /**
     * Verifica si el alojamiento de un owner está disponible en una fecha específica.
     *
     * @param ownerId el ID del dueño
     * @param date la fecha a consultar
     * @return true si el alojamiento está disponible, false si está desactivado
     */
    @Transactional(readOnly = true)
    public boolean isAccommodationAvailable(Long ownerId, LocalDateTime date) {
        return !disableRepository.findActiveDisableAtDate(ownerId, date).isPresent();
    }
}