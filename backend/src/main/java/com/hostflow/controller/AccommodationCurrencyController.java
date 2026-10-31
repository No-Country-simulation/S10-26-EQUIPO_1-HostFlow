package com.hostflow.controller;

import com.hostflow.dto.request.AcceptedCurrencyRequest;
import com.hostflow.dto.response.AcceptedCurrencyResponse;
import com.hostflow.enums.Currency;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.AcceptedCurrency;
import com.hostflow.model.Owner;
import com.hostflow.repository.OwnerRepository;
import com.hostflow.service.AcceptedCurrencyService;
import com.hostflow.service.CurrencyConversionService;
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
import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador REST para que el propietario (Owner) gestione las monedas habilitadas
 * de su alojamiento.
 *
 * <p>Permite habilitar o deshabilitar monedas extranjeras (USD, EUR), elegir entre
 * tasas automáticas (obtenidas de APIs externas como DolarAPI o Frankfurter) o
 * tasas manuales, y para el caso del USD, seleccionar el tipo de cotización
 * (blue, oficial, MEP, etc.).</p>
 *
 * <p><strong>Seguridad:</strong> Todos los endpoints requieren autenticación
 * y el rol {@code ROLE_OWNER}. Cada owner solo puede gestionar las monedas
 * de SU PROPIO alojamiento.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/owners/accommodation/currencies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Monedas del alojamiento",
        description = "Habilitar/deshabilitar monedas extranjeras y configurar tasas de conversión")
@Slf4j
public class AccommodationCurrencyController {

    private final AcceptedCurrencyService currencyService;
    private final CurrencyConversionService conversionService;
    private final OwnerRepository ownerRepository;

    // =========================================================================
    // LISTAR MONEDAS CONFIGURADAS
    // =========================================================================

    @GetMapping
    @Operation(summary = "Listar monedas configuradas con sus tasas efectivas",
            description = "Devuelve todas las monedas configuradas para el alojamiento del owner, " +
                    "incluyendo la tasa efectiva actual (automática desde API o manual).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de monedas obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "El owner aún no ha registrado su alojamiento")
    })
    public ResponseEntity<List<AcceptedCurrencyResponse>> list(Authentication auth) {
        Long ownerId = currentOwnerId(auth);

        List<AcceptedCurrencyResponse> responses = currencyService.list(ownerId).stream()
                .map(c -> {
                    BigDecimal effective = safeRate(ownerId, c.getCurrency());
                    return AcceptedCurrencyResponse.from(c, effective);
                })
                .toList();

        return ResponseEntity.ok(responses);
    }

    // =========================================================================
    // CREAR O ACTUALIZAR MONEDA (UPSERT)
    // =========================================================================

    @PutMapping
    @Operation(summary = "Crear o actualizar la configuración de una moneda",
            description = "Habilita/deshabilita una moneda y configura su tasa. " +
                    "Para USD se puede elegir el tipo de cotización (blue, oficial, MEP, etc.). " +
                    "Si la moneda ya existe, se actualiza su configuración.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Moneda configurada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o tasa manual incorrecta"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "El owner aún no ha registrado su alojamiento")
    })
    public ResponseEntity<AcceptedCurrencyResponse> upsert(
            @Valid @RequestBody AcceptedCurrencyRequest request,
            Authentication auth) {

        Long ownerId = currentOwnerId(auth);

        // Llamar al servicio pasando todos los parámetros, incluido el tipo de dólar
        AcceptedCurrency saved = currencyService.upsert(
                ownerId,
                request.currency(),
                Boolean.TRUE.equals(request.enabled()),
                Boolean.TRUE.equals(request.useAutomaticRate()),
                request.manualRate(),
                request.usdRateType()
        );

        BigDecimal effective = safeRate(ownerId, saved.getCurrency());

        return ResponseEntity.ok(AcceptedCurrencyResponse.from(saved, effective));
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Obtiene la tasa efectiva de forma segura.
     *
     * <p>Si la API externa está caída o la tasa no se puede resolver,
     * retorna {@code null} en lugar de lanzar una excepción, permitiendo
     * que el endpoint siga funcionando y mostrando el resto de la configuración.</p>
     */
    private BigDecimal safeRate(Long ownerId, Currency c) {
        try {
            return conversionService.resolveEffectiveRate(ownerId, c);
        } catch (RuntimeException e) {
            log.warn("No se pudo resolver la tasa para la moneda {}: {}", c, e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene el ID del owner actualmente autenticado a partir del token JWT.
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