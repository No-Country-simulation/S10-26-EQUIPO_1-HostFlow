package com.hostflow.controller;

import com.hostflow.dto.response.AdditionalServiceResponse;
import com.hostflow.dto.response.ConvertedPriceResponse;
import com.hostflow.service.PublicPricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST PÚBLICO (sin autenticación) para que los huéspedes consulten
 * los precios del alojamiento en múltiples monedas.
 *
 * <p><strong>🆕 Mejora de arquitectura (v2.0):</strong></p>
 * <ul>
 *   <li>Se eliminó la inyección directa de repositorios, respetando la separación de capas.</li>
 *   <li>Se eliminó el manejo de {@code @Transactional} en el controlador.</li>
 *   <li>Toda la lógica de negocio se delega al servicio, manteniendo el controlador como un enrutador puro.</li>
 * </ul>
 */
@RestController
@RequestMapping("/public/accommodations/{accommodationId}")
@RequiredArgsConstructor
@Tag(name = "Precios públicos", description = "Consulta de precios del alojamiento en múltiples monedas (sin autenticación)")
@Slf4j
public class PublicPricingController {

    // ✅ ÚNICA DEPENDENCIA: El servicio que contiene la lógica y la transacción
    private final PublicPricingService pricingService;

    @GetMapping("/base-price")
    @Operation(
            summary = "Precio base del alojamiento en todas las monedas habilitadas",
            description = "Devuelve el precio base convertido a todas las monedas habilitadas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Precios obtenidos exitosamente"),
            @ApiResponse(responseCode = "404", description = "Alojamiento no encontrado con ese ID")
    })
    public ResponseEntity<List<ConvertedPriceResponse>> getBasePrice(@PathVariable Long accommodationId) {
        log.debug("📊 Solicitud de precio base para alojamiento ID: {}", accommodationId);

        // ✅ Delegamos toda la lógica y la transacción al servicio
        List<ConvertedPriceResponse> result = pricingService.getBasePrice(accommodationId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/additional-services")
    @Operation(
            summary = "Servicios adicionales con precios en todas las monedas habilitadas",
            description = "Devuelve los servicios adicionales activos con sus precios convertidos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicios obtenidos exitosamente"),
            @ApiResponse(responseCode = "404", description = "Alojamiento no encontrado con ese ID")
    })
    public ResponseEntity<List<AdditionalServiceResponse>> getAdditionalServices(@PathVariable Long accommodationId) {
        log.debug("🧾 Solicitud de servicios adicionales para alojamiento ID: {}", accommodationId);

        // ✅ Delegamos toda la lógica y la transacción al servicio
        List<AdditionalServiceResponse> responses = pricingService.getAdditionalServices(accommodationId);
        return ResponseEntity.ok(responses);
    }
}