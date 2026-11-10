package com.hostflow.controller;

import com.hostflow.dto.request.AccommodationDisableRequest;
import com.hostflow.dto.response.AccommodationDisableResponse;
import com.hostflow.service.AccommodationTemporaryDisableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owners/accommodation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestión de Alojamiento", description = "Endpoints para que los owners gestionen su alojamiento")
@SecurityRequirement(name = "Bearer Authentication")
public class AccommodationController {

    private final AccommodationTemporaryDisableService disableService;

    @PostMapping("/disable")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Desactivar temporalmente mi alojamiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Alojamiento desactivado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Ya existe una desactivación activa o fechas inválidas"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "Owner no encontrado")
    })
    public ResponseEntity<AccommodationDisableResponse> disableAccommodation(
            @Valid @RequestBody AccommodationDisableRequest request,
            Authentication authentication) {

        String ownerEmail = authentication.getName();
        log.debug("🔒 Solicitando desactivación de alojamiento para email: {}", ownerEmail);

        // ✅ El servicio ya retorna el DTO mapeado
        AccommodationDisableResponse response = disableService.disableAccommodationByEmail(
                ownerEmail,
                request.getReason(),
                request.getDisabledFrom(),
                request.getDisabledUntil()
        );
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/enable")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Reactivar mi alojamiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alojamiento reactivado exitosamente"),
            @ApiResponse(responseCode = "400", description = "No hay desactivación activa"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner")
    })
    public ResponseEntity<AccommodationDisableResponse> enableAccommodation(Authentication authentication) {
        String ownerEmail = authentication.getName();
        log.debug("🔓 Solicitando reactivación de alojamiento para email: {}", ownerEmail);

        // ✅ El servicio ya retorna el DTO mapeado
        AccommodationDisableResponse response = disableService.enableAccommodationByEmail(ownerEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Consultar estado de mi alojamiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner")
    })
    public ResponseEntity<AccommodationDisableResponse> getAccommodationStatus(Authentication authentication) {
        String ownerEmail = authentication.getName();
        log.debug("📊 Consultando estado de alojamiento para email: {}", ownerEmail);

        // ✅ El servicio ya retorna el DTO mapeado (o null si no hay desactivación)
        AccommodationDisableResponse response = disableService.getActiveDisableByEmail(ownerEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Historial de desactivaciones de mi alojamiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner")
    })
    public ResponseEntity<List<AccommodationDisableResponse>> getAccommodationHistory(Authentication authentication) {
        String ownerEmail = authentication.getName();
        log.debug("📜 Obteniendo historial de desactivaciones para email: {}", ownerEmail);

        // ✅ El servicio ya retorna la lista de DTOs mapeada
        List<AccommodationDisableResponse> responses = disableService.getDisableHistoryByEmail(ownerEmail);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/availability")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Verificar disponibilidad en fecha específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disponibilidad consultada exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner")
    })
    public ResponseEntity<Boolean> checkAvailability(
            @RequestParam java.time.LocalDateTime date,
            Authentication authentication) {

        String ownerEmail = authentication.getName();
        log.debug("📅 Verificando disponibilidad para email: {} en fecha: {}", ownerEmail, date);

        boolean isAvailable = disableService.isAccommodationAvailableByEmail(ownerEmail, date);
        return ResponseEntity.ok(isAvailable);
    }
}