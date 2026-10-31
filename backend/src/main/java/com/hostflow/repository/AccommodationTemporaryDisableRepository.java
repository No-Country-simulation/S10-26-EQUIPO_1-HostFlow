package com.hostflow.repository;

import com.hostflow.model.AccommodationTemporaryDisable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la gestión de desactivaciones temporales de alojamientos.
 *
 * <p>Proporciona operaciones de consulta personalizadas para verificar
 * el estado de disponibilidad de alojamientos en fechas específicas.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Repository
public interface AccommodationTemporaryDisableRepository extends JpaRepository<AccommodationTemporaryDisable, Long> {

    /**
     * Busca la desactivación temporal activa actual de un propietario.
     *
     * @param ownerId el ID del dueño
     * @return Optional con la desactivación activa, o vacío si no hay ninguna
     */
    Optional<AccommodationTemporaryDisable> findByOwnerIdAndIsActiveTrue(Long ownerId);

    /**
     * Busca todas las desactivaciones temporales de un propietario,
     * ordenadas por fecha de creación (más reciente primero).
     *
     * @param ownerId el ID del dueño
     * @return lista de desactivaciones ordenadas cronológicamente
     */
    List<AccommodationTemporaryDisable> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    /**
     * Busca desactivaciones temporales activas que estén vigentes en una fecha específica.
     *
     * <p>Esta consulta verifica si existe una desactivación activa que cubra
     * la fecha consultada, considerando tanto el inicio como el fin del período.</p>
     *
     * @param ownerId el ID del dueño
     * @param date la fecha a consultar
     * @return Optional con la desactivación vigente en esa fecha
     */
    @Query("SELECT a FROM AccommodationTemporaryDisable a WHERE a.ownerId = :ownerId AND a.isActive = true AND a.disabledFrom <= :date AND (a.disabledUntil IS NULL OR a.disabledUntil >= :date)")
    Optional<AccommodationTemporaryDisable> findActiveDisableAtDate(
            @Param("ownerId") Long ownerId,
            @Param("date") LocalDateTime date
    );
}