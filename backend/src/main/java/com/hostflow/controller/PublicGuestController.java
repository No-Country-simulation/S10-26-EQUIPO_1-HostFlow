package com.hostflow.controller;

import com.hostflow.dto.request.CreateGuestRequest;
import com.hostflow.dto.request.GuestPaymentMethodRequest;
import com.hostflow.dto.response.GuestPaymentMethodResponse;
import com.hostflow.dto.response.GuestResponse;
import com.hostflow.model.Guest;
import com.hostflow.model.GuestPaymentMethod;
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
 * <p><strong>Seguridad:</strong> Todos los endpoints son públicos ({@code permitAll})
 * ya que están bajo la ruta {@code /public/**} configurada en {@code SecurityConfig},
 * permitiendo que los huéspedes interactúen con el sistema antes de autenticarse.</p>
 *
 * <p><strong>Flujo de uso típico:</strong></p>
 * <ol>
 *   <li>Huésped se registra: {@code POST /public/guests}</li>
 *   <li>Huésped agrega método de pago: {@code POST /public/guests/{id}/payment-methods}</li>
 *   <li>Owner crea reserva usando el email del huésped: {@code POST /owners/reservations}</li>
 *   <li>El sistema valida que el huésped tenga al menos un método de pago activo.</li>
 * </ol>
 *
 * @author HostFlow Team
 * @version 2.0.0
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

    private final GuestService guestService;

    // =========================================================================
    // CREAR O BUSCAR HUÉSPED
    // =========================================================================

    /**
     * Crea un nuevo huésped o retorna el existente si el email ya está registrado.
     *
     * <p><strong>Idempotencia:</strong> Este endpoint es idempotente. Si el email
     * ya existe en la base de datos, retorna el huésped existente en lugar de
     * crear uno nuevo o lanzar un error. Esto permite que el frontend reintente
     * la operación de forma segura sin generar duplicados.</p>
     *
     * <p><strong>Validaciones:</strong></p>
     * <ul>
     *   <li>Nombre y apellido son obligatorios.</li>
     *   <li>Email debe tener formato válido.</li>
     *   <li>Documento (opcional): solo números, entre 5 y 12 dígitos.</li>
     * </ul>
     */
    @PostMapping
    @Operation(
            summary = "Crear o buscar huésped por email",
            description = """
            Crea un nuevo huésped en el sistema. Si el email ya está registrado,
            retorna el huésped existente. Este endpoint es útil para que los
            huéspedes se registren antes de hacer una reserva.
            
            <strong>Flujo:</strong>
            1. El huésped proporciona sus datos (nombre, apellido, email, teléfono, documento).
            2. El sistema verifica si el email ya existe.
            3. Si no existe, valida la unicidad del documento y crea un nuevo huésped.
            4. Si existe, retorna el huésped existente.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Huésped creado exitosamente o existente retornado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (ej: documento con formato incorrecto)")
    })
    public ResponseEntity<GuestResponse> createOrGetGuest(
            @Valid @RequestBody CreateGuestRequest request) {

        log.info("📝 Solicitud para crear/buscar huésped con email: {}", request.email());

        // Delegamos toda la lógica de validación y creación/búsqueda al servicio
        Guest guest = guestService.createOrFindGuest(request);

        // En REST, un POST que resulta en la persistencia (o recuperación idempotente)
        // de un recurso debe retornar 201 Created.
        return ResponseEntity.status(HttpStatus.CREATED).body(GuestResponse.from(guest));
    }

    // =========================================================================
    // BUSCAR HUÉSPED POR EMAIL
    // =========================================================================

    /**
     * Busca un huésped por su email.
     *
     * <p>Útil para que el frontend pueda verificar si un huésped ya está
     * registrado y obtener su ID antes de intentar agregar métodos de pago
     * o realizar una reserva.</p>
     */
    @GetMapping("/email/{email}")
    @Operation(
            summary = "Buscar huésped por email",
            description = "Obtiene la información de un huésped utilizando su correo electrónico."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Huésped encontrado"),
            @ApiResponse(responseCode = "404", description = "Huésped no encontrado con ese email")
    })
    public ResponseEntity<GuestResponse> getGuestByEmail(@PathVariable String email) {
        log.debug(" Buscando huésped por email: {}", email);
        Guest guest = guestService.getGuestByEmail(email);
        return ResponseEntity.ok(GuestResponse.from(guest));
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
    public ResponseEntity<List<GuestPaymentMethodResponse>> listPaymentMethods(
            @PathVariable Long guestId) {

        log.debug("💳 Listando métodos de pago para huésped ID: {}", guestId);
        List<GuestPaymentMethod> methods = guestService.listPaymentMethods(guestId);

        List<GuestPaymentMethodResponse> responses = methods.stream()
                .map(GuestPaymentMethodResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    // =========================================================================
    // AGREGAR MÉTODO DE PAGO
    // =========================================================================

    /**
     * Agrega un nuevo método de pago a un huésped.
     *
     * <p><strong>Reglas de negocio:</strong></p>
     * <ul>
     *   <li>Si se marca como predeterminado ({@code isDefault = true}), se establece
     *       como el método principal para futuras reservas.</li>
     *   <li>Si no existe ningún método predeterminado para este huésped, el sistema
     *       establece automáticamente este nuevo método como predeterminado.</li>
     * </ul>
     */
    @PostMapping("/{guestId}/payment-methods")
    @Operation(
            summary = "Agregar método de pago a un huésped",
            description = """
            Registra un nuevo método de pago para un huésped.
            
            <strong>Restricciones:</strong>
            - paymentType y displayName son obligatorios.
            - Si se marca como predeterminado (isDefault=true), se establece como método principal.
            - Si no existe un método predeterminado, el primero se establece como tal.
            
            <strong>Tipos de pago soportados:</strong>
            - BANK_TRANSFER: Transferencia bancaria
            - DIGITAL_WALLET: Billetera digital (Mercado Pago, PayPal, etc.)
            - CARD: Tarjeta de crédito/débito
            - CASH: Efectivo
            - OTHER: Otro
            """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Método de pago agregado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Huésped no encontrado")
    })
    public ResponseEntity<GuestPaymentMethodResponse> addPaymentMethod(
            @PathVariable Long guestId,
            @Valid @RequestBody GuestPaymentMethodRequest request) {

        // ✅ CORRECCIÓN: En records de Java, se usa request.paymentType() NO request.getPaymentType()
        log.info("💳 Agregando método de pago '{}' para huésped ID: {}", request.paymentType(), guestId);

        GuestPaymentMethod method = guestService.createPaymentMethod(guestId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GuestPaymentMethodResponse.from(method));
    }

    // =========================================================================
    // ELIMINAR MÉTODO DE PAGO
    // =========================================================================

    /**
     * Elimina un método de pago de un huésped.
     *
     * <p><strong>Manejo de integridad:</strong> Si el método eliminado era el
     * predeterminado, el sistema automáticamente limpia el flag para que otro
     * método existente pueda ser promovido a predeterminado en el futuro.</p>
     */
    @DeleteMapping("/{guestId}/payment-methods/{methodId}")
    @Operation(
            summary = "Eliminar método de pago",
            description = "Elimina un método de pago específico de un huésped. " +
                    "Si era el método predeterminado, se limpia el flag para mantener la integridad."
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