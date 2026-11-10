package com.hostflow.controller;

import com.hostflow.dto.request.CreateGuestRequest;
import com.hostflow.dto.request.GuestPaymentMethodRequest;
import com.hostflow.dto.response.GuestPaymentMethodResponse;
import com.hostflow.dto.response.GuestResponse;
import com.hostflow.model.Guest;
import com.hostflow.service.GuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST PÚBLICO para la gestión de huéspedes y sus métodos de pago.
 *
 * <p>Estos endpoints permiten a los huéspedes:</p>
 * <ul>
 *   <li>Registrarse en el sistema (crear cuenta de huésped con datos personales y documento).</li>
 *   <li>Consultar su información por email.</li>
 *   <li>Gestionar sus métodos de pago (agregar, listar, eliminar) antes de realizar una reserva.</li>
 * </ul>
 *
 * <p><strong>🆕 Mejora de arquitectura (v2.1):</strong></p>
 * <ul>
 *   <li>Se eliminó toda la lógica de mapeo de entidades a DTOs del controlador.</li>
 *   <li>El controlador ahora solo recibe la petición, delega al servicio y envuelve
 *       la respuesta (que ya es un DTO) en un {@link ResponseEntity}.</li>
 *   <li>Esto garantiza una estricta separación de responsabilidades y evita la
 *       exposición accidental de entidades JPA o código duplicado.</li>
 * </ul>
 *
 * <p><strong>Seguridad:</strong> Todos los endpoints son públicos ({@code permitAll})
 * ya que están bajo la ruta {@code /public/**} configurada en {@code SecurityConfig},
 * permitiendo que los huéspedes interactúen con el sistema antes de autenticarse.</p>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@RestController
@RequestMapping("/public/guests")
@RequiredArgsConstructor
@Tag(name = "Gestión de Huéspedes (Público)",
        description = "Endpoints públicos para registro y gestión de métodos de pago de huéspedes")
@Slf4j
public class PublicGuestController {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    /**
     * Servicio que encapsula toda la lógica de negocio, validaciones y
     * mapeo a DTOs relacionados con los huéspedes y sus métodos de pago.
     */
    private final GuestService guestService;

    // =========================================================================
    // CREAR O BUSCAR HUÉSPED
    // =========================================================================

    /**
     * Crea un huésped o reutiliza el registro si coincide el documento de identidad.
     *
     * <p><strong>Idempotencia:</strong> Este endpoint es idempotente. Si el email
     * ya existe en la base de datos, retorna el huésped existente en lugar de
     * crear uno nuevo o lanzar un error.</p>
     */
    @PostMapping
    @Operation(
            summary = "Crear o buscar huésped por documento",
            description = "Crea un nuevo huésped en el sistema. Si el documento ya está registrado, reutiliza el huésped; el email puede repetirse."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Huésped creado exitosamente o existente retornado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (ej: documento con formato incorrecto)")
    })
    public ResponseEntity<GuestResponse> createOrGetGuest(@Valid @RequestBody CreateGuestRequest request) {
        log.info("📝 Solicitud para crear/buscar huésped con email: {}", request.email());

        // El servicio devuelve la entidad Guest (necesaria para lógica interna de otros servicios),
        // y el controlador la mapea a DTO de forma segura y centralizada.
        Guest guest = guestService.createOrFindGuest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GuestResponse.from(guest));
    }

    // =========================================================================
    // BUSCAR HUÉSPED POR EMAIL
    // =========================================================================

    @GetMapping("/email/{email}")
    @Operation(
            summary = "Buscar huésped por email (registro más reciente)",
            description = "Obtiene la información de un huésped utilizando su correo electrónico."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Huésped encontrado"),
            @ApiResponse(responseCode = "404", description = "Huésped no encontrado con ese email")
    })
    public ResponseEntity<GuestResponse> getGuestByEmail(@PathVariable String email) {
        log.debug("🔍 Buscando huésped por email: {}", email);

        GuestResponse guestResponse = guestService.getGuestByEmail(email);
        return ResponseEntity.ok(guestResponse);
    }

    // =========================================================================
    // LISTAR MÉTODOS DE PAGO
    // =========================================================================

    /**
     * Lista todos los métodos de pago de un huésped.
     *
     * <p>Retorna los métodos ordenados colocando primero el método marcado
     * como predeterminado ({@code isDefault = true}), seguido del resto
     * ordenado por fecha de creación.</p>
     */
    @GetMapping("/{guestId}/payment-methods")
    @Operation(
            summary = "Listar métodos de pago de un huésped",
            description = "Obtiene todos los métodos de pago registrados para un huésped específico, " +
                    "ordenados con el método predeterminado primero."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de métodos de pago obtenida"),
            @ApiResponse(responseCode = "404", description = "Huésped no encontrado")
    })
    public ResponseEntity<List<GuestPaymentMethodResponse>> listPaymentMethods(@PathVariable Long guestId) {
        log.debug("💳 Listando métodos de pago para huésped ID: {}", guestId);

        // ✅ El servicio ya devuelve la lista de DTOs mapeada, evitando código repetido en el controlador.
        return ResponseEntity.ok(guestService.listPaymentMethods(guestId));
    }

    // =========================================================================
    // AGREGAR MÉTODO DE PAGO
    // =========================================================================

    @PostMapping("/{guestId}/payment-methods")
    @Operation(
            summary = "Agregar método de pago a un huésped",
            description = "Registra un nuevo método de pago para un huésped."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Método de pago agregado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Huésped no encontrado")
    })
    public ResponseEntity<GuestPaymentMethodResponse> addPaymentMethod(
            @PathVariable Long guestId,
            @Valid @RequestBody GuestPaymentMethodRequest request) {

        log.info("💳 Agregando método de pago '{}' para huésped ID: {}", request.paymentType(), guestId);

        // ✅ El servicio ya devuelve el DTO mapeado, evitando lógica de presentación en el controlador.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(guestService.createPaymentMethod(guestId, request));
    }

    // =========================================================================
    // ELIMINAR MÉTODO DE PAGO
    // =========================================================================

    @DeleteMapping("/{guestId}/payment-methods/{methodId}")
    @Operation(
            summary = "Eliminar método de pago",
            description = "Elimina un método de pago específico de un huésped."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Método de pago eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Método de pago o huésped no encontrado")
    })
    public ResponseEntity<Void> deletePaymentMethod(
            @PathVariable Long guestId,
            @PathVariable Long methodId) {

        log.info("🗑️ Eliminando método de pago ID: {} del huésped ID: {}", methodId, guestId);

        guestService.deletePaymentMethod(guestId, methodId);
        return ResponseEntity.noContent().build();
    }
}