package com.hostflow.controller;

import com.hostflow.dto.request.AccommodationPaymentMethodRequest;
import com.hostflow.dto.response.AccommodationPaymentMethodResponse;
import com.hostflow.service.AccommodationPaymentMethodService;
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
 * Controlador REST para la gestión de métodos de pago del alojamiento.
 *
 * <p>Permite al propietario registrar, actualizar y eliminar métodos de pago
 * que recibirán los pagos de los huéspedes (transferencias bancarias,
 * billeteras digitales, efectivo, tarjetas, etc.).</p>
 *
 * <p><strong>🆕 Mejora de arquitectura (v2.0):</strong></p>
 * <ul>
 *   <li>Se eliminó la inyección directa de {@code OwnerRepository} y {@code AccommodationRepository}.</li>
 *   <li>El controlador ahora solo extrae el email del usuario autenticado y lo delega
 *       al servicio, manteniendo la estricta separación de responsabilidades.</li>
 *   <li>El servicio retorna directamente DTOs ({@link AccommodationPaymentMethodResponse}),
 *       evitando lógica de mapeo redundante en esta capa y previniendo duplicación de código.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/owners/accommodation/payment-methods")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Métodos de pago del alojamiento", description = "Gestión de múltiples métodos de pago del alojamiento propio")
@Slf4j
public class AccommodationPaymentMethodController {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    /**
     * Servicio que encapsula toda la lógica de negocio, validaciones y consultas
     * relacionadas con los métodos de pago, retornando DTOs listos para serializar.
     */
    private final AccommodationPaymentMethodService service;

    // =========================================================================
    // ENDPOINTS
    // =========================================================================

    /**
     * Lista todos los métodos de pago configurados para el alojamiento del propietario.
     *
     * @param authentication el contexto de autenticación de Spring Security
     * @return lista de DTOs con la configuración de los métodos de pago
     */
    @GetMapping
    @Operation(summary = "Listar mis métodos de pago",
            description = "Devuelve todos los métodos de pago pertenecientes al alojamiento del propietario autenticado, ordenados con el predeterminado primero.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de métodos de pago obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "El owner aún no ha registrado su alojamiento")
    })
    public ResponseEntity<List<AccommodationPaymentMethodResponse>> list(Authentication authentication) {
        String ownerEmail = getOwnerEmailOrThrow(authentication);
        log.debug("📋 Listando métodos de pago para owner email: {}", ownerEmail);

        // ✅ El servicio ya retorna la lista de DTOs mapeada, evitando código repetido en el controlador.
        List<AccommodationPaymentMethodResponse> responses = service.listByEmail(ownerEmail);
        return ResponseEntity.ok(responses);
    }

    /**
     * Registra un nuevo método de pago para el alojamiento.
     *
     * @param request        los datos del método de pago a crear
     * @param authentication el contexto de autenticación de Spring Security
     * @return el DTO del método de pago creado
     */
    @PostMapping
    @Operation(summary = "Registrar un método de pago",
            description = "paymentType y displayName son obligatorios. BANK_TRANSFER requiere bankName, accountHolder, accountType, accountNumber, cbuCvu y alias. DIGITAL_WALLET requiere accountHolder, userIdentifier, walletPhone y walletEmail válido. Si no existe un método predeterminado, el primero se establece como tal.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Método de pago creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o faltan campos obligatorios según el tipo de pago"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "El owner aún no ha registrado su alojamiento")
    })
    public ResponseEntity<AccommodationPaymentMethodResponse> create(
            @Valid @RequestBody AccommodationPaymentMethodRequest request,
            Authentication authentication) {

        String ownerEmail = getOwnerEmailOrThrow(authentication);
        log.info("💳 Creando método de pago para owner email: {}", ownerEmail);

        // ✅ El servicio realiza la creación, validación y mapeo a DTO en una sola operación.
        AccommodationPaymentMethodResponse response = service.createByEmail(ownerEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza parcialmente un método de pago existente.
     *
     * @param methodId       el identificador del método de pago a actualizar
     * @param request        los datos a actualizar (solo campos no nulos)
     * @param authentication el contexto de autenticación de Spring Security
     * @return el DTO del método de pago actualizado
     */
    @PatchMapping("/{methodId}")
    @Operation(summary = "Actualizar parcialmente un método de pago",
            description = "Solo modifica los campos enviados, valida los requisitos del tipo sobre el estado final y únicamente si el método pertenece al alojamiento autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Método de pago actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o solicitud vacía"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "Método de pago no encontrado o no pertenece al alojamiento")
    })
    public ResponseEntity<AccommodationPaymentMethodResponse> update(
            @PathVariable Long methodId,
            @Valid @RequestBody AccommodationPaymentMethodRequest request,
            Authentication authentication) {

        String ownerEmail = getOwnerEmailOrThrow(authentication);
        log.info("✏️ Actualizando método de pago ID: {} para owner email: {}", methodId, ownerEmail);

        // ✅ El servicio aplica el PATCH, valida el estado final y retorna el DTO actualizado.
        AccommodationPaymentMethodResponse response = service.updateByEmail(ownerEmail, methodId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina un método de pago del alojamiento.
     *
     * @param methodId       el identificador del método de pago a eliminar
     * @param authentication el contexto de autenticación de Spring Security
     * @return respuesta HTTP 204 No Content
     */
    @DeleteMapping("/{methodId}")
    @Operation(summary = "Eliminar un método de pago",
            description = "Elimina solo métodos del alojamiento propio. Si se elimina el predeterminado, se elige otro existente automáticamente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Método de pago eliminado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "Método de pago no encontrado o no pertenece al alojamiento")
    })
    public ResponseEntity<Void> delete(
            @PathVariable Long methodId,
            Authentication authentication) {

        String ownerEmail = getOwnerEmailOrThrow(authentication);
        log.info("🗑️ Eliminando método de pago ID: {} para owner email: {}", methodId, ownerEmail);

        service.deleteByEmail(ownerEmail, methodId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Extrae el email del propietario autenticado directamente del contexto de seguridad.
     *
     * <p>Este método centraliza la validación de la autenticación, delegando la
     * resolución del ID del propietario y su alojamiento completamente al servicio.</p>
     *
     * @param authentication el contexto de autenticación de Spring Security
     * @return el email del propietario autenticado
     * @throws org.springframework.security.authentication.AuthenticationCredentialsNotFoundException si no hay autenticación válida
     */
    private String getOwnerEmailOrThrow(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "Se requiere autenticación válida para realizar esta operación.");
        }
        return authentication.getName();
    }
}