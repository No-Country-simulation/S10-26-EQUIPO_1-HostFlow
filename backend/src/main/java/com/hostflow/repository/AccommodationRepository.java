package com.hostflow.repository;

import com.hostflow.model.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {
    @EntityGraph(attributePaths = "images")
    Optional<Accommodation> findByOwner_Id(Long ownerId);
    boolean existsByOwner_Id(Long ownerId);
}
