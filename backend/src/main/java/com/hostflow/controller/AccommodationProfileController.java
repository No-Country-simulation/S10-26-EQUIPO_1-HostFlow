package com.hostflow.controller;

import com.hostflow.dto.request.AccommodationImageRequest;
import com.hostflow.dto.request.AccommodationProfileRequest;
import com.hostflow.dto.response.AccommodationImageResponse;
import com.hostflow.dto.response.AccommodationProfileResponse;
import com.hostflow.model.Accommodation;
import com.hostflow.model.AccommodationImage;
import com.hostflow.model.Owner;
import com.hostflow.repository.OwnerRepository;
import com.hostflow.service.AccommodationProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/owners/accommodation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Perfil y galería del alojamiento", description = "CRUD de datos generales y galería; separado de la desactivación temporal")
public class AccommodationProfileController {
    private final AccommodationProfileService profileService;
    private final OwnerRepository ownerRepository;

    @PostMapping("/profile")
    @Operation(
            operationId = "createAccommodationProfile",
            summary = "Registrar el perfil del alojamiento",
            description = "Crea el perfil del propietario autenticado. No se recibe ownerId desde el cliente y solo se permite un perfil por propietario. No modifica la desactivación temporal ni la disponibilidad.",
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
            @ApiResponse(responseCode = "201", description = "Perfil creado correctamente", content = @Content(schema = @Schema(implementation = AccommodationProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o ya existe un perfil", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER", content = @Content),
            @ApiResponse(responseCode = "404", description = "No se encontró el propietario autenticado", content = @Content)
    })
    public ResponseEntity<AccommodationProfileResponse> createProfile(
            @Valid @RequestBody AccommodationProfileRequest request, Authentication authentication) {
        Accommodation created = profileService.createProfile(getCurrentOwnerId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AccommodationProfileResponse.from(created));
    }

    @GetMapping("/profile")
    @Operation(operationId = "getAccommodationProfile",
            summary = "Consultar mi perfil de alojamiento",
            description = "Devuelve los datos generales y las referencias de imágenes de la galería del propietario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil recuperado correctamente", content = @Content(schema = @Schema(implementation = AccommodationProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER", content = @Content),
            @ApiResponse(responseCode = "404", description = "No existe un perfil para el propietario autenticado", content = @Content)
    })
    public ResponseEntity<AccommodationProfileResponse> getProfile(Authentication authentication) {
        Accommodation accommodation = profileService.getProfile(getCurrentOwnerId(authentication));
        return ResponseEntity.ok(AccommodationProfileResponse.from(accommodation));
    }

    @PatchMapping("/profile")
    @Operation(operationId = "updateAccommodationProfile",
            summary = "Actualizar parcialmente mi perfil de alojamiento",
            description = "Actualiza únicamente los campos informados en la solicitud. Los campos omitidos o enviados como null permanecen sin cambios. No permite seleccionar un propietario mediante un ID enviado por el cliente ni modifica la desactivación temporal.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true, description = "Enviar solo los campos que se desean modificar. Los campos omitidos o null se conservan sin cambios.",
                    content = @Content(schema = @Schema(implementation = AccommodationProfileRequest.class),
                            examples = @ExampleObject(name = "Actualización parcial de precio", value = """
                                    {
                                      "basePrice": 90000.00
                                    }
                                    """)))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado correctamente", content = @Content(schema = @Schema(implementation = AccommodationProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o solicitud sin campos modificables", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER", content = @Content),
            @ApiResponse(responseCode = "404", description = "No existe un perfil para el propietario autenticado", content = @Content)
    })
    public ResponseEntity<AccommodationProfileResponse> updateProfile(
            @Valid @RequestBody AccommodationProfileRequest request, Authentication authentication) {
        Accommodation updated = profileService.updateProfile(getCurrentOwnerId(authentication), request);
        return ResponseEntity.ok(AccommodationProfileResponse.from(updated));
    }

