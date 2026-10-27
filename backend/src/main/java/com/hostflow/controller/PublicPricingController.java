package com.hostflow.controller;


import com.hostflow.dto.response.AdditionalServiceResponse;
import com.hostflow.dto.response.ConvertedPriceResponse;
import com.hostflow.enums.Currency;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.AdditionalService;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.AdditionalServiceRepository;
import com.hostflow.service.CurrencyConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.util.ArrayList;
import java.util.List;

/**
 * Controlador REST PÚBLICO (sin autenticación) para que los huéspedes consulten
 * los precios del alojamiento en múltiples monedas.
 *
 * <p>Estos endpoints están diseñados para ser consumidos por la página pública
 * del alojamiento (landing page) antes de que el huésped inicie sesión.
 * Permiten mostrar el precio base y los servicios adicionales en ARS, USD y EUR
 * simultáneamente, facilitando la toma de decisiones de huéspedes internacionales.</p>
 *
 * <p><strong>Seguridad:</strong> Todos los endpoints de este controlador son
 * públicos ({@code permitAll()}) gracias a la configuración en {@code SecurityConfig}
 * para la ruta {@code /public/**}.</p>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/public/accommodations/{accommodationId}")
@RequiredArgsConstructor
@Tag(name = "Precios públicos",
        description = "Consulta de precios del alojamiento en múltiples monedas (sin autenticación)")
@Slf4j
public class PublicPricingController {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    private final AccommodationRepository accommodationRepository;
    private final AdditionalServiceRepository serviceRepository;
    private final CurrencyConversionService conversionService;

    // =========================================================================
    // PRECIO BASE DEL ALOJAMIENTO
    // =========================================================================

    @GetMapping("/base-price")
    @Operation(
            summary = "Precio base del alojamiento en todas las monedas habilitadas",
            description = "Devuelve el precio base (basePrice) del alojamiento convertido " +
                    "a todas las monedas habilitadas por el propietario (ARS, USD, EUR). " +
                    "Si una moneda no está configurada o la API de tasas está caída, " +
                    "se omite de la respuesta para no mostrar precios incorrectos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Precios obtenidos exitosamente"),
            @ApiResponse(responseCode = "404", description = "Alojamiento no encontrado con ese ID")
    })
    public ResponseEntity<List<ConvertedPriceResponse>> getBasePrice(
            @PathVariable Long accommodationId) {

        Accommodation acc = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el alojamiento con ID: " + accommodationId));

        List<ConvertedPriceResponse> result = new ArrayList<>();

        // 1. Precio en la moneda base (siempre incluido, ej: ARS)
        result.add(ConvertedPriceResponse.of(acc.getBaseCurrency(), acc.getBasePrice()));

        // 2. Precios en las demás monedas (USD, EUR)
        for (Currency c : Currency.values()) {
            if (c == acc.getBaseCurrency()) continue;
            try {
                result.add(ConvertedPriceResponse.of(
                        c, conversionService.convert(acc.getId(), acc.getBasePrice(), c)));
            } catch (RuntimeException e) {
                // Moneda no habilitada, sin tasa disponible o API caída: se omite silenciosamente
                log.debug("Moneda {} omitida para alojamiento {}: {}", c, accommodationId, e.getMessage());
            }
        }

        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // SERVICIOS ADICIONALES
    // =========================================================================

    @GetMapping("/additional-services")
    @Operation(
            summary = "Servicios adicionales con precios en todas las monedas habilitadas",
            description = "Devuelve los servicios adicionales activos del alojamiento, " +
                    "con sus precios convertidos a todas las monedas habilitadas. " +
                    "Solo se muestran los servicios marcados como activos por el propietario."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicios obtenidos exitosamente"),
            @ApiResponse(responseCode = "404", description = "Alojamiento no encontrado con ese ID")
    })
    public ResponseEntity<List<AdditionalServiceResponse>> getAdditionalServices(
            @PathVariable Long accommodationId) {

        Accommodation acc = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el alojamiento con ID: " + accommodationId));

        // Obtener solo los servicios activos
        List<AdditionalService> services = serviceRepository
                .findAllByAccommodation_IdAndActiveTrueOrderByCreatedAtAsc(acc.getId());

        // Mapear cada servicio a su DTO con precios convertidos
        List<AdditionalServiceResponse> responses = services.stream()
                .map(s -> toResponse(acc, s))
                .toList();

        return ResponseEntity.ok(responses);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Construye el DTO de respuesta para un servicio adicional, incluyendo
     * los precios convertidos a todas las monedas habilitadas.
     */
    private AdditionalServiceResponse toResponse(Accommodation acc, AdditionalService s) {
        List<ConvertedPriceResponse> converted = new ArrayList<>();

        // Precio en la moneda base (siempre incluido)
        converted.add(ConvertedPriceResponse.of(acc.getBaseCurrency(), s.getBasePrice()));

        // Precios en las demás monedas habilitadas
        for (Currency c : Currency.values()) {
            if (c == acc.getBaseCurrency()) continue;
            try {
                converted.add(ConvertedPriceResponse.of(
                        c, conversionService.convert(acc.getId(), s.getBasePrice(), c)));
            } catch (RuntimeException e) {
                // Moneda no habilitada o sin tasa: se omite
                log.debug("Moneda {} omitida para servicio {}: {}", c, s.getId(), e.getMessage());
            }
        }

        return AdditionalServiceResponse.from(s, converted);
    }
}