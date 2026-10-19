package com.hostflow.repository;

import com.hostflow.model.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link Owner}.
 *
 * <p>Proporciona operaciones CRUD y consultas personalizadas
 * para gestionar los dueños de alojamientos registrados en HostFlow.</p>
 *
 * @author HostFlow Team
 */
@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {

    /**
     * Busca un dueño por su correo electrónico.
     * Se utiliza principalmente para el proceso de autenticación.
     *
     * @param email el correo electrónico del dueño
     * @return un {@link Optional} con el dueño encontrado, o vacío si no existe
     */
    Optional<Owner> findByEmail(String email);

    /**
     * Verifica si ya existe un dueño registrado con el correo especificado.
     * Se utiliza para validar la unicidad durante el registro.
     *
     * @param email el correo electrónico a verificar
     * @return {@code true} si ya existe un dueño con ese correo, {@code false} en caso contrario
     */
    boolean existsByEmail(String email);
}