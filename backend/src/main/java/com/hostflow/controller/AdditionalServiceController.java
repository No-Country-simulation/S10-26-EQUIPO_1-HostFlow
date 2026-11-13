package com.hostflow.controller;

import com.hostflow.dto.request.AdditionalServiceRequest;
import com.hostflow.dto.response.AdditionalServiceImageResponse;
import com.hostflow.dto.response.AdditionalServiceResponse;
import com.hostflow.service.AdditionalServiceImageService;
import com.hostflow.service.AdditionalServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controlador REST para que el propietario (Owner) gestione los servicios
 * adicionales de su alojamiento, incluyendo la gestión de sus imágenes.
 *
 * <p><strong>🆕 Mejora de arquitectura (v2.1):</strong></p>
 * <ul>
 *   <li>Se eliminó la inyección directa de {@code OwnerRepository} y {@code AccommodationRepository}.</li>
 *   <li>Se eliminaron los métodos privados {@code currentOwnerId()} y {@code getAccommodation()}.</li>
 *   <li>El controlador ahora solo extrae el email del usuario autenticado y lo delega
 *       al servicio, manteniendo la estricta separación de responsabilidades (Controller -> Service).</li>
 * </ul>
 *
 * <p><strong>Características clave:</strong></p>
 * <ul>
 *   <li>Los precios se almacenan en la moneda base del alojamiento (ARS).</li>
 *   <li>Al consultarlos, se convierten automáticamente a todas las monedas habilitadas.</li>
 *   <li>Cada servicio puede tener hasta 3 imágenes, almacenadas en Cloudinary.</li>
 *   <li>Solo el propietario del alojamiento puede gestionar sus propios servicios.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@RestController
@RequestMapping("/owners/accommodation/additional-services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Servicios adicionales",
        description = "CRUD de servicios extra con conversión automática a monedas habilitadas y gestión de imágenes")
@Slf4j
public class AdditionalServiceController {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    /**
     * Servicio que encapsula la lógica de negocio de los servicios adicionales.
     * Se encarga de validar la pertenencia del alojamiento al propietario.
     */
    private final AdditionalServiceService serviceService;

    /**
     * Servicio que encapsula la lógica de negocio para la gestión de imágenes
     * de los servicios adicionales (subida a Cloudinary, validaciones, etc.).
     */
    private final AdditionalServiceImageService imageService;

    // =========================================================================
    // CRUD DE SERVICIOS ADICIONALES
    // =========================================================================

    @GetMapping
    @Operation(
            summary = "Listar servicios adicionales con precios convertidos e imágenes",
            description = "Devuelve todos los servicios adicionales del alojamiento del owner, " +
                    "incluyendo sus precios convertidos y la lista de imágenes asociadas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de servicios obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "El owner aún no ha registrado su alojamiento")
    })
    public ResponseEntity<List<AdditionalServiceResponse>> list(Authentication auth) {
        // ✅ Extraemos solo el email y delegamos la resolución al servicio
        String ownerEmail = auth.getName();
        log.debug("📋 Listando servicios adicionales para owner email: {}", ownerEmail);

        List<AdditionalServiceResponse> responses = serviceService.listWithConvertedPricesByEmail(ownerEmail);
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    @Operation(
            summary = "Crear un servicio adicional",
            description = "Registra un nuevo servicio adicional. El precio se almacena en la moneda base " +
                    "y se convierte automáticamente. Las imágenes se agregan posteriormente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Servicio creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (nombre vacío, precio <= 0)"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "El owner aún no ha registrado su alojamiento")
    })
    public ResponseEntity<AdditionalServiceResponse> create(
            @Valid @RequestBody AdditionalServiceRequest request,
            Authentication auth) {

        String ownerEmail = auth.getName();
        log.info("✅ Creando servicio adicional para owner email: {}", ownerEmail);

        AdditionalServiceResponse created = serviceService.createWithConvertedPricesByEmail(ownerEmail, request);

        log.info("✅ Servicio adicional creado: '{}' ({} ARS, {}) para owner {}",
                request.name(), request.basePrice(), request.priceType(), ownerEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{serviceId}")
    @Operation(
            summary = "Actualizar parcialmente un servicio adicional",
            description = "Modifica únicamente los campos enviados. Los campos omitidos permanecen sin cambios."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicio actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado o no pertenece al alojamiento")
    })
    public ResponseEntity<AdditionalServiceResponse> update(
            @PathVariable Long serviceId,
            @Valid @RequestBody AdditionalServiceRequest request,
            Authentication auth) {

        String ownerEmail = auth.getName();
        log.info("✏️ Actualizando servicio adicional ID: {} para owner email: {}", serviceId, ownerEmail);

        AdditionalServiceResponse updated = serviceService.updateWithConvertedPricesByEmail(ownerEmail, serviceId, request);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{serviceId}")
    @Operation(
            summary = "Eliminar un servicio adicional",
            description = "Elimina permanentemente un servicio adicional y todas sus imágenes asociadas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Servicio eliminado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado o no pertenece al alojamiento")
    })
    public ResponseEntity<Void> delete(
            @PathVariable Long serviceId,
            Authentication auth) {

        String ownerEmail = auth.getName();
        log.info("🗑️ Eliminando servicio adicional ID: {} para owner email: {}", serviceId, ownerEmail);

        serviceService.deleteByEmail(ownerEmail, serviceId);

        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // GESTIÓN DE IMÁGENES DE SERVICIOS ADICIONALES
    // =========================================================================

    @GetMapping("/{serviceId}/images")
    @Operation(
            summary = "Listar imágenes de un servicio adicional",
            description = "Devuelve hasta 3 imágenes asociadas al servicio, ordenadas por índice."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Imágenes obtenidas exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado o no pertenece al alojamiento")
    })
    public ResponseEntity<List<AdditionalServiceImageResponse>> listImages(
            @PathVariable Long serviceId,
            Authentication auth) {

        String ownerEmail = auth.getName();
        log.debug("🖼️ Listando imágenes del servicio ID: {} para owner email: {}", serviceId, ownerEmail);

        List<AdditionalServiceImageResponse> images = imageService.listImagesByEmail(ownerEmail, serviceId);
        return ResponseEntity.ok(images);
    }

    @PostMapping(value = "/{serviceId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Subir una imagen a un servicio adicional",
            description = "Sube una imagen a Cloudinary. Restricciones: Máx 3 imágenes, hasta 2 MB, tipo image/*."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Imagen subida exitosamente",
                    content = @Content(schema = @Schema(implementation = AdditionalServiceImageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Archivo inválido o se excedió el límite"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado"),
            @ApiResponse(responseCode = "502", description = "Error al subir la imagen a Cloudinary")
    })
    public ResponseEntity<AdditionalServiceImageResponse> uploadImage(
            @PathVariable Long serviceId,
            @Parameter(description = "Archivo de imagen (hasta 2 MB)", required = true)
            @RequestParam("file") MultipartFile file,
            Authentication auth) {

        String ownerEmail = auth.getName();
        log.info("⬆️ Subiendo imagen para servicio ID: {} por owner email: {}", serviceId, ownerEmail);

        AdditionalServiceImageResponse response = imageService.uploadImageByEmail(ownerEmail, serviceId, file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{serviceId}/images/{imageId}")
    @Operation(
            summary = "Eliminar una imagen de un servicio adicional",
            description = "Elimina la imagen de Cloudinary y de la base de datos. Las restantes se reordenan."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Imagen eliminada exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de owner"),
            @ApiResponse(responseCode = "404", description = "Imagen o servicio no encontrado"),
            @ApiResponse(responseCode = "502", description = "Error al eliminar la imagen de Cloudinary")
    })
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long serviceId,
            @PathVariable Long imageId,
            Authentication auth) {

        String ownerEmail = auth.getName();
        log.info("🗑️ Eliminando imagen ID: {} del servicio ID: {} por owner email: {}", imageId, serviceId, ownerEmail);

        imageService.deleteImageByEmail(ownerEmail, serviceId, imageId);

        return ResponseEntity.noContent().build();
    }
}
