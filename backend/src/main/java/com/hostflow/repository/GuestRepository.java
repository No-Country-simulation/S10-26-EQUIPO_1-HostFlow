package com.hostflow.repository;

import com.hostflow.model.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repositorio JPA para la gestión de la entidad {@link Guest}.
 *
 * <p>Proporciona operaciones de persistencia y consultas personalizadas
 * para administrar los huéspedes (clientes) que realizan reservas en la plataforma.</p>
 *
 * <p><strong>Características:</strong></p>
 * <ul>
 *   <li>Extiende {@link JpaRepository} para heredar operaciones CRUD básicas.</li>
 *   <li>Métodos de consulta basados en la convención de nombres de Spring Data JPA.</li>
 *   <li>Soporte para búsqueda y validación de unicidad por correo electrónico.</li>
 *   <li>🆕 Soporte para búsqueda y validación de unicidad por número de documento.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 * @see Guest
 * @see com.hostflow.service.GuestService
 */
@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {

    /**
     * Busca el registro de huésped más reciente asociado a un correo.
     * El correo puede repetirse entre diferentes reservas, por lo que esta
     * consulta no debe utilizarse para identificar de forma única a una persona.
     *
     * @param email el correo electrónico del huésped (normalizado a minúsculas)
     * @return un {@link Optional} con el huésped encontrado, o vacío si no existe
     */
    Optional<Guest> findFirstByEmailOrderByIdDesc(String email);

    /**
     * Verifica si existe al menos un registro asociado al correo.
     * No debe utilizarse como validación de unicidad.
     *
     * @param email el correo electrónico a verificar
     * @return {@code true} si ya existe un huésped con ese correo, {@code false} en caso contrario
     */
    boolean existsByEmail(String email);

    /**
     * 🆕 Busca un huésped por su número de documento de identidad.
     *
     * <p>Se utiliza para validar que no existan duplicados de documentos
     * en el sistema, cumpliendo con las regulaciones de identificación
     * de huéspedes y previniendo fraudes o registros accidentales múltiples.</p>
     *
     * @param documentNumber el número de documento (DNI, CC, Pasaporte, etc.)
     * @return un {@link Optional} con el huésped encontrado, o vacío si no existe
     */
    Optional<Guest> findByDocumentNumber(String documentNumber);

    /** Bloquea el perfil durante una actualización para evitar escrituras concurrentes perdidas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Guest g where g.documentNumber = :documentNumber")
    Optional<Guest> findByDocumentNumberForUpdate(@Param("documentNumber") String documentNumber);

    /**
     * 🆕 Verifica si ya existe un huésped registrado con el número de documento especificado.
     *
     * <p>Este método es altamente eficiente para validaciones de unicidad
     * antes de intentar persistir un nuevo huésped, evitando excepciones
     * de restricción única (ConstraintViolationException) en la base de datos.</p>
     *
     * @param documentNumber el número de documento a verificar
     * @return {@code true} si ya existe un huésped con ese documento, {@code false} en caso contrario
     */
    boolean existsByDocumentNumber(String documentNumber);
}