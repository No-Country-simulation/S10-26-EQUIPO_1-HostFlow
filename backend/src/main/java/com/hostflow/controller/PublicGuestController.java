package com.hostflow.controller;

import com.hostflow.dto.request.CreateGuestRequest;
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

/**
 * Controlador REST público para datos de huésped usados en el onboarding.
 *
 * <p>No expone métodos de pago del huésped: están fuera del alcance de HostFlow.
 * Los métodos de pago informativos del flujo de reservas son los del alojamiento.</p>
 *
 * @author HostFlow Team
 * @version 3.0.0
 */
@RestController
@RequestMapping("/public/guests")
@RequiredArgsConstructor
@Tag(name = "Gestión de Huéspedes (Público)",
        description = "Endpoints públicos para registro y consulta de huéspedes (sin métodos de pago)")
@Slf4j
public class PublicGuestController {

    private final GuestService guestService;

    @PostMapping
    @Operation(
            summary = "Crear o buscar huésped por documento",
            description = "Crea un nuevo huésped. Si el documento ya está registrado, reutiliza el huésped; el email puede repetirse."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Huésped creado o existente retornado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<GuestResponse> createOrGetGuest(@Valid @RequestBody CreateGuestRequest request) {
        log.info("Solicitud para crear/buscar huésped con email: {}", request.email());
        Guest guest = guestService.createOrFindGuest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GuestResponse.from(guest));
    }

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
        log.debug("Buscando huésped por email: {}", email);
        return ResponseEntity.ok(guestService.getGuestByEmail(email));
    }
}
