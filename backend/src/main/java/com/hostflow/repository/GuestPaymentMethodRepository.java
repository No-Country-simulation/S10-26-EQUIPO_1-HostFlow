package com.hostflow.repository;

import com.hostflow.model.GuestPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la gestión de métodos de pago de huéspedes.
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Repository
public interface GuestPaymentMethodRepository extends JpaRepository<GuestPaymentMethod, Long> {

    /**
     * Lista todos los métodos de pago de un huésped,
     * mostrando primero el predeterminado.
     *
     * @param guestId ID del huésped
     * @return lista de métodos de pago ordenados
     */
    List<GuestPaymentMethod> findAllByGuest_IdOrderByDefaultMethodDescCreatedAtAsc(Long guestId);

    /**
     * Busca el método de pago predeterminado de un huésped.
     *
     * @param guestId ID del huésped
     * @return el método predeterminado, o vacío si no existe
     */
    Optional<GuestPaymentMethod> findByGuest_IdAndDefaultMethodTrue(Long guestId);

    /**
     * Verifica si un huésped tiene al menos un método de pago activo.
     *
     * @param guestId ID del huésped
     * @return {@code true} si existe al menos un método activo
     */
    boolean existsByGuest_IdAndActiveTrue(Long guestId);

    /**
     * Verifica si existe un método predeterminado para un huésped.
     *
     * @param guestId ID del huésped
     * @return {@code true} si existe un método predeterminado
     */
    boolean existsByGuest_IdAndDefaultMethodTrue(Long guestId);

    /**
     * Limpia el flag de predeterminado de todos los métodos de un huésped.
     *
     * @param guestId ID del huésped
     * @return número de filas actualizadas
     */
    @Modifying
    @Query("UPDATE GuestPaymentMethod p SET p.defaultMethod = false WHERE p.guest.id = :guestId AND p.defaultMethod = true")
    int clearDefaultForGuest(@Param("guestId") Long guestId);
}