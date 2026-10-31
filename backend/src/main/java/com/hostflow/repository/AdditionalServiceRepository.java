package com.hostflow.repository;

import com.hostflow.model.AdditionalService;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la gestión de la entidad {@link AdditionalService}.
 *
 * <p>Proporciona operaciones de consulta personalizadas para administrar
 * los servicios adicionales ofrecidos por un alojamiento (ej: desayuno,
 * transfer, spa, estacionamiento, etc.).</p>
 *
 * <p><strong>Características:</strong></p>
 * <ul>
 *   <li>Extiende {@link JpaRepository} para heredar operaciones CRUD básicas.</li>
 *   <li>Métodos de consulta basados en la convención de nombres de Spring Data JPA.</li>
 *   <li>Soporte para filtrar servicios activos e inactivos por alojamiento.</li>
 *   <li> Uso de {@link EntityGraph} para cargar las imágenes junto con el servicio
 *       en una sola consulta, evitando el problema N+1 de Hibernate.</li>
 * </ul>
 *
 * <p><strong>🆕 Optimización de rendimiento (v2.0):</strong></p>
 * <p>Se agregaron anotaciones {@code @EntityGraph} a los métodos de consulta que
 * devuelven listas de servicios. Esto permite cargar la relación {@code images}
 * (lista de imágenes del servicio) en la misma consulta SQL, evitando múltiples
 * consultas adicionales (problema N+1) cuando se serializa la respuesta JSON.</p>
 *
 * <p><strong>Ejemplo del problema N+1 sin EntityGraph:</strong></p>
 * <pre>
 * 1. SELECT * FROM additional_services WHERE accommodation_id = 1  (1 consulta)
 * 2. SELECT * FROM additional_service_images WHERE service_id = 1  (N consultas, una por cada servicio)
 * 3. SELECT * FROM additional_service_images WHERE service_id = 2
 * 4. SELECT * FROM additional_service_images WHERE service_id = 3
 *    ...
 * </pre>
 *
 * <p><strong>Solución con EntityGraph:</strong></p>
 * <pre>
 * 1. SELECT s.*, i.* FROM additional_services s
 *    LEFT JOIN additional_service_images i ON s.id = i.additional_service_id
 *    WHERE s.accommodation_id = 1  (1 sola consulta con JOIN)
 * </pre>
 *
 * @author HostFlow Team
 * @version 2.0.0
 * @see AdditionalService
 * @see com.hostflow.service.AdditionalServiceService
 */
@Repository
public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {

    /**
     * Lista todos los servicios adicionales de un alojamiento específico,
     * ordenados por fecha de creación ascendente.
     *
     * <p>Se utiliza principalmente en el panel del propietario para mostrar
     * el listado completo de servicios, incluyendo los que están desactivados.</p>
     *
     * <p><strong>🆕 Optimización:</strong> Carga las imágenes del servicio en la
     * misma consulta mediante {@link EntityGraph}, evitando el problema N+1.</p>
     *
     * @param accommodationId el identificador único del alojamiento
     * @return lista de servicios adicionales ordenados cronológicamente,
     *         con sus imágenes ya cargadas
     */
    @EntityGraph(attributePaths = {"images", "accommodation"})
    List<AdditionalService> findAllByAccommodation_IdOrderByCreatedAtAsc(Long accommodationId);

    /**
     * Lista únicamente los servicios adicionales activos de un alojamiento.
     *
     * <p>Este método es utilizado en los endpoints públicos ({@code PublicPricingController})
     * para mostrar a los huéspedes solo los servicios que el propietario
     * ha marcado como disponibles ({@code active = true}).</p>
     *
     * <p><strong>🆕 Optimización:</strong> Carga las imágenes del servicio en la
     * misma consulta mediante {@link EntityGraph}, evitando el problema N+1.</p>
     *
     * @param accommodationId el identificador único del alojamiento
     * @return lista de servicios adicionales activos ordenados cronológicamente,
     *         con sus imágenes ya cargadas
     */
    @EntityGraph(attributePaths = {"images"})
    List<AdditionalService> findAllByAccommodation_IdAndActiveTrueOrderByCreatedAtAsc(Long accommodationId);

    /**
     * Busca un servicio adicional por su ID, validando que pertenezca al alojamiento indicado.
     *
     * <p>Se utiliza para operaciones de actualización y eliminación, garantizando
     * que un propietario solo pueda modificar servicios de SU PROPIO alojamiento.</p>
     *
     * <p><strong>🆕 Optimización:</strong> Carga las imágenes del servicio en la
     * misma consulta mediante {@link EntityGraph}, evitando el problema N+1.</p>
     *
     * @param id              el identificador único del servicio
     * @param accommodationId el identificador único del alojamiento propietario
     * @return un {@link Optional} con el servicio si existe y pertenece al alojamiento,
     *         o vacío si no. Si existe, sus imágenes ya estarán cargadas.
     */
    @EntityGraph(attributePaths = {"images"})
    Optional<AdditionalService> findByIdAndAccommodation_Id(Long id, Long accommodationId);
}