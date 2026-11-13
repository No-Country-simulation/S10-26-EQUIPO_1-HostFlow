package com.hostflow.repository;

import com.hostflow.enums.ReservationStatus;
import com.hostflow.model.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la gestión de reservas en HostFlow.
 *
 * <p>Proporciona consultas personalizadas optimizadas para:</p>
 * <ul>
 *   <li>Verificar disponibilidad y prevenir reservas solapadas (overlapping/double booking).</li>
 *   <li>Listar reservas por alojamiento, huésped o estado específico.</li>
 *   <li>Buscar reservas de forma única por código de confirmación.</li>
 *   <li>Calcular métricas financieras y de volumen para los Dashboards (Owner y Admin).</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.3.0 (Corregido binding de parámetros en consulta nativa de disponibilidad)
 * @see com.hostflow.model.Reservation
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // =========================================================================
    // 1. CONSULTAS BÁSICAS Y DE BÚSQUEDA
    // =========================================================================

    /**
     * Busca una reserva por su código de confirmación único.
     *
     * @param confirmationCode código de confirmación (ej: "RES-A1B2C3")
     * @return la reserva encontrada, o {@link Optional#empty()} si no existe
     */
    Optional<Reservation> findByConfirmationCode(String confirmationCode);

    /**
     * Bloquea la reserva durante la transición de pago para evitar que dos
     * solicitudes concurrentes envíen dos veces los correos de confirmación.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :reservationId")
    Optional<Reservation> findByIdForUpdate(@Param("reservationId") Long reservationId);

    /**
     * Lista todas las reservas de un alojamiento específico,
     * ordenadas por fecha de check-in de manera descendente (más recientes primero).
     *
     * @param accommodationId ID del alojamiento
     * @return lista de reservas ordenadas
     */
    List<Reservation> findAllByAccommodation_IdOrderByCheckInDesc(Long accommodationId);

    /**
     * Lista todas las reservas de un huésped específico,
     * ordenadas por fecha de check-in de manera descendente.
     *
     * @param guestId ID del huésped
     * @return lista de reservas ordenadas
     */
    List<Reservation> findAllByGuest_IdOrderByCheckInDesc(Long guestId);

    /**
     * Lista las reservas de un alojamiento filtradas por un estado específico.
     *
     * @param accommodationId ID del alojamiento
     * @param status          estado de la reserva (ej: CONFIRMED, CANCELLED)
     * @return lista de reservas filtradas
     */
    List<Reservation> findAllByAccommodation_IdAndStatus(Long accommodationId, ReservationStatus status);


    // =========================================================================
    // 2. VALIDACIÓN DE DISPONIBILIDAD (PREVENCIÓN DE DOBLE RESERVA)
    // =========================================================================

    /**
     * 🛡️ VERIFICACIÓN CRÍTICA: Busca reservas ACTIVAS que se solapen
     * con un rango de fechas específico para un alojamiento.
     *
     * <p><strong>Lógica de solapamiento:</strong> Dos estadías [A, B) y [C, D)
     * se solapan si y solo si: A < D y C < B.</p>
     *
     * @param accommodationId ID del alojamiento a verificar
     * @param newCheckIn      fecha de check-in solicitada
     * @param newCheckOut     fecha de check-out solicitada
     * @return lista de reservas que se solapan (vacía si hay disponibilidad total)
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
            @Param("newCheckIn") LocalDate newCheckIn,
            @Param("newCheckOut") LocalDate newCheckOut);

    /**
     * Verifica de forma optimizada y nativa si existe alguna reserva activa
     * que se solape con el rango de fechas especificado.
     *
     * <p>Se utiliza {@code nativeQuery = true} para garantizar que el motor
     * de base de datos evalúe los parámetros nombrados en el orden exacto,
     * evitando errores de binding de Spring Data JPA.</p>
     *
     * @param accommodationId ID del alojamiento
     * @param newCheckIn      fecha de check-in solicitada
     * @param newCheckOut     fecha de check-out solicitada
     * @return {@code true} si existe al menos una reserva que se solapa (NO disponible)
     */
    @Query(value = """
        SELECT CASE WHEN COUNT(r.id) > 0 THEN true ELSE false END
        FROM reservations r
        WHERE r.accommodation_id = :accommodationId
          AND r.status IN ('PENDING', 'CONFIRMED')
          AND r.check_in_date < :newCheckOut
          AND r.check_out_date > :newCheckIn
    """, nativeQuery = true)
    boolean existsOverlappingReservation(
            @Param("accommodationId") Long accommodationId,
            @Param("newCheckIn") LocalDate newCheckIn,
            @Param("newCheckOut") LocalDate newCheckOut);

    /**
     * Cuenta las reservas activas de un alojamiento en un rango de fechas específico.
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
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);



    @Query("SELECT r FROM Reservation r WHERE r.accommodation.id = :accommodationId AND r.id <> :excludeId AND r.status IN ('PENDING', 'CONFIRMED') AND r.checkIn < :newCheckOut AND r.checkOut > :newCheckIn")
    List<Reservation> findOverlappingReservationsExcludingId(
            @Param("accommodationId") Long accommodationId,
            @Param("newCheckIn") LocalDate newCheckIn,
            @Param("newCheckOut") LocalDate newCheckOut,
            @Param("excludeId") Long excludeId);

    // =========================================================================
    // 3. MÉTRICAS PARA DASHBOARD DEL PROPIETARIO (OWNER)
    // =========================================================================

    /**
     * Cuenta las reservas actualmente activas (CONFIRMED) de un propietario.
     *
     * @param ownerId ID del propietario
     * @return cantidad de reservas confirmadas
     */
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.accommodation.owner.id = :ownerId AND r.status = 'CONFIRMED'")
    long countActiveReservationsByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Cuenta las reservas completadas exitosamente por un propietario.
     *
     * @param ownerId ID del propietario
     * @return cantidad de reservas completadas
     */
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.accommodation.owner.id = :ownerId AND r.status = 'COMPLETED'")
    long countCompletedReservationsByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Calcula los ingresos del último mes para un propietario.
     *
     * <p>Suma el total de las reservas completadas. {@code COMPLETED} solo se asigna
     * tras registrar el pago ({@code PAID}), por lo que el ingreso se considera asegurado.
     * Utiliza {@code COALESCE} para devolver 0 en lugar de null si no hay reservas.</p>
     *
     * @param ownerId      ID del propietario
     * @param startOfMonth fecha de inicio del mes actual (ej: 2026-11-01T00:00:00)
     * @return suma de los montos totales (0 si no hay reservas)
     */
    @Query("""
        SELECT COALESCE(SUM(r.totalAmount), 0) FROM Reservation r
        WHERE r.accommodation.owner.id = :ownerId
          AND r.status = 'COMPLETED'
          AND r.createdAt >= :startOfMonth
    """)
    BigDecimal sumIncomeLastMonthByOwnerId(
            @Param("ownerId") Long ownerId,
            @Param("startOfMonth") LocalDateTime startOfMonth);

    /**
     * Calcula los ingresos acumulados históricos para un propietario.
     *
     * <p>Suma total de los montos de reservas completadas.
     * Utiliza {@code COALESCE} para garantizar que nunca retorne null.</p>
     *
     * @param ownerId ID del propietario
     * @return suma total de los montos de reservas completadas (0 si no hay)
     */
    @Query("""
        SELECT COALESCE(SUM(r.totalAmount), 0) FROM Reservation r
        WHERE r.accommodation.owner.id = :ownerId
          AND r.status = 'COMPLETED'
    """)
    BigDecimal sumAccumulatedIncomeByOwnerId(@Param("ownerId") Long ownerId);


    // =========================================================================
    // 4. MÉTRICAS PARA DASHBOARD DEL ADMINISTRADOR (ADMIN)
    // =========================================================================

    /**
     * Cuenta el total de reservas completadas en toda la plataforma.
     *
     * <p>Utilizado en el dashboard global del Admin para mostrar el volumen
     * de negocios exitosos, excluyendo las reservas canceladas o pendientes.</p>
     *
     * @return cantidad total de reservas con estado COMPLETED
     */
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = 'COMPLETED'")
    long countTotalCompletedReservations();
}