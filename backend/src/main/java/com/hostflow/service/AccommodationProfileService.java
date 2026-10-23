package com.hostflow.service;

import com.hostflow.dto.request.AccommodationImageRequest;
import com.hostflow.dto.request.AccommodationProfileRequest;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.AccommodationImage;
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationImageRepository;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccommodationProfileService {
    private final AccommodationRepository accommodationRepository;
    private final AccommodationImageRepository imageRepository;
    private final OwnerRepository ownerRepository;

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

    @Transactional
    public AccommodationImage addImage(Long ownerId, AccommodationImageRequest request) {
        Accommodation accommodation = accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Registre los datos generales del alojamiento antes de agregar imágenes."));
        AccommodationImage image = AccommodationImage.builder()
                .accommodation(accommodation).imageUrl(request.url().trim()).build();
        return imageRepository.save(image);
    }

    @Transactional
    public void deleteImage(Long ownerId, Long imageId) {
        Accommodation accommodation = accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un alojamiento para este propietario."));
        imageRepository.findByIdAndAccommodation_Id(imageId, accommodation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró esa imagen en la galería de tu alojamiento."));

        // DELETE explícito y acotado por alojamiento: no depende del estado de la colección
        // images que pueda estar cargada en memoria dentro del contexto de persistencia.
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
                && r.getLogoUrl() == null
                && r.getPrimaryColor() == null
                && r.getWelcomeMessage() == null
                && r.getEmail() == null
                && r.getPhone() == null
                && r.getAddress() == null
                && r.getBankName() == null
                && r.getBankAccountHolder() == null
                && r.getBankAccountNumber() == null
                && r.getBankCbuCvu() == null
                && r.getBankAlias() == null
                && r.getBankCuit() == null;
    }

    /** Aplica todos los campos para la creación del perfil. */
    private void applyRequest(Accommodation a, AccommodationProfileRequest r) {
        a.setName(r.getName().trim());
        a.setRoomCount(r.getRoomCount());
        a.setBathroomCount(r.getBathroomCount());
        a.setGuestCapacity(r.getGuestCapacity());
        a.setBasePrice(r.getBasePrice());
        a.setLogoUrl(blankToNull(r.getLogoUrl()));
        a.setPrimaryColor(blankToNull(r.getPrimaryColor()));
        a.setWelcomeMessage(blankToNull(r.getWelcomeMessage()));
        a.setEmail(blankToNull(r.getEmail()));
        a.setPhone(blankToNull(r.getPhone()));
        a.setAddress(blankToNull(r.getAddress()));
        a.setBankName(blankToNull(r.getBankName()));
        a.setBankAccountHolder(blankToNull(r.getBankAccountHolder()));
        a.setBankAccountNumber(blankToNull(r.getBankAccountNumber()));
        a.setBankCbuCvu(blankToNull(r.getBankCbuCvu()));
        a.setBankAlias(blankToNull(r.getBankAlias()));
        a.setBankCuit(blankToNull(r.getBankCuit()));
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
        if (r.getLogoUrl() != null) a.setLogoUrl(blankToNull(r.getLogoUrl()));
        if (r.getPrimaryColor() != null) a.setPrimaryColor(blankToNull(r.getPrimaryColor()));
        if (r.getWelcomeMessage() != null) a.setWelcomeMessage(blankToNull(r.getWelcomeMessage()));
        if (r.getEmail() != null) a.setEmail(blankToNull(r.getEmail()));
        if (r.getPhone() != null) a.setPhone(blankToNull(r.getPhone()));
        if (r.getAddress() != null) a.setAddress(blankToNull(r.getAddress()));
        if (r.getBankName() != null) a.setBankName(blankToNull(r.getBankName()));
        if (r.getBankAccountHolder() != null) a.setBankAccountHolder(blankToNull(r.getBankAccountHolder()));
        if (r.getBankAccountNumber() != null) a.setBankAccountNumber(blankToNull(r.getBankAccountNumber()));
        if (r.getBankCbuCvu() != null) a.setBankCbuCvu(blankToNull(r.getBankCbuCvu()));
        if (r.getBankAlias() != null) a.setBankAlias(blankToNull(r.getBankAlias()));
        if (r.getBankCuit() != null) a.setBankCuit(blankToNull(r.getBankCuit()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
