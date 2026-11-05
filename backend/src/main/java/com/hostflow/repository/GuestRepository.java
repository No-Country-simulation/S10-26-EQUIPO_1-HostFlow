package com.hostflow.repository;

import com.hostflow.model.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
     * Busca un huésped por su correo electrónico.
     *
     * <p>Se utiliza principalmente para verificar si un huésped ya está
     * registrado en el sistema antes de intentar crear uno nuevo, o para
     * recuperar sus datos al momento de generar una reserva.</p>
     *
     * @param email el correo electrónico del huésped (normalizado a minúsculas)
     * @return un {@link Optional} con el huésped encontrado, o vacío si no existe
     */
    Optional<Guest> findByEmail(String email);

    /**
     * Verifica si ya existe un huésped registrado con el correo especificado.
     *
     * <p>Este método es más eficiente que {@link #findByEmail} cuando solo
     * se necesita saber si el registro existe (ej. para validaciones de
     * unicidad), ya que ejecuta una consulta {@code EXISTS} o {@code COUNT}
     * en la base de datos en lugar de traer toda la entidad a la memoria.</p>
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