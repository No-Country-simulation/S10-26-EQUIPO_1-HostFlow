package com.hostflow.repository;

import com.hostflow.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link EmailVerificationToken}.
 *
 * <p>Proporciona operaciones CRUD y consultas personalizadas
 * para gestionar los tokens de verificación de correo electrónico.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * Busca un token de verificación por su código.
     *
     * @param token el token UUID de verificación
     * @return un {@link Optional} con el token encontrado, o vacío si no existe
     */
    Optional<EmailVerificationToken> findByToken(String token);

    /**
     * Busca un token de verificación por su código y correo electrónico.
     *
     * <p>Se utiliza para validar que el token ingresado corresponde
     * al correo electrónico proporcionado.</p>
     *
     * @param token el token UUID de verificación
     * @param email el correo electrónico del usuario
     * @return un {@link Optional} con el token encontrado, o vacío si no existe
     */
    Optional<EmailVerificationToken> findByTokenAndEmail(String token, String email);

    /**
     * Busca tokens pendientes (no usados) para un correo electrónico específico.
     *
     * <p>Se utiliza para invalidar tokens anteriores cuando el usuario
     * solicita un nuevo enlace de verificación.</p>
     *
     * @param email el correo electrónico del usuario
     * @return lista de tokens pendientes para ese correo
     */
    java.util.List<EmailVerificationToken> findByEmailAndUsedFalse(String email);

    /**
     * Elimina todos los tokens expirados de la base de datos.
     *
     * <p>Se utiliza para limpiar tokens que ya no son válidos
     * y mantener la base de datos optimizada.</p>
     *
     * @param now la fecha y hora actual para comparar con la expiración
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}