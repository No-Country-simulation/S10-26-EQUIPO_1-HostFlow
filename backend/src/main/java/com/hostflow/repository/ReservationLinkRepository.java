package com.hostflow.repository;

import com.hostflow.model.ReservationLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

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

    /**
     * Busca el enlace asociado a una reserva específica.
     *
     * @param reservationId ID de la reserva
     * @return el enlace encontrado, o vacío si no existe
     */
    Optional<ReservationLink> findByReservation_Id(Long reservationId);

    /**
     * Busca un enlace por token y valida que pertenezca a una reserva específica.
     *
     * @param token         el token del enlace
     * @param reservationId ID de la reserva
     * @return el enlace encontrado, o vacío si no coincide
     */
    Optional<ReservationLink> findByTokenAndReservation_Id(String token, Long reservationId);

    /**
     * Elimina todos los enlaces expirados y no utilizados de la base de datos.
     *
     * <p>Útil para tareas de limpieza programadas que mantienen
     * la base de datos optimizada.</p>
     *
     * @param now fecha y hora actual
     * @return número de enlaces eliminados
     */
    @Modifying
    @Query("DELETE FROM ReservationLink l WHERE l.expiresAt < :now AND l.used = false")
    int deleteExpiredUnusedLinks(@Param("now") LocalDateTime now);

    /**
     * Marca como usados todos los enlaces expirados que no fueron utilizados.
     *
     * @param now fecha y hora actual
     * @return número de enlaces actualizados
     */
    @Modifying
    @Query("""
        UPDATE ReservationLink l
        SET l.used = true, l.usedAt = :now
        WHERE l.expiresAt < :now AND l.used = false
    """)
    int markExpiredLinksAsUsed(@Param("now") LocalDateTime now);
}
