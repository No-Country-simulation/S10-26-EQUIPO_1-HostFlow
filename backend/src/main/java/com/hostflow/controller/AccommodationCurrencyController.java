package com.hostflow.controller;

import com.hostflow.dto.request.AcceptedCurrencyRequest;
import com.hostflow.dto.response.AcceptedCurrencyResponse;
import com.hostflow.service.AcceptedCurrencyService;
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

/**
 * Controlador REST para que el propietario (Owner) gestione las monedas habilitadas
 * de su alojamiento.
 *
 * <p><strong>🆕 Mejora de arquitectura (v2.0):</strong></p>
 * <ul>
 *   <li>Se eliminó la inyección directa de {@code OwnerRepository} y cualquier consulta a la BD.</li>
 *   <li>El controlador actúa únicamente como un enrutador: extrae el email del contexto de
 *       seguridad y delega toda la lógica de negocio y mapeo a DTOs al servicio.</li>
 *   <li>Se aplica el principio DRY (Don't Repeat Yourself): el servicio centraliza el cálculo
 *       de tasas efectivas, evitando lógica duplicada en esta capa.</li>
 * </ul>
 *
 * <p>Permite habilitar o deshabilitar monedas extranjeras (USD, EUR), elegir entre
 * tasas automáticas (obtenidas de APIs externas) o tasas manuales, y para el caso
 * del USD, seleccionar el tipo de cotización (blue, oficial, MEP, etc.).</p>
 *
 * <p><strong>Seguridad:</strong> Todos los endpoints requieren autenticación JWT válida
 * y el rol {@code ROLE_OWNER}. Cada propietario solo puede gestionar las monedas
 * de SU PROPIO alojamiento.</p>
 *
 * @author HostFlow Team
 * @version 2.0.0
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

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    /**
     * Servicio que encapsula toda la lógica de negocio, validaciones y consultas
     * relacionadas con las monedas del alojamiento, retornando DTOs completamente
     * enriquecidos (incluyendo el cálculo seguro de tasas efectivas).
     */
    private final AcceptedCurrencyService currencyService;

    // =========================================================================
    // ENDPOINTS
    // =========================================================================

    /**
     * Lista todas las monedas configuradas para el alojamiento del propietario,
     * incluyendo sus tasas efectivas actuales (automáticas o manuales).
     *
     * @param auth el contexto de autenticación de Spring Security (garantizado por @PreAuthorize)
     * @return respuesta HTTP 200 OK con la lista de DTOs de configuración de monedas
     */
    @GetMapping
    @Operation(
            summary = "Listar monedas configuradas con sus tasas efectivas",
            description = "Devuelve todas las monedas configuradas para el alojamiento del owner, " +
                    "incluyendo la tasa efectiva actual (automática desde API o manual)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de monedas obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado o token inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner (se requiere ROLE_OWNER)"),
            @ApiResponse(responseCode = "404", description = "El owner aún no ha registrado su alojamiento")
    })
    public ResponseEntity<List<AcceptedCurrencyResponse>> list(Authentication auth) {
        // ✅ Extraer email de forma segura. @PreAuthorize garantiza que auth no es null.
        String ownerEmail = auth.getName();
        log.debug("💱 Listando monedas configuradas para owner email: {}", ownerEmail);

        // ✅ Delegar al servicio. El servicio ya retorna la lista de DTOs mapeada y enriquecida.
        List<AcceptedCurrencyResponse> responses = currencyService.listByEmail(ownerEmail);

        return ResponseEntity.ok(responses);
    }

    /**
     * Crea o actualiza la configuración de una moneda para el alojamiento (Upsert).
     *
     * @param request los datos de la configuración de la moneda (validados por @Valid)
     * @param auth el contexto de autenticación de Spring Security
     * @return respuesta HTTP 200 OK con el DTO de la configuración guardada y su tasa efectiva
     */
    @PutMapping
    @Operation(
            summary = "Crear o actualizar la configuración de una moneda",
            description = "Habilita/deshabilita una moneda y configura su tasa. " +
                    "Para USD se puede elegir el tipo de cotización (blue, oficial, MEP, etc.). " +
                    "Si la moneda ya existe, se actualiza su configuración (patrón Upsert)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Moneda configurada o actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o tasa manual incorrecta (<= 0)"),
            @ApiResponse(responseCode = "401", description = "No autenticado o token inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "El owner aún no ha registrado su alojamiento")
    })
    public ResponseEntity<AcceptedCurrencyResponse> upsert(
            @Valid @RequestBody AcceptedCurrencyRequest request,
            Authentication auth) {

        String ownerEmail = auth.getName();
        log.info("💱 Configurando moneda {} para owner email: {}", request.currency(), ownerEmail);

        // ✅ Uso de Boolean.TRUE.equals para evitar NullPointerException si el frontend envía null
        // ✅ Delegación total al servicio, que se encarga de la persistencia y el mapeo a DTO.
        AcceptedCurrencyResponse response = currencyService.upsertByEmail(
                ownerEmail,
                request.currency(),
                Boolean.TRUE.equals(request.enabled()),
                Boolean.TRUE.equals(request.useAutomaticRate()),
                request.manualRate(),
                request.usdRateType()
        );

        return ResponseEntity.ok(response);
    }
}