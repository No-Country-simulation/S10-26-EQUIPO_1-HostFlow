package com.hostflow.repository;

import com.hostflow.model.ReservationLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

/**
 * Repositorio JPA para la gestión de enlaces de confirmación de reservas.
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Repository
public interface ReservationLinkRepository extends JpaRepository<ReservationLink, Long> {

    /**
     * Busca un enlace por su token único.
     *
     * @param token el token UUID del enlace
     * @return el enlace encontrado, o vacío si no existe
     */
    Optional<ReservationLink> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM ReservationLink l WHERE l.token = :token")
    Optional<ReservationLink> findByTokenForUpdate(@Param("token") String token);

    /**
     * Busca el enlace asociado a una reserva específica.
     *
     * @param reservationId ID de la reserva
     * @return el enlace encontrado, o vacío si no existe
     */
    List<ReservationLink> findAllByReservation_Id(Long reservationId);

    Optional<ReservationLink> findFirstByReservation_IdOrderByCreatedAtDescIdDesc(Long reservationId);

    /**
     * Busca un enlace por token y valida que pertenezca a una reserva específica.
     *
     * @param token         el token del enlace
     * @param reservationId ID de la reserva
     * @return el enlace encontrado, o vacío si no coincide
     */
    Optional<ReservationLink> findByTokenAndReservation_Id(String token, Long reservationId);

}
