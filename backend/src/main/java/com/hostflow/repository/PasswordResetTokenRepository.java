package com.hostflow.repository;

import com.hostflow.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link PasswordResetToken}.
 *
 * <p>Proporciona operaciones CRUD y consultas personalizadas
 * para gestionar los tokens de recuperación de contraseña.</p>
 *
 * @author HostFlow Team
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Busca un token de recuperación por su código y correo electrónico.
     *
     * <p>Se utiliza para validar que el token ingresado por el usuario
     * corresponde al correo electrónico proporcionado.</p>
     *
     * @param token el código de recuperación de 6 dígitos
     * @param email el correo electrónico del usuario
     * @return un {@link Optional} con el token encontrado, o vacío si no existe
     */
    Optional<PasswordResetToken> findByTokenAndEmail(String token, String email);

    /**
     * Busca un token de recuperación por su código.
     *
     * @param token el código de recuperación de 6 dígitos
     * @return un {@link Optional} con el token encontrado, o vacío si no existe
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Elimina todos los tokens expirados de la base de datos.
     *
     * <p>Se utiliza para limpiar tokens que ya no son válidos
     * y mantener la base de datos optimizada.</p>
     *
     * @param now la fecha y hora actual para comparar con la expiración
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Busca tokens pendientes (no usados) para un correo electrónico específico.
     *
     * <p>Se utiliza para invalidar tokens anteriores cuando el usuario
     * solicita un nuevo código de recuperación.</p>
     *
     * @param email el correo electrónico del usuario
     * @return lista de tokens pendientes para ese correo
     */
    java.util.List<PasswordResetToken> findByEmailAndUsedFalse(String email);
}