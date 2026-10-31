package com.hostflow.repository;

import com.hostflow.enums.RoleType;
import com.hostflow.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link Role}.
 *
 * <p>Proporciona operaciones CRUD y consultas personalizadas
 * para gestionar los roles del sistema.</p>
 *
 * @author HostFlow Team
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Busca un rol por su nombre.
     *
     * @param roleName el nombre del rol a buscar
     * @return un {@link Optional} con el rol encontrado, o vacío si no existe
     */
    Optional<Role> findByRoleName(RoleType roleName);

    /**
     * Verifica si existe un rol con el nombre especificado.
     *
     * @param roleName el nombre del rol a verificar
     * @return {@code true} si el rol existe, {@code false} en caso contrario
     */
    boolean existsByRoleName(RoleType roleName);
}