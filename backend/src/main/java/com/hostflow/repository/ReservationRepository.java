package com.hostflow.repository;

import com.hostflow.enums.ReservationStatus;
import com.hostflow.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la gestión de reservas.
 *
 * <p>Proporciona consultas personalizadas para verificar disponibilidad,
 * listar reservas por alojamiento/huésped y buscar por código de confirmación.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Busca una reserva por su código de confirmación único.
     *
     * @param confirmationCode código de confirmación (formato: RES-XXXXXX)
     * @return la reserva encontrada, o vacío si no existe
     */
    Optional<Reservation> findByConfirmationCode(String confirmationCode);

    /**
     * Lista todas las reservas de un alojamiento específico,
     * ordenadas por fecha de check-in descendente.
     *
     * @param accommodationId ID del alojamiento
     * @return lista de reservas ordenadas
     */
    List<Reservation> findAllByAccommodation_IdOrderByCheckInDesc(Long accommodationId);

    /**
     * Lista todas las reservas de un huésped específico.
     *
     * @param guestId ID del huésped
     * @return lista de reservas ordenadas por check-in descendente
     */
    List<Reservation> findAllByGuest_IdOrderByCheckInDesc(Long guestId);

    /**
     * Lista las reservas de un alojamiento con un estado específico.
     *
     * @param accommodationId ID del alojamiento
     * @param status          estado de la reserva
     * @return lista de reservas filtradas
     */
    List<Reservation> findAllByAccommodation_IdAndStatus(
            Long accommodationId, ReservationStatus status);

    /**
     * 🆕 VERIFICACIÓN CRÍTICA: Busca reservas ACTIVAS que se superpongan
     * con un rango de fechas específico para un alojamiento.
     *
     * <p>Esta consulta es fundamental para garantizar que no se puedan
     * hacer reservas overlapping (doble booking) en el mismo alojamiento.</p>
     *
     * <p>Una reserva se considera que se superpone si:</p>
     * <ul>
     *   <li>Su check-in es antes del nuevo check-out</li>
     *   <li>Su check-out es después del nuevo check-in</li>
     *   <li>Su estado es PENDING o CONFIRMED (activas)</li>
     * </ul>
     *
     * @param accommodationId ID del alojamiento a verificar
     * @param newCheckIn      fecha de check-in solicitada
     * @param newCheckOut     fecha de check-out solicitada
     * @return lista de reservas que se superponen (vacía si hay disponibilidad)
     */
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

    /**
     * Verifica si existe alguna reserva activa que se superponga
     * con el rango de fechas especificado.
     *
     * @param accommodationId ID del alojamiento
     * @param newCheckIn      fecha de check-in
     * @param newCheckOut     fecha de check-out
     * @return {@code true} si existe al menos una reserva que se superpone
     */
    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM Reservation r
        WHERE r.accommodation.id = :accommodationId
          AND r.status IN ('PENDING', 'CONFIRMED')
          AND r.checkIn < :newCheckOut
          AND r.checkOut > :newCheckIn
    """)
    boolean existsOverlappingReservation(
            @Param("accommodationId") Long accommodationId,
            @Param("newCheckIn") LocalDateTime newCheckIn,
            @Param("newCheckOut") LocalDateTime newCheckOut);

    /**
     * Cuenta las reservas activas de un alojamiento en un rango de fechas.
     *
     * @param accommodationId ID del alojamiento
     * @param from            fecha de inicio del rango
     * @param to              fecha de fin del rango
     * @return número de reservas activas en el período
     */
    @Query("""
        SELECT COUNT(r) FROM Reservation r
        WHERE r.accommodation.id = :accommodationId
          AND r.status IN ('PENDING', 'CONFIRMED')
          AND r.checkIn < :to
          AND r.checkOut > :from
    """)
    long countActiveReservationsInRange(
            @Param("accommodationId") Long accommodationId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}