package com.hostflow.repository;

import com.hostflow.model.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {
    @EntityGraph(attributePaths = "images")
    Optional<Accommodation> findByOwner_Id(Long ownerId);
    /** Serializa las cargas de galería del mismo alojamiento para respetar el límite bajo concurrencia. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Accommodation a where a.owner.id = :ownerId")
    Optional<Accommodation> findByOwner_IdForUpdate(@Param("ownerId") Long ownerId);

    boolean existsByOwner_Id(Long ownerId);
}
