package com.hostflow.controller;

import com.hostflow.dto.request.AdditionalServiceRequest;
import com.hostflow.dto.response.AdditionalServiceResponse;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.OwnerRepository;
import com.hostflow.service.AdditionalServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

/**
 * Controlador REST para que el propietario (Owner) gestione los servicios
 * adicionales de su alojamiento.
 *
 * <p>Los servicios adicionales son extras que el alojamiento ofrece a los huéspedes
 * más allá del alojamiento base, tales como:</p>
 * <ul>
 *   <li>Desayuno buffet</li>
 *   <li>Transfer al aeropuerto</li>
 *   <li>Spa / masajes</li>
 *   <li>Estacionamiento</li>
 *   <li>Mascotas</li>
 *   <li>Early check-in / Late check-out</li>
 * </ul>
 *
 * <p><strong>Características clave:</strong></p>
 * <ul>
 *   <li>Los precios se almacenan en la moneda base del alojamiento (ARS).</li>
 *   <li>Al consultarlos, se convierten automáticamente a todas las monedas
 *       habilitadas (USD, EUR) mediante {@code CurrencyConversionService}.</li>
 *   <li>Solo el propietario del alojamiento puede crear, modificar o eliminar
 *       sus propios servicios adicionales.</li>
 * </ul>
 *
 * <p><strong>Seguridad:</strong> Todos los endpoints requieren autenticación
 * y el rol {@code ROLE_OWNER}.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/owners/accommodation/additional-services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Servicios adicionales",
        description = "CRUD de servicios extra con conversión automática a monedas habilitadas")
@Slf4j
public class AdditionalServiceController {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    /**
     * Servicio que encapsula la lógica de negocio de los servicios adicionales.
     * Se utiliza en lugar de acceder directamente al repositorio para mantener
     * la separación de responsabilidades y aplicar reglas de negocio.
     */
    private final AdditionalServiceService serviceService;

    /**
     * Repositorio para obtener el alojamiento del propietario autenticado.
     */
    private final AccommodationRepository accommodationRepository;

    /**
     * Repositorio para obtener el ID del propietario a partir del email del token JWT.
     */
    private final OwnerRepository ownerRepository;

    // =========================================================================
    // LISTAR SERVICIOS ADICIONALES
    // =========================================================================

    @GetMapping
    @Operation(
            summary = "Listar servicios adicionales con precios convertidos",
            description = "Devuelve todos los servicios adicionales del alojamiento del owner, " +
                    "incluyendo sus precios convertidos a todas las monedas habilitadas (ARS, USD, EUR)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de servicios obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "El owner aún no ha registrado su alojamiento")
    })
    public ResponseEntity<List<AdditionalServiceResponse>> list(Authentication auth) {
        Long ownerId = currentOwnerId(auth);
        List<AdditionalServiceResponse> responses = serviceService.listWithConvertedPrices(ownerId);
        return ResponseEntity.ok(responses);
    }

    // =========================================================================
    // CREAR SERVICIO ADICIONAL
    // =========================================================================

    @PostMapping
    @Operation(
            summary = "Crear un servicio adicional",
            description = "Registra un nuevo servicio adicional para el alojamiento del owner. " +
                    "El precio se almacena en la moneda base (ARS) y se convierte automáticamente " +
                    "a las demás monedas habilitadas al consultarlo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Servicio creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (nombre vacío, precio <= 0)"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "El owner aún no ha registrado su alojamiento")
    })
    public ResponseEntity<AdditionalServiceResponse> create(
            @Valid @RequestBody AdditionalServiceRequest request,
            Authentication auth) {

        Long ownerId = currentOwnerId(auth);
        AdditionalServiceResponse created = serviceService.createWithConvertedPrices(ownerId, request);

        log.info("✅ Servicio adicional creado: '{}' ({} ARS) para owner {}",
                request.name(), request.basePrice(), ownerId);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // =========================================================================
    // ACTUALIZAR SERVICIO ADICIONAL (PATCH)
    // =========================================================================

    @PatchMapping("/{serviceId}")
    @Operation(
            summary = "Actualizar parcialmente un servicio adicional",
            description = "Modifica únicamente los campos enviados en la solicitud. " +
                    "Los campos omitidos o null permanecen sin cambios. " +
                    "Solo se pueden modificar servicios del alojamiento propio."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicio actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado o no pertenece al alojamiento")
    })
    public ResponseEntity<AdditionalServiceResponse> update(
            @PathVariable Long serviceId,
            @Valid @RequestBody AdditionalServiceRequest request,
            Authentication auth) {

        Long ownerId = currentOwnerId(auth);
        AdditionalServiceResponse updated = serviceService.updateWithConvertedPrices(ownerId, serviceId, request);

        log.info("✏️ Servicio adicional actualizado: ID={} para owner {}", serviceId, ownerId);

        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // ELIMINAR SERVICIO ADICIONAL
    // =========================================================================

    @DeleteMapping("/{serviceId}")
    @Operation(
            summary = "Eliminar un servicio adicional",
            description = "Elimina permanentemente un servicio adicional del alojamiento. " +
                    "Solo se pueden eliminar servicios del alojamiento propio."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Servicio eliminado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado o no pertenece al alojamiento")
    })
    public ResponseEntity<Void> delete(
            @PathVariable Long serviceId,
            Authentication auth) {

        Long ownerId = currentOwnerId(auth);
        serviceService.delete(ownerId, serviceId);

        log.info("🗑️ Servicio adicional eliminado: ID={} para owner {}", serviceId, ownerId);

        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Obtiene el alojamiento asociado al propietario autenticado.
     *
     * <p>Valida que el propietario haya registrado previamente los datos
     * generales de su alojamiento antes de permitir operaciones sobre
     * servicios adicionales.</p>
     *
     * @param auth el contexto de autenticación de Spring Security
     * @return la entidad Accommodation del propietario
     * @throws ResourceNotFoundException si el propietario no tiene alojamiento registrado
     */
    private Accommodation getAccommodation(Authentication auth) {
        Long ownerId = currentOwnerId(auth);
        return accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registra primero los datos generales del alojamiento antes de gestionar servicios adicionales."));
    }

    /**
     * Obtiene el ID del propietario actualmente autenticado a partir del token JWT.
     *
     * <p>El email del usuario se extrae del token JWT (campo "sub") y se busca
     * en la base de datos para obtener su ID.</p>
     *
     * @param auth el contexto de autenticación de Spring Security
     * @return el ID del propietario autenticado
     * @throws ResourceNotFoundException si no se encuentra el propietario
     */
    private Long currentOwnerId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "Se requiere autenticación.");
        }
        Owner owner = ownerRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el propietario autenticado."));
        return owner.getId();
    }
}