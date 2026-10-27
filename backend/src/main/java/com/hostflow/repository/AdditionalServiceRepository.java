package com.hostflow.repository;

import com.hostflow.model.AdditionalService;
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
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
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
     * @param accommodationId el identificador único del alojamiento
     * @return lista de servicios adicionales ordenados cronológicamente
     */
    List<AdditionalService> findAllByAccommodation_IdOrderByCreatedAtAsc(Long accommodationId);

    /**
     * Lista únicamente los servicios adicionales activos de un alojamiento.
     *
     * <p>Este método es utilizado en los endpoints públicos ({@code PublicPricingController})
     * para mostrar a los huéspedes solo los servicios que el propietario
     * ha marcado como disponibles ({@code active = true}).</p>
     *
     * @param accommodationId el identificador único del alojamiento
     * @return lista de servicios adicionales activos ordenados cronológicamente
     */
    List<AdditionalService> findAllByAccommodation_IdAndActiveTrueOrderByCreatedAtAsc(Long accommodationId);

    /**
     * Busca un servicio adicional por su ID, validando que pertenezca al alojamiento indicado.
     *
     * <p>Se utiliza para operaciones de actualización y eliminación, garantizando
     * que un propietario solo pueda modificar servicios de SU PROPIO alojamiento.</p>
     *
     * @param id              el identificador único del servicio
     * @param accommodationId el identificador único del alojamiento propietario
     * @return un {@link Optional} con el servicio si existe y pertenece al alojamiento, o vacío si no
     */
    Optional<AdditionalService> findByIdAndAccommodation_Id(Long id, Long accommodationId);
}