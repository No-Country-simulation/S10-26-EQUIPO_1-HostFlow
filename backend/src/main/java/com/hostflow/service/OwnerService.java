package com.hostflow.service;

import com.hostflow.dto.response.OwnerResponse;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Owner;
import com.hostflow.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de dueños de alojamientos en HostFlow.
 *
 * <p>Proporciona operaciones de consulta y gestión sobre los dueños
 * registrados en el sistema. Incluye operaciones tanto para dueños
 * individuales como para listados generales de uso administrativo.</p>
 *
 * <p><strong>Responsabilidades principales:</strong></p>
 * <ul>
 *   <li>Consulta de información de dueños por ID o correo electrónico.</li>
 *   <li>Listado completo de dueños (restringido a administradores).</li>
 *   <li>Gestión del estado de la cuenta (activación/desactivación) con
 *       registro de auditoría de la fecha de deshabilitación.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerService {

    /**
     * Repositorio para acceder a los datos de persistencia de la entidad {@link Owner}.
     */
    private final OwnerRepository ownerRepository;

    // =========================================================================
    // CONSULTAS DE USUARIOS
    // =========================================================================

    /**
     * Obtiene la información de un dueño por su identificador único.
     *
     * @param id el identificador único del dueño
     * @return los datos del dueño en formato de respuesta (DTO)
     * @throws ResourceNotFoundException si no se encuentra el dueño con el ID proporcionado
     */
    @Transactional(readOnly = true)
    public OwnerResponse getOwnerById(Long id) {
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el dueño con el ID: " + id));
        return mapToResponse(owner);
    }

    /**
     * Obtiene la información de un dueño por su correo electrónico.
     *
     * @param email el correo electrónico del dueño
     * @return los datos del dueño en formato de respuesta (DTO)
     * @throws ResourceNotFoundException si no se encuentra el dueño con el correo proporcionado
     */
    @Transactional(readOnly = true)
    public OwnerResponse getOwnerByEmail(String email) {
        Owner owner = ownerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el dueño con el correo: " + email));
        return mapToResponse(owner);
    }

    /**
     * Obtiene la lista de todos los dueños registrados en el sistema.
     *
     * <p>⚠️ <strong>Nota de Seguridad:</strong> Esta operación está restringida
     * al rol {@code ROLE_ADMIN} a nivel de controlador y se utiliza exclusivamente
     * para la administración general y auditoría de la plataforma.</p>
     *
     * @return lista de todos los dueños en formato de respuesta (DTO)
     */
    @Transactional(readOnly = true)
    public List<OwnerResponse> getAllOwners() {
        return ownerRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // GESTIÓN ADMINISTRATIVA DE CUENTAS
    // =========================================================================

    /**
     * Activa o desactiva la cuenta de un dueño de alojamiento.
     *
     * <p>Operación administrativa que permite al super usuario habilitar o
     * deshabilitar cuentas sin eliminarlas físicamente de la base de datos
     * (soft delete). Una cuenta desactivada no podrá iniciar sesión.</p>
     *
     * <p><strong>Lógica de Auditoría:</strong></p>
     * <ul>
     *   <li>Si {@code enabled = false}: Se registra la fecha y hora actual en
     *       el campo {@code disabledAt} para informar al usuario cuándo fue
     *       deshabilitado.</li>
     *   <li>Si {@code enabled = true}: Se limpia el campo {@code disabledAt}
     *       (se establece en {@code null}) indicando que la cuenta está activa.</li>
     * </ul>
     *
     * @param id      el identificador único del dueño
     * @param enabled el nuevo estado de la cuenta ({@code true} = activa, {@code false} = desactivada)
     * @return los datos actualizados del dueño en formato de respuesta (DTO)
     * @throws ResourceNotFoundException si no se encuentra el dueño con el ID proporcionado
     */
    @Transactional
    public OwnerResponse toggleOwnerStatus(Long id, Boolean enabled) {
        // 1. Buscar el dueño o lanzar excepción si no existe
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el dueño con el ID: " + id));

        // 2. Actualizar el estado de activación
        owner.setEnabled(enabled);

        // 3. 🆕 Gestionar el registro de auditoría de la fecha de deshabilitación
        if (!enabled) {
            // La cuenta se desactiva: registrar el momento exacto
            owner.setDisabledAt(LocalDateTime.now());
            log.warn("⚠️ Cuenta deshabilitada por administrador - Owner ID: {}, Email: {}", id, owner.getEmail());
        } else {
            // La cuenta se reactiva: limpiar el registro de deshabilitación
            owner.setDisabledAt(null);
            log.info("✅ Cuenta reactivada por administrador - Owner ID: {}, Email: {}", id, owner.getEmail());
        }

        // 4. Persistir los cambios en la base de datos
        Owner savedOwner = ownerRepository.save(owner);

        // 5. Retornar la representación DTO actualizada
        return mapToResponse(savedOwner);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Mapea una entidad {@link Owner} a su DTO de respuesta {@link OwnerResponse}.
     *
     * <p>Este método transforma la entidad JPA en un objeto de transferencia
     * de datos, exponiendo solo la información necesaria para el cliente y
     * excluyendo datos sensibles como la contraseña encriptada.</p>
     *
     * @param owner la entidad Owner a mapear
     * @return el DTO OwnerResponse con los datos seguros del dueño
     */
    private OwnerResponse mapToResponse(Owner owner) {
        return OwnerResponse.builder()
                .id(owner.getId())
                .fullName(owner.getFullName())
                .accommodationName(owner.getAccommodationName())
                .email(owner.getEmail())
                .enabled(owner.getEnabled())
                .roles(owner.getRoles().stream()
                        .map(role -> role.getRoleName().name())
                        .collect(Collectors.toList()))
                .createdAt(owner.getCreatedAt())
                .updatedAt(owner.getUpdatedAt())
                .build();
    }
}