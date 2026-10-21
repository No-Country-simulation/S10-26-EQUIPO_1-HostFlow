package com.hostflow.repository;

import com.hostflow.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link LoginAttempt}.
 *
 * <p>Proporciona operaciones para consultar y gestionar los registros
 * de intentos de inicio de sesión, incluyendo la detección de cuentas
 * bloqueadas y el conteo de intentos fallidos.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * Cuenta los intentos fallidos de login para un email específico
     * dentro de un rango de tiempo.
     *
     * @param email el correo electrónico a consultar
     * @param since la fecha y hora desde la cual contar (ej. hace 1 hora)
     * @return número de intentos fallidos en el período especificado
     */
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.email = :email AND la.success = false AND la.attemptAt >= :since")
    long countFailedAttemptsSince(@Param("email") String email, @Param("since") LocalDateTime since);

    /**
     * Busca el intento de login más reciente que causó un bloqueo para un email.
     *
     * @param email el correo electrónico a consultar
     * @return el último registro de bloqueo, o {@link Optional#empty()} si no hay bloqueo
     */
    @Query("SELECT la FROM LoginAttempt la WHERE la.email = :email AND la.blocked = true ORDER BY la.attemptAt DESC")
    Optional<LoginAttempt> findLatestBlockedAttempt(@Param("email") String email);

    /**
     * Obtiene todos los intentos de login para un email específico,
     * ordenados del más reciente al más antiguo.
     *
     * @param email el correo electrónico a consultar
     * @return lista de intentos de login ordenados cronológicamente
     */
    List<LoginAttempt> findByEmailOrderByAttemptAtDesc(String email);

    /**
     * Obtiene todos los intentos de login para un usuario específico (por ID),
     * ordenados del más reciente al más antiguo.
     *
     * @param ownerId el ID del usuario
     * @return lista de intentos de login ordenados cronológicamente
     */
    List<LoginAttempt> findByOwnerIdOrderByAttemptAtDesc(Long ownerId);

    /**
     * Elimina los intentos de login antiguos (más antiguos que la fecha especificada).
     * Útil para mantener limpia la tabla de auditoría.
     *
     * @param before la fecha y hora límite (se eliminarán registros anteriores a esta fecha)
     * @return número de registros eliminados
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.attemptAt < :before")
    int deleteOldAttempts(@Param("before") LocalDateTime before);

    /**
     * Cuenta el total de intentos fallidos para un email (sin límite de tiempo).
     *
     * @param email el correo electrónico a consultar
     * @return número total de intentos fallidos
     */
    long countByEmailAndSuccessFalse(String email);
}