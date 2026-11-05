package com.hostflow.controller;

import com.hostflow.dto.response.OwnerResponse;
import com.hostflow.service.OwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión administrativa de dueños de alojamientos en HostFlow.
 *
 * <p>Expone los endpoints protegidos para consultar y gestionar la información
 * de los dueños de alojamiento. Todos los endpoints de este controlador requieren
 * autenticación mediante token JWT y están restringidos EXCLUSIVAMENTE al rol de
 * Administrador ({@code ROLE_ADMIN}).</p>
 *
 * <p>⚠️ <strong>Nota de Seguridad:</strong> Los usuarios con rol {@code ROLE_OWNER}
 * no tienen permisos para acceder a ninguno de los endpoints de este controlador.
 * Cualquier intento de acceso resultará en un error HTTP 403 (Forbidden) con el
 * mensaje: "No tienes permisos para realizar esta operación".</p>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/owners")
@RequiredArgsConstructor
@Tag(name = "Gestión de Dueños (Admin)", description = "Endpoints exclusivos para la administración y auditoría de dueños de alojamientos")
@SecurityRequirement(name = "Bearer Authentication")
public class OwnerController {

    /**
     * Servicio de gestión de dueños inyectado por Lombok ({@code @RequiredArgsConstructor}).
     */
    private final OwnerService ownerService;

    /**
     * Obtiene la información de un dueño por su correo electrónico.
     *
     * <p>⚠️ Restringido exclusivamente al rol {@code ROLE_ADMIN}.</p>
     *
     * @param email el correo electrónico del dueño a consultar
     * @return respuesta HTTP 200 con los datos del dueño
     */
    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')") // 🔒 CAMBIO CLAVE: Solo ADMIN puede consultar por email
    @Operation(
            summary = "Obtener dueño por email (Solo Admin)",
            description = "Obtiene la información detallada de un dueño de alojamiento buscando por su correo electrónico. Acceso exclusivo para administradores."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dueño encontrado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Dueño no encontrado con ese email"),
            @ApiResponse(responseCode = "401", description = "No autenticado (falta o invalidez del token)"),
            @ApiResponse(responseCode = "403", description = "Sin permisos suficientes para acceder (No eres administrador)")
    })
    public ResponseEntity<OwnerResponse> getOwnerByEmail(@PathVariable String email) {
        OwnerResponse response = ownerService.getOwnerByEmail(email);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene la información de un dueño por su identificador único.
     *
     * <p>⚠️ Restringido exclusivamente al rol {@code ROLE_ADMIN}.</p>
     *
     * @param id el identificador único (ID) del dueño
     * @return respuesta HTTP 200 con los datos del dueño
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // 🔒 CAMBIO CLAVE: Solo ADMIN puede consultar por ID
    @Operation(
            summary = "Obtener dueño por ID (Solo Admin)",
            description = "Obtiene la información detallada de un dueño de alojamiento buscando por su identificador único. Acceso exclusivo para administradores."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dueño encontrado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Dueño no encontrado con ese ID"),
            @ApiResponse(responseCode = "401", description = "No autenticado (falta o invalidez del token)"),
            @ApiResponse(responseCode = "403", description = "Sin permisos suficientes para acceder (No eres administrador)")
    })
    public ResponseEntity<OwnerResponse> getOwnerById(@PathVariable Long id) {
        OwnerResponse response = ownerService.getOwnerById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene la lista completa de todos los dueños registrados en el sistema.
     *
     * <p>⚠️ Este endpoint está restringido exclusivamente al rol {@code ROLE_ADMIN}
     * y se utiliza para la administración y auditoría general de la plataforma.</p>
     *
     * @return respuesta HTTP 200 con la lista de todos los dueños
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Listar todos los dueños (Solo Admin)",
            description = "Obtiene la lista completa de dueños de alojamiento registrados. " +
                    "Este endpoint está restringido exclusivamente al rol ADMINISTRADOR."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de dueños obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado (falta o invalidez del token)"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<List<OwnerResponse>> getAllOwners() {
        List<OwnerResponse> responses = ownerService.getAllOwners();
        return ResponseEntity.ok(responses);
    }

    /**
     * Activa o desactiva la cuenta de un dueño de alojamiento.
     *
     * <p>Operación administrativa que permite al super usuario habilitar o deshabilitar
     * cuentas. Una cuenta desactivada ({@code enabled = false}) no podrá iniciar sesión
     * en el sistema, aunque sus datos se mantienen en la base de datos.</p>
     *
     * <p>⚠️ Restringido exclusivamente al rol {@code ROLE_ADMIN}.</p>
     *
     * @param id      el identificador único del dueño
     * @param enabled el nuevo estado de la cuenta ({@code true} = activa, {@code false} = desactivada)
     * @return respuesta HTTP 200 con los datos actualizados del dueño
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Activar/Desactivar cuenta de dueño (Solo Admin)",
            description = "Cambia el estado de activación de la cuenta de un dueño. " +
                    "Una cuenta desactivada no podrá iniciar sesión. " +
                    "Restringido exclusivamente al rol ADMINISTRADOR."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado de la cuenta actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Dueño no encontrado con ese ID"),
            @ApiResponse(responseCode = "401", description = "No autenticado (falta o invalidez del token)"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<OwnerResponse> toggleOwnerStatus(
            @PathVariable Long id,
            @RequestParam Boolean enabled) {

        OwnerResponse response = ownerService.toggleOwnerStatus(id, enabled);
        return ResponseEntity.ok(response);
    }
}