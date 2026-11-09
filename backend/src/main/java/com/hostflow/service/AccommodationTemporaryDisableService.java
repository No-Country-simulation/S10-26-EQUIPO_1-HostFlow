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
 * <p><strong>🆕 Mejora de arquitectura (v2.1):</strong></p>
 * <p>Se agregaron métodos que aceptan el email del propietario autenticado
 * en lugar del ownerId, delegando la resolución del Owner al servicio
 * y eliminando la necesidad de que los controladores inyecten repositorios.</p>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationTemporaryDisableService {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    private final AccommodationTemporaryDisableRepository disableRepository;
    private final OwnerRepository ownerRepository;

    // =========================================================================
    // 🆕 MÉTODOS PÚBLICOS QUE ACEPTAN EMAIL (Para controladores refactorizados)
    // =========================================================================

    /**
     * Desactiva temporalmente el alojamiento del propietario autenticado.
     *
     * <p>Este método resuelve internamente el Owner a partir del email,
     * evitando que el controlador necesite inyectar repositorios.</p>
     *
     * @param ownerEmail   el email del propietario autenticado (extraído del JWT)
     * @param reason       el motivo de la desactivación
     * @param disabledFrom fecha de inicio de la desactivación
     * @param disabledUntil fecha estimada de reactivación (puede ser null)
     * @return el registro de desactivación creado
     * @throws ResourceNotFoundException si no se encuentra el propietario
     * @throws BusinessException si ya existe una desactivación activa o las fechas son inválidas
     */
    @Transactional
    public AccommodationTemporaryDisable disableAccommodationByEmail(
            String ownerEmail, String reason, LocalDateTime disabledFrom, LocalDateTime disabledUntil) {

        Owner owner = getOwnerByEmail(ownerEmail);
        return disableAccommodation(owner.getId(), reason, disabledFrom, disabledUntil);
    }

    /**
     * Reactiva el alojamiento del propietario autenticado.
     *
     * @param ownerEmail el email del propietario autenticado
     * @return el registro de desactivación actualizado
     * @throws BusinessException si no hay una desactivación activa
     */
    @Transactional
    public AccommodationTemporaryDisable enableAccommodationByEmail(String ownerEmail) {
        Long ownerId = getOwnerIdByEmail(ownerEmail);
        return enableAccommodation(ownerId);
    }

    /**
     * Obtiene el estado actual de desactivación del alojamiento.
     *
     * @param ownerEmail el email del propietario autenticado
     * @return la desactivación activa actual, o null si no hay ninguna
     */
    @Transactional(readOnly = true)
    public AccommodationTemporaryDisable getActiveDisableByEmail(String ownerEmail) {
        Long ownerId = getOwnerIdByEmail(ownerEmail);
        return getActiveDisable(ownerId);
    }

    /**
     * Obtiene el historial de desactivaciones del propietario.
     *
     * @param ownerEmail el email del propietario autenticado
     * @return lista de desactivaciones ordenadas por fecha (más reciente primero)
     */
    @Transactional(readOnly = true)
    public List<AccommodationTemporaryDisable> getDisableHistoryByEmail(String ownerEmail) {
        Long ownerId = getOwnerIdByEmail(ownerEmail);
        return getDisableHistory(ownerId);
    }

    /**
     * Verifica si el alojamiento está disponible en una fecha específica.
     *
     * @param ownerEmail el email del propietario autenticado
     * @param date la fecha a consultar
     * @return true si el alojamiento está disponible, false si está desactivado
     */
    @Transactional(readOnly = true)
    public boolean isAccommodationAvailableByEmail(String ownerEmail, LocalDateTime date) {
        Long ownerId = getOwnerIdByEmail(ownerEmail);
        return isAccommodationAvailable(ownerId, date);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS - RESOLUCIÓN DE OWNER
    // =========================================================================

    /**
     * Obtiene el ID del propietario a partir de su email.
     *
     * <p>Este método centraliza la lógica de búsqueda del Owner,
     * evitando duplicación en los controladores.</p>
     *
     * @param ownerEmail el email del propietario
     * @return el ID del propietario
     * @throws ResourceNotFoundException si no se encuentra el propietario
     */
    private Long getOwnerIdByEmail(String ownerEmail) {
        return getOwnerByEmail(ownerEmail).getId();
    }

    /**
     * Obtiene la entidad Owner completa a partir de su email.
     *
     * @param ownerEmail el email del propietario
     * @return la entidad Owner
     * @throws ResourceNotFoundException si no se encuentra el propietario
     */
    private Owner getOwnerByEmail(String ownerEmail) {
        return ownerRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el propietario con email: " + ownerEmail));
    }

    // =========================================================================
    // MÉTODOS ORIGINALES (Se mantienen para compatibilidad)
    // =========================================================================

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
        log.info("🚫 Alojamiento desactivado temporalmente - Owner: {}, Motivo: {}, Desde: {}, Hasta: {}",
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