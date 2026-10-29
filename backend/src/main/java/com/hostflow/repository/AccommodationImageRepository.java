package com.hostflow.repository;

import com.hostflow.model.AccommodationImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AccommodationImageRepository extends JpaRepository<AccommodationImage, Long> {
    List<AccommodationImage> findAllByAccommodation_IdOrderByCreatedAtAscIdAsc(Long accommodationId);
    Optional<AccommodationImage> findByIdAndAccommodation_Id(Long id, Long accommodationId);
    long countByAccommodation_Id(Long accommodationId);

    /** Elimina únicamente si la imagen pertenece al alojamiento indicado. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from AccommodationImage image where image.id = :imageId and image.accommodation.id = :accommodationId")
    int deleteImageFromAccommodation(@Param("imageId") Long imageId,
                                     @Param("accommodationId") Long accommodationId);
}
