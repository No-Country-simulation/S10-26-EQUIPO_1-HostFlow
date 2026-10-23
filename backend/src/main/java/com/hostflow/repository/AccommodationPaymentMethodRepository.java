package com.hostflow.repository;

import com.hostflow.model.AccommodationPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AccommodationPaymentMethodRepository extends JpaRepository<AccommodationPaymentMethod, Long> {
    List<AccommodationPaymentMethod> findAllByAccommodation_IdOrderByDefaultMethodDescCreatedAtAscIdAsc(Long accommodationId);
    Optional<AccommodationPaymentMethod> findByIdAndAccommodation_Id(Long id, Long accommodationId);
    boolean existsByAccommodation_IdAndDefaultMethodTrue(Long accommodationId);

    @Modifying
    @Query("update AccommodationPaymentMethod p set p.defaultMethod = false where p.accommodation.id = :accommodationId and p.defaultMethod = true")
    int clearDefaultForAccommodation(@Param("accommodationId") Long accommodationId);
}
