package com.hostflow.service;

import com.hostflow.dto.request.AccommodationProfileRequest;
import com.hostflow.dto.response.CloudinaryUploadResult;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.AccommodationImage;
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationImageRepository;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationProfileService {
    private static final long MAX_GALLERY_IMAGES = 4;
    private static final String DEFAULT_PRIMARY_COLOR = "#0fa3a3";
    private static final String DEFAULT_ACCENT_COLOR = "#d4f53c";
    private static final String DEFAULT_TERTIARY_COLOR = "#f4b8c4";
    private static final String DEFAULT_BACKGROUND_PATTERN = "none";
    private final AccommodationRepository accommodationRepository;
    private final AccommodationImageRepository imageRepository;
    private final OwnerRepository ownerRepository;
    private final CloudinaryImageService cloudinaryImageService;

    @Transactional(readOnly = true)
    public Accommodation getProfile(Long ownerId) {
        return accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("El propietario aún no ha registrado los datos de su alojamiento."));
    }

    @Transactional
    public Accommodation createProfile(Long ownerId, AccommodationProfileRequest request) {
        if (accommodationRepository.existsByOwner_Id(ownerId)) {
            throw new BusinessException("Ya existe un alojamiento asociado a este propietario.", "ACCOMMODATION_ALREADY_EXISTS");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("El nombre del alojamiento es obligatorio.");
        }
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el propietario autenticado."));
        Accommodation accommodation = new Accommodation();
        accommodation.setOwner(owner);
        applyRequest(accommodation, request);
        // El modelo de autenticación también conserva el nombre comercial registrado inicialmente.
        owner.setAccommodationName(request.getName().trim());
        return accommodationRepository.save(accommodation);
    }

    @Transactional
    public Accommodation updateProfile(Long ownerId, AccommodationProfileRequest request) {
        if (isEmptyPatch(request)) {
            throw new BusinessException("Debe enviar al menos un campo para actualizar.");
        }
        Accommodation accommodation = accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un alojamiento para editar. Regístrelo primero."));
        applyPatch(accommodation, request);
        // Solo sincronizar el nombre del modelo de autenticación si se modificó en el PATCH.
        if (request.getName() != null) {
            accommodation.getOwner().setAccommodationName(request.getName().trim());
        }
        return accommodationRepository.save(accommodation);
    }

    @Transactional(readOnly = true)
    public List<AccommodationImage> getGallery(Long ownerId) {
        Accommodation accommodation = getProfile(ownerId);
        return imageRepository.findAllByAccommodation_IdOrderByCreatedAtAscIdAsc(accommodation.getId());
    }

    /**
     * Sube una imagen a Cloudinary y registra su URL segura en la galería del alojamiento.
     */
    @Transactional
    public AccommodationImage uploadGalleryImage(Long ownerId, MultipartFile file) {
        // El bloqueo de la fila evita que dos cargas simultáneas superen el máximo de cuatro.
        Accommodation accommodation = accommodationRepository.findByOwner_IdForUpdate(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registre los datos generales del alojamiento antes de agregar imágenes."));

        ensureGalleryHasSpace(accommodation.getId());

        CloudinaryUploadResult uploaded = cloudinaryImageService.uploadImage(file);
        AccommodationImage image = AccommodationImage.builder()
                .accommodation(accommodation)
                .imageUrl(uploaded.secureUrl())
                .cloudinaryPublicId(uploaded.publicId())
                .build();
        return imageRepository.save(image);
    }

    /**
     * Sube el logotipo a Cloudinary y actualiza el logoUrl del alojamiento.
     */
    @Transactional
    public Accommodation uploadLogo(Long ownerId, MultipartFile file) {
        Accommodation accommodation = accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registre los datos generales del alojamiento antes de actualizar el logotipo."));

        // Conservamos el logo actual hasta que el nuevo esté subido y la BD confirme el cambio.
        String previousPublicId = accommodation.getLogoPublicId();
        String previousLogoUrl = accommodation.getLogoUrl();
        CloudinaryUploadResult uploaded = cloudinaryImageService.uploadImage(file);

        accommodation.setLogoUrl(uploaded.secureUrl());
        accommodation.setLogoPublicId(uploaded.publicId());

        // Cloudinary no participa en la transacción de la BD. Por eso la limpieza remota
        // se ejecuta solo cuando la transacción termina, evitando borrar el logo anterior
        // si la persistencia del nuevo logo falla.
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // No borramos el recurso anterior fuera de una sincronización transaccional:
            // no podríamos garantizar que el cambio de la BD haya quedado confirmado.
            log.warn("No hay sincronización transaccional activa al sustituir el logo; se conserva el logo anterior en Cloudinary.");
        } else {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) {
                        if (previousPublicId != null && !previousPublicId.isBlank()
                                && !previousPublicId.equals(uploaded.publicId())) {
                            try {
                                cloudinaryImageService.deleteImage(previousPublicId);
                            } catch (RuntimeException ex) {
                                log.warn("El logo nuevo quedó guardado, pero no se pudo eliminar el logo anterior {}. Requiere limpieza posterior.", previousPublicId, ex);
                            }
                        } else if (previousLogoUrl != null && !previousLogoUrl.isBlank()
                                && (previousPublicId == null || previousPublicId.isBlank())) {
                            log.info("El logo anterior {} no tiene logo_public_id registrado; se conserva en Cloudinary para evitar eliminar un recurso incorrecto.", previousLogoUrl);
                        }
                    } else {
                        // La BD rechazó el cambio: el logo nuevo no debe quedar huérfano.
                        try {
                            cloudinaryImageService.deleteImage(uploaded.publicId());
                        } catch (RuntimeException ex) {
                            log.warn("La actualización del logo no se confirmó y tampoco se pudo limpiar el nuevo recurso {}.", uploaded.publicId(), ex);
                        }
                    }
                }
            });
        }

        // Guardamos después de registrar el callback: si save() falla, la finalización
        // de la transacción limpiará el recurso nuevo que acabamos de subir.
        return accommodationRepository.save(accommodation);
    }

    private void ensureGalleryHasSpace(Long accommodationId) {
        if (imageRepository.countByAccommodation_Id(accommodationId) >= MAX_GALLERY_IMAGES) {
            throw new BusinessException(
                    "La galería permite un máximo de 4 imágenes.",
                    "GALLERY_IMAGE_LIMIT_EXCEEDED"
            );
        }
    }

    @Transactional
    public void deleteImage(Long ownerId, Long imageId) {
        Accommodation accommodation = accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un alojamiento para este propietario."));
        AccommodationImage image = imageRepository.findByIdAndAccommodation_Id(imageId, accommodation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró esa imagen en la galería de tu alojamiento."));

        // Solo borramos en Cloudinary recursos subidos y administrados por HostFlow.
        if (image.getCloudinaryPublicId() != null && !image.getCloudinaryPublicId().isBlank()) {
            cloudinaryImageService.deleteImage(image.getCloudinaryPublicId());
        }

        // El registro se elimina únicamente después de confirmar la operación remota.
        int deletedRows = imageRepository.deleteImageFromAccommodation(imageId, accommodation.getId());
        if (deletedRows == 0) {
            throw new ResourceNotFoundException("No se encontró esa imagen en la galería de tu alojamiento.");
        }
    }

    private boolean isEmptyPatch(AccommodationProfileRequest r) {
        return r.getName() == null
                && r.getRoomCount() == null
                && r.getBathroomCount() == null
                && r.getGuestCapacity() == null
                && r.getBasePrice() == null
                && r.getPrimaryColor() == null
                && r.getAccentColor() == null
                && r.getTertiaryColor() == null
                && r.getBackgroundPattern() == null
                && r.getWelcomeMessage() == null
                && r.getEmail() == null
                && r.getPhone() == null
                && r.getAddress() == null
                ;
    }

    /** Aplica todos los campos para la creación del perfil. */
    private void applyRequest(Accommodation a, AccommodationProfileRequest r) {
        a.setName(r.getName().trim());
        a.setRoomCount(r.getRoomCount());
        a.setBathroomCount(r.getBathroomCount());
        a.setGuestCapacity(r.getGuestCapacity());
        a.setBasePrice(r.getBasePrice());
        a.setPrimaryColor(defaultIfBlank(r.getPrimaryColor(), DEFAULT_PRIMARY_COLOR));
        a.setAccentColor(defaultIfBlank(r.getAccentColor(), DEFAULT_ACCENT_COLOR));
        a.setTertiaryColor(defaultIfBlank(r.getTertiaryColor(), DEFAULT_TERTIARY_COLOR));
        a.setBackgroundPattern(defaultIfBlank(r.getBackgroundPattern(), DEFAULT_BACKGROUND_PATTERN));
        a.setWelcomeMessage(blankToNull(r.getWelcomeMessage()));
        a.setEmail(blankToNull(r.getEmail()));
        a.setPhone(blankToNull(r.getPhone()));
        a.setAddress(blankToNull(r.getAddress()));
    }

    /**
     * Aplica únicamente los campos no nulos. En este contrato, tanto un campo omitido
     * como uno enviado explícitamente como null significan "no modificar".
     * Para limpiar campos de texto opcionales, se puede enviar una cadena vacía cuando
     * sus validaciones lo permitan; blankToNull la persiste como null.
     */
    private void applyPatch(Accommodation a, AccommodationProfileRequest r) {
        if (r.getName() != null) {
            if (r.getName().isBlank()) {
                throw new BusinessException("El nombre del alojamiento no puede estar vacío.");
            }
            a.setName(r.getName().trim());
        }
        if (r.getRoomCount() != null) a.setRoomCount(r.getRoomCount());
        if (r.getBathroomCount() != null) a.setBathroomCount(r.getBathroomCount());
        if (r.getGuestCapacity() != null) a.setGuestCapacity(r.getGuestCapacity());
        if (r.getBasePrice() != null) a.setBasePrice(r.getBasePrice());
        if (r.getPrimaryColor() != null) a.setPrimaryColor(blankToNull(r.getPrimaryColor()));
        if (r.getAccentColor() != null) a.setAccentColor(blankToNull(r.getAccentColor()));
        if (r.getTertiaryColor() != null) a.setTertiaryColor(blankToNull(r.getTertiaryColor()));
        if (r.getBackgroundPattern() != null) a.setBackgroundPattern(blankToNull(r.getBackgroundPattern()));
        if (r.getWelcomeMessage() != null) a.setWelcomeMessage(blankToNull(r.getWelcomeMessage()));
        if (r.getEmail() != null) a.setEmail(blankToNull(r.getEmail()));
        if (r.getPhone() != null) a.setPhone(blankToNull(r.getPhone()));
        if (r.getAddress() != null) a.setAddress(blankToNull(r.getAddress()));
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