    @GetMapping("/gallery")
    @Operation(operationId = "getAccommodationGallery",
            summary = "Consultar mi galería de imágenes",
            description = "Devuelve las URLs registradas para el alojamiento propio; no descarga ni devuelve archivos binarios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Galería recuperada correctamente", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AccommodationImageResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER", content = @Content),
            @ApiResponse(responseCode = "404", description = "No existe un perfil para el propietario autenticado", content = @Content)
    })
    public ResponseEntity<List<AccommodationImageResponse>> getGallery(Authentication authentication) {
        List<AccommodationImageResponse> images = profileService.getGallery(getCurrentOwnerId(authentication))
                .stream().map(AccommodationImageResponse::from).toList();
        return ResponseEntity.ok(images);
    }

    @PostMapping("/gallery")
    @Operation(operationId = "addAccommodationGalleryImage",
            summary = "Agregar una imagen a mi galería",
            description = "Registra una referencia de imagen por URL HTTPS; el archivo binario no se carga al backend.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true, description = "URL HTTPS de la imagen.",
                    content = @Content(schema = @Schema(implementation = AccommodationImageRequest.class),
                            examples = @ExampleObject(name = "URL de imagen", value = "{ \"url\": \"https://example.com/images/cabin-exterior.jpg\" }"))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Imagen registrada correctamente", content = @Content(schema = @Schema(implementation = AccommodationImageResponse.class))),
            @ApiResponse(responseCode = "400", description = "URL vacía, inválida o no HTTPS", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER", content = @Content),
            @ApiResponse(responseCode = "404", description = "No existe un perfil para el propietario autenticado", content = @Content)
    })
    public ResponseEntity<AccommodationImageResponse> addImage(
            @Valid @RequestBody AccommodationImageRequest request, Authentication authentication) {
        AccommodationImage image = profileService.addImage(getCurrentOwnerId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AccommodationImageResponse.from(image));
    }

    @PostMapping(value = "/gallery/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "uploadAccommodationGalleryImage",
            summary = "Subir una imagen a mi galería",
            description = "Recibe un archivo de imagen, lo sube a Cloudinary y registra su URL segura en la galería del alojamiento del propietario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Imagen subida y registrada correctamente", content = @Content(schema = @Schema(implementation = AccommodationImageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Archivo ausente, vacío, no válido o superior a 5 MB", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER", content = @Content),
            @ApiResponse(responseCode = "404", description = "No existe un perfil para el propietario autenticado", content = @Content),
            @ApiResponse(responseCode = "502", description = "No fue posible subir la imagen a Cloudinary", content = @Content)
    })
    public ResponseEntity<AccommodationImageResponse> uploadGalleryImage(
            @Parameter(description = "Archivo de imagen (máximo 5 MB).", required = true)
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        AccommodationImage image = profileService.uploadGalleryImage(getCurrentOwnerId(authentication), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(AccommodationImageResponse.from(image));
    }

    @PostMapping(value = "/profile/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "uploadAccommodationLogo",
            summary = "Subir o reemplazar el logotipo de mi alojamiento",
            description = "Recibe un archivo de imagen, lo sube a Cloudinary y actualiza logoUrl del alojamiento del propietario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logotipo actualizado correctamente", content = @Content(schema = @Schema(implementation = AccommodationProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Archivo ausente, vacío, no válido o superior a 5 MB", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER", content = @Content),
            @ApiResponse(responseCode = "404", description = "No existe un perfil para el propietario autenticado", content = @Content),
            @ApiResponse(responseCode = "502", description = "No fue posible subir el logotipo a Cloudinary", content = @Content)
    })
    public ResponseEntity<AccommodationProfileResponse> uploadLogo(
            @Parameter(description = "Archivo del logotipo (máximo 5 MB).", required = true)
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        Accommodation accommodation = profileService.uploadLogo(getCurrentOwnerId(authentication), file);
        return ResponseEntity.ok(AccommodationProfileResponse.from(accommodation));
    }

    @DeleteMapping("/gallery/{imageId}")
    @Operation(operationId = "deleteAccommodationGalleryImage",
            summary = "Eliminar una imagen de mi galería",
            description = "Si la imagen fue subida a Cloudinary por HostFlow, elimina primero el recurso remoto y luego el registro. Las URLs externas agregadas manualmente solo se eliminan de la galería.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Imagen eliminada correctamente"),
            @ApiResponse(responseCode = "401", description = "Token ausente o inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene el rol OWNER", content = @Content),
            @ApiResponse(responseCode = "404", description = "La imagen no existe o no pertenece al propietario autenticado", content = @Content),
            @ApiResponse(responseCode = "502", description = "Cloudinary no confirmó la eliminación; el registro se conserva", content = @Content)
    })
    public ResponseEntity<Void> deleteImage(
            @Parameter(description = "Identificador de la imagen que se desea eliminar.", example = "12", required = true)
            @PathVariable Long imageId,
            Authentication authentication) {
        profileService.deleteImage(getCurrentOwnerId(authentication), imageId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentOwnerId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("Se requiere autenticación.");
        }
        Owner owner = ownerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new com.hostflow.exception.ResourceNotFoundException("No se encontró el propietario autenticado."));
        return owner.getId();
    }
}
