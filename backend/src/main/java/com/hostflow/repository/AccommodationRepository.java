package com.hostflow.repository;

import com.hostflow.model.Accommodation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositorio JPA para la gestión de la entidad {@link Accommodation}.
 *
 * <p>Proporciona operaciones de persistencia y consultas personalizadas
 * para administrar los alojamientos registrados en la plataforma HostFlow.</p>
 *
 * <p><strong>Características clave:</strong></p>
 * <ul>
 *   <li>Carga optimizada de relaciones (ej. galería de imágenes) mediante {@link EntityGraph}
 *       para prevenir el problema N+1 de Hibernate.</li>
 *   <li>Bloqueo pesimista ({@link LockModeType#PESSIMISTIC_WRITE}) para prevenir
 *       condiciones de carrera (race conditions) durante actualizaciones concurrentes.</li>
 *   <li>Consultas de métricas agregadas para el Dashboard del Administrador.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.2.0
 * @see Accommodation
 * @see com.hostflow.model.AccommodationTemporaryDisable
 */
@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

    // =========================================================================
    // CONSULTAS DE LECTURA OPTIMIZADA
    // =========================================================================

    /**
     * Busca el alojamiento asociado a un propietario específico, cargando
     * de manera eager (ansiosa) la colección de imágenes en una sola consulta SQL.
     *
     * <p><strong>¿Cuándo usarlo?</strong></p>
     * <ul>
     *   <li>Al mostrar el perfil completo del alojamiento al propietario.</li>
     *   <li>Cuando se necesita acceder a la galería de imágenes inmediatamente
     *       sin provocar consultas adicionales (N+1) durante la serialización JSON.</li>
     * </ul>
     *
     * @param ownerId el identificador único del propietario
     * @return un {@link Optional} con el alojamiento y sus imágenes cargadas,
     *         o vacío si el propietario no tiene un alojamiento registrado
     */
    @EntityGraph(attributePaths = "images")
    Optional<Accommodation> findByOwner_Id(Long ownerId);

    // =========================================================================
    // CONSULTAS DE ESCRITURA CON BLOQUEO (CONTROL DE CONCURRENCIA)
    // =========================================================================

    /**
     * Busca el alojamiento de un propietario aplicando un bloqueo pesimista
     * de escritura ({@code PESSIMISTIC_WRITE}) a nivel de base de datos.
     *
     * <p><strong>Propósito:</strong> Serializa las operaciones de modificación
     * (ej. subir imágenes, actualizar perfil) sobre el mismo alojamiento.
     * Esto previene condiciones de carrera donde múltiples peticiones simultáneas
     * podrían exceder límites de negocio (ej. máximo de imágenes permitidas)
     * al leer el estado de la base de datos exactamente al mismo tiempo.</p>
     *
     * <p>La base de datos bloqueará la fila hasta que la transacción actual
     * se complete (commit o rollback), garantizando consistencia absoluta.</p>
     *
     * <p><strong>¿Cuándo usarlo?</strong></p>
     * <ul>
     *   <li>En servicios que modifican el alojamiento y validan límites
     *       (ej. {@code AccommodationProfileService.uploadGalleryImage}).</li>
     * </ul>
     *
     * @param ownerId el identificador único del propietario
     * @return un {@link Optional} con el alojamiento bloqueado para actualización,
     *         o vacío si no existe
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Accommodation a WHERE a.owner.id = :ownerId")
    Optional<Accommodation> findByOwner_IdForUpdate(@Param("ownerId") Long ownerId);

    // =========================================================================
    // VALIDACIONES DE EXISTENCIA
    // =========================================================================

    /**
     * Verifica si un propietario ya tiene un alojamiento registrado en el sistema.
     *
     * <p>Se utiliza para validar la regla de negocio de "un alojamiento
     * por propietario" antes de permitir la creación de un nuevo perfil,
     * evitando consultas innecesarias que traigan toda la entidad a memoria.</p>
     *
     * @param ownerId el identificador único del propietario
     * @return {@code true} si el propietario ya tiene un alojamiento,
     *         {@code false} en caso contrario
     */
    boolean existsByOwner_Id(Long ownerId);

    // =========================================================================
    // 🆕 MÉTRICAS PARA DASHBOARD DEL ADMINISTRADOR (ADMIN)
    // =========================================================================

    /**
     * Cuenta el total de alojamientos registrados en toda la plataforma HostFlow.
     *
     * <p>Utilizado en el dashboard global del Admin para mostrar el volumen
     * total de propiedades gestionadas en el sistema.</p>
     *
     * @return cantidad total de alojamientos
     */
    @Query("SELECT COUNT(a) FROM Accommodation a")
    long countTotalAccommodations();

    /**
     * Cuenta la cantidad de alojamientos registrados en el último mes.
     *
     * <p>Utilizado en el dashboard del Admin para medir el crecimiento
     * y la tasa de adquisición de nuevos alojamientos en el período actual.</p>
     *
     * @param startOfMonth fecha de inicio del mes actual (ej. 2026-11-01T00:00:00)
     * @return cantidad de alojamientos creados desde esa fecha
     */
    @Query("SELECT COUNT(a) FROM Accommodation a WHERE a.createdAt >= :startOfMonth")
    long countAccommodationsLastMonth(@Param("startOfMonth") LocalDateTime startOfMonth);

    /**
     * 🆕 Cuenta los alojamientos que tienen una desactivación temporal activa.
     *
     * <p>Utilizado en el dashboard del Admin para monitorear propiedades que
     * no están operativas o han sido suspendidas temporalmente por sus dueños.</p>
     *
     * <p><strong>Nota importante:</strong> Esta consulta realiza un JOIN con la
     * entidad {@link com.hostflow.model.AccommodationTemporaryDisable} para verificar
     * el estado de desactivación. Esto resuelve el error de compilación anterior
     * donde se intentaba acceder a un campo {@code active} que no existe en
     * la entidad {@link Accommodation}.</p>
     *
     * @return cantidad de alojamientos con desactivación temporal activa
     */
    @Query("""
        SELECT COUNT(DISTINCT a) FROM Accommodation a
        JOIN AccommodationTemporaryDisable d ON a.owner.id = d.ownerId
        WHERE d.isActive = true
    """)
    long countInactiveAccommodations();
}