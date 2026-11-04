package com.hostflow.service;

import com.hostflow.dto.response.OwnerResponse;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Owner;
import com.hostflow.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de dueños de alojamientos.
 *
 * <p>Proporciona operaciones de consulta y gestión sobre los dueños
 * registrados en el sistema HostFlow. Incluye operaciones tanto
 * para dueños individuales como para listados generales
 * (uso administrativo).</p>
 *
 * @author HostFlow Team
 */
@Service
@RequiredArgsConstructor
public class OwnerService {

    /**
     * Repositorio para acceder a los datos de los dueños.
     */
    private final OwnerRepository ownerRepository;

    /**
     * Obtiene la información de un dueño por su identificador.
     *
     * @param id el identificador único del dueño
     * @return los datos del dueño en formato de respuesta
     * @throws ResourceNotFoundException si no se encuentra el dueño
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
     * @return los datos del dueño en formato de respuesta
     * @throws ResourceNotFoundException si no se encuentra el dueño
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
     * <p>Esta operación está restringida al rol {@code ROLE_ADMIN}
     * y se utiliza para la administración general de la plataforma.</p>
     *
     * @return lista de todos los dueños en formato de respuesta
     */
    @Transactional(readOnly = true)
    public List<OwnerResponse> getAllOwners() {
        return ownerRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Activa o desactiva la cuenta de un dueño.
     *
     * <p>Operación administrativa que permite al super usuario
     * habilitar o deshabilitar cuentas sin eliminarlas de la base de datos.</p>
     *
     * @param id      el identificador del dueño
     * @param enabled el nuevo estado de la cuenta
     * @return los datos actualizados del dueño
     * @throws ResourceNotFoundException si no se encuentra el dueño
     */
    @Transactional
    public OwnerResponse toggleOwnerStatus(Long id, Boolean enabled) {
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el dueño con el ID: " + id));

        owner.setEnabled(enabled);
        Owner savedOwner = ownerRepository.save(owner);

        return mapToResponse(savedOwner);
    }

    /**
     * Mapea una entidad {@link Owner} a su DTO de respuesta {@link OwnerResponse}.
     *
     * <p>Este método transforma la entidad JPA en un objeto de transferencia
     * de datos, excluyendo información sensible como la contraseña.</p>
     *
     * @param owner la entidad Owner a mapear
     * @return el DTO OwnerResponse con los datos del dueño
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