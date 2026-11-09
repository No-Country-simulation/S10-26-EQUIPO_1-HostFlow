package com.hostflow.repository;

import com.hostflow.enums.ReservationStatus;
import com.hostflow.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // =========================================================================
    // 1. CONSULTAS BÁSICAS Y DE BÚSQUEDA
    // =========================================================================

    Optional<Reservation> findByConfirmationCode(String confirmationCode);

    // ✅ Usa 'CheckInDesc' porque el campo en la entidad se llama 'checkIn'
    List<Reservation> findAllByAccommodation_IdOrderByCheckInDesc(Long accommodationId);

    List<Reservation> findAllByGuest_IdOrderByCheckInDesc(Long guestId);

    List<Reservation> findAllByAccommodation_IdAndStatus(Long accommodationId, ReservationStatus status);

    // =========================================================================
    // 2. VALIDACIÓN DE DISPONIBILIDAD
    // =========================================================================

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.accommodation.id = :accommodationId
          AND r.status IN ('PENDING', 'CONFIRMED')
          AND r.checkIn < :newCheckOut
          AND r.checkOut > :newCheckIn
    """)
    List<Reservation> findOverlappingReservations(
            @Param("accommodationId") Long accommodationId,
            @Param("newCheckIn") LocalDateTime newCheckIn,
            @Param("newCheckOut") LocalDateTime newCheckOut);

    @Query(value = """
        SELECT CASE WHEN COUNT(r.id) > 0 THEN true ELSE false END
        FROM reservations r
        WHERE r.accommodation_id = :accommodationId
          AND r.status IN ('PENDING', 'CONFIRMED')
          AND r.check_in < :newCheckOut
          AND r.check_out > :newCheckIn
    """, nativeQuery = true)
    boolean existsOverlappingReservation(
            @Param("accommodationId") Long accommodationId,
            @Param("newCheckIn") LocalDateTime newCheckIn,
            @Param("newCheckOut") LocalDateTime newCheckOut);

    // =========================================================================
    // 3. MÉTRICAS PARA DASHBOARD
    // =========================================================================

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.accommodation.owner.id = :ownerId AND r.status = 'CONFIRMED'")
    long countActiveReservationsByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.accommodation.owner.id = :ownerId AND r.status = 'COMPLETED'")
    long countCompletedReservationsByOwnerId(@Param("ownerId") Long ownerId);

    // ✅ Usa 'totalAmount' y 'checkOut' porque así se llaman los campos en la entidad
    @Query("""
        SELECT COALESCE(SUM(r.totalAmount), 0) FROM Reservation r
        WHERE r.accommodation.owner.id = :ownerId
          AND r.status = 'COMPLETED'
          AND r.checkOut >= :startOfMonth
    """)
    BigDecimal sumIncomeLastMonthByOwnerId(
            @Param("ownerId") Long ownerId,
            @Param("startOfMonth") LocalDateTime startOfMonth);

    // ✅ Usa 'totalAmount'
    @Query("""
        SELECT COALESCE(SUM(r.totalAmount), 0) FROM Reservation r
        WHERE r.accommodation.owner.id = :ownerId
          AND r.status = 'COMPLETED'
    """)
    BigDecimal sumAccumulatedIncomeByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = 'COMPLETED'")
    long countTotalCompletedReservations();
}