package com.hostflow.repository;

import com.hostflow.model.AdditionalServiceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link AdditionalServiceImage}.
 *
 * <p>Proporciona operaciones de consulta y eliminación de imágenes
 * asociadas a servicios adicionales.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Repository
public interface AdditionalServiceImageRepository
        extends JpaRepository<AdditionalServiceImage, Long> {

    /**
     * Lista todas las imágenes de un servicio adicional,
     * ordenadas por orderIndex ascendente.
     *
     * @param serviceId ID del servicio adicional
     * @return lista de imágenes ordenadas
     */
    List<AdditionalServiceImage> findAllByAdditionalService_IdOrderByOrderIndexAsc(Long serviceId);

    /**
     * Cuenta la cantidad de imágenes de un servicio adicional.
     *
     * <p>Se utiliza para validar el límite máximo de 3 imágenes.</p>
     *
     * @param serviceId ID del servicio adicional
     * @return cantidad de imágenes existentes
     */
    long countByAdditionalService_Id(Long serviceId);

    /**
     * Busca una imagen por su ID, validando que pertenezca al servicio indicado.
     *
     * @param imageId   ID de la imagen
     * @param serviceId ID del servicio adicional
     * @return la imagen encontrada, o {@link Optional#empty()} si no existe
     */
    Optional<AdditionalServiceImage> findByIdAndAdditionalService_Id(
            Long imageId, Long serviceId);

    /**
     * Elimina una imagen únicamente si pertenece al servicio indicado.
     *
     * <p>Se utiliza para evitar que un propietario elimine imágenes
     * de servicios de otros propietarios.</p>
     *
     * @param imageId   ID de la imagen
     * @param serviceId ID del servicio adicional
     * @return número de filas eliminadas (0 o 1)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AdditionalServiceImage img WHERE img.id = :imageId AND img.additionalService.id = :serviceId")
    int deleteByImageIdAndServiceId(@Param("imageId") Long imageId,
                                    @Param("serviceId") Long serviceId);
}