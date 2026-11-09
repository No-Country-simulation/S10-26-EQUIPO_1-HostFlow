package com.hostflow.controller;

import com.hostflow.dto.request.AccommodationProfileRequest;
import com.hostflow.dto.response.AccommodationImageResponse;
import com.hostflow.dto.response.AccommodationProfileResponse;
import com.hostflow.service.AccommodationProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
 * Controlador REST para la gestión del perfil y la galería del alojamiento.
 *
 * <p><strong>🆕 Mejora de arquitectura (v2.0):</strong></p>
 * <ul>
 *   <li>Se eliminó la inyección directa de {@code OwnerRepository}.</li>
 *   <li>Se eliminó el método privado {@code getCurrentOwnerId()} que consultaba la BD.</li>
 *   <li>El controlador ahora solo extrae el email del usuario autenticado y lo delega
 *       al servicio, manteniendo la estricta separación de responsabilidades.</li>
 *   <li>El servicio retorna directamente DTOs ({@link AccommodationProfileResponse} y
 *       {@link AccommodationImageResponse}), evitando lógica de mapeo redundante en esta capa.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/owners/accommodation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Perfil y galería del alojamiento", description = "CRUD de datos generales y galería del alojamiento propio")
@Slf4j
public class AccommodationProfileController {

    // =========================================================================
    // DEPENDENCIAS INYECTADAS
    // =========================================================================

    /**
     * Servicio que encapsula toda la lógica de negocio, validaciones y consultas
     * relacionadas con el perfil y la galería del alojamiento.
     */
    private final AccommodationProfileService profileService;

    // =========================================================================
    // ENDPOINTS DE PERFIL
    // =========================================================================

    /**
     * Registra el perfil del alojamiento para el propietario autenticado.
     */
    @PostMapping("/profile")
    @Operation(
            operationId = "createAccommodationProfile",
            summary = "Registrar el perfil del alojamiento",
            description = "Crea el perfil del propietario autenticado. Solo se permite un perfil por propietario.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos generales, capacidad, precio, identidad visual y contacto.",
                    content = @Content(schema = @Schema(implementation = AccommodationProfileRequest.class),
                            examples = @ExampleObject(name = "Perfil de ejemplo", value = """
                                    {
                                      "name": "Cabañas del Río",
                                      "roomCount": 8,
                                      "bathroomCount": 4,
                                      "guestCapacity": 20,
                                      "basePrice": 85000.00,
                                      "primaryColor": "#1E293B",
                                      "welcomeMessage": "¡Te damos la bienvenida!",
                                      "email": "reservas@example.com",
                                      "phone": "+54 11 5555 5555",
                                      "address": "Delta del Tigre"
                                    }
                                    """)))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Perfil creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o ya existe un perfil"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER"),
            @ApiResponse(responseCode = "404", description = "No se encontró el propietario autenticado")
    })
    public ResponseEntity<AccommodationProfileResponse> createProfile(
            @Valid @RequestBody AccommodationProfileRequest request,
            Authentication authentication) {

        String ownerEmail = getOwnerEmailOrThrow(authentication);
        log.info("🏠 Creando perfil de alojamiento para owner email: {}", ownerEmail);

        // ✅ El servicio devuelve el DTO ya mapeado, evitando lógica de conversión en el controlador
        AccommodationProfileResponse response = profileService.createProfileByEmail(ownerEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Consulta el perfil completo del alojamiento del propietario autenticado.
     */
    @GetMapping("/profile")
    @Operation(
            operationId = "getAccommodationProfile",
            summary = "Consultar mi perfil de alojamiento",
            description = "Devuelve los datos generales y las referencias de imágenes de la galería."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil recuperado correctamente"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER"),
            @ApiResponse(responseCode = "404", description = "No existe un perfil para el propietario autenticado")
    })
    public ResponseEntity<AccommodationProfileResponse> getProfile(Authentication authentication) {
        String ownerEmail = getOwnerEmailOrThrow(authentication);
        log.debug("🔍 Consultando perfil de alojamiento para owner email: {}", ownerEmail);

        return ResponseEntity.ok(profileService.getProfileByEmail(ownerEmail));
    }

    /**
     * Actualiza parcialmente los datos del perfil del alojamiento.
     */
    @PatchMapping("/profile")
    @Operation(
            operationId = "updateAccommodationProfile",
            summary = "Actualizar parcialmente mi perfil de alojamiento",
            description = "Actualiza únicamente los campos informados. Los campos omitidos o null permanecen sin cambios.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Enviar solo los campos que se desean modificar.",
                    content = @Content(schema = @Schema(implementation = AccommodationProfileRequest.class),
                            examples = @ExampleObject(name = "Actualización parcial de precio", value = """
                                    {
                                      "basePrice": 90000.00
                                    }
                                    """)))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o solicitud sin campos modificables"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER"),
            @ApiResponse(responseCode = "404", description = "No existe un perfil para el propietario autenticado")
    })
    public ResponseEntity<AccommodationProfileResponse> updateProfile(
            @Valid @RequestBody AccommodationProfileRequest request,
            Authentication authentication) {

        String ownerEmail = getOwnerEmailOrThrow(authentication);
        log.info("✏️ Actualizando parcialmente perfil de alojamiento para owner email: {}", ownerEmail);

        return ResponseEntity.ok(profileService.updateProfileByEmail(ownerEmail, request));
    }

    // =========================================================================
    // ENDPOINTS DE GALERÍA E IMÁGENES
    // =========================================================================

    /**
     * Lista todas las imágenes registradas en la galería del alojamiento.
     */
    @GetMapping("/gallery")
    @Operation(
            operationId = "getAccommodationGallery",
            summary = "Consultar mi galería de imágenes",
            description = "Devuelve las URLs registradas para el alojamiento propio; no devuelve archivos binarios."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Galería recuperada correctamente", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AccommodationImageResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER"),
            @ApiResponse(responseCode = "404", description = "No existe un perfil para el propietario autenticado")
    })
    public ResponseEntity<List<AccommodationImageResponse>> getGallery(Authentication authentication) {
        String ownerEmail = getOwnerEmailOrThrow(authentication);
        log.debug("🖼️ Consultando galería para owner email: {}", ownerEmail);

        // ✅ El servicio ya devuelve la lista de DTOs mapeada
        return ResponseEntity.ok(profileService.getGalleryByEmail(ownerEmail));
    }

    /**
     * Sube un archivo de imagen a Cloudinary y lo registra en la galería.
     */
    @PostMapping(value = "/gallery/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "uploadAccommodationGalleryImage",
            summary = "Subir una imagen a mi galería",
            description = "Recibe un archivo de imagen, lo sube a Cloudinary y registra su URL segura."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Imagen subida y registrada correctamente"),
            @ApiResponse(responseCode = "400", description = "Archivo ausente, vacío, inválido o fuera del rango de 500 KB a 2 MB, o galería completa"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER"),
            @ApiResponse(responseCode = "404", description = "No existe un perfil para el propietario autenticado"),
            @ApiResponse(responseCode = "502", description = "No fue posible subir la imagen a Cloudinary")
    })
    public ResponseEntity<AccommodationImageResponse> uploadGalleryImage(
            @Parameter(description = "Archivo de imagen (entre 500 KB y 2 MB).", required = true)
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        String ownerEmail = getOwnerEmailOrThrow(authentication);
        log.info("⬆️ Subiendo imagen a galería para owner email: {}", ownerEmail);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.uploadGalleryImageByEmail(ownerEmail, file));
    }

    /**
     * Sube o reemplaza el logotipo del alojamiento en Cloudinary.
     */
    @PostMapping(value = "/profile/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "uploadAccommodationLogo",
            summary = "Subir o reemplazar el logotipo de mi alojamiento",
            description = "Recibe un archivo de imagen, lo sube a Cloudinary y actualiza el logoUrl del alojamiento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logotipo actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Archivo ausente, vacío, inválido o fuera del rango de 500 KB a 2 MB"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER"),
            @ApiResponse(responseCode = "404", description = "No existe un perfil para el propietario autenticado"),
            @ApiResponse(responseCode = "502", description = "No fue posible subir el logotipo a Cloudinary")
    })
    public ResponseEntity<AccommodationProfileResponse> uploadLogo(
            @Parameter(description = "Archivo del logotipo (entre 500 KB y 2 MB).", required = true)
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        String ownerEmail = getOwnerEmailOrThrow(authentication);
        log.info("🎨 Subiendo/reemplazando logotipo para owner email: {}", ownerEmail);

        return ResponseEntity.ok(profileService.uploadLogoByEmail(ownerEmail, file));
    }

    /**
     * Elimina una imagen de la galería y su recurso asociado en Cloudinary.
     */
    @DeleteMapping("/gallery/{imageId}")
    @Operation(
            operationId = "deleteAccommodationGalleryImage",
            summary = "Eliminar una imagen de mi galería",
            description = "Elimina de Cloudinary el recurso administrado por HostFlow y después elimina el registro de la galería."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Imagen eliminada correctamente"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER"),
            @ApiResponse(responseCode = "404", description = "La imagen no existe o no pertenece al propietario autenticado"),
            @ApiResponse(responseCode = "502", description = "Cloudinary no confirmó la eliminación; el registro se conserva")
    })
    public ResponseEntity<Void> deleteImage(
            @Parameter(description = "Identificador de la imagen que se desea eliminar.", example = "12", required = true)
            @PathVariable Long imageId,
            Authentication authentication) {

        String ownerEmail = getOwnerEmailOrThrow(authentication);
        log.info("🗑️ Eliminando imagen ID: {} de la galería para owner email: {}", imageId, ownerEmail);

        profileService.deleteImageByEmail(ownerEmail, imageId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Extrae el email del propietario autenticado directamente del contexto de seguridad.
     *
     * <p>Este método reemplaza la consulta directa a la base de datos que antes
     * se realizaba en el controlador, delegando esa responsabilidad completamente al servicio.</p>
     *
     * @param authentication el contexto de autenticación de Spring Security
     * @return el email del propietario autenticado
     * @throws org.springframework.security.authentication.AuthenticationCredentialsNotFoundException si no hay autenticación
     */
    private String getOwnerEmailOrThrow(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "Se requiere autenticación válida para realizar esta operación.");
        }
        return authentication.getName();
    }
}