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
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el propietario autenticado."));
        Accommodation accommodation = new Accommodation();
        accommodation.setOwner(owner);
        applyRequest(accommodation, request);
        // El modelo de autenticación también conserva el nombre comercial registrado inicialmente.
        owner.setAccommodationName(request.name().trim());
        return accommodationRepository.save(accommodation);
    }

    @Transactional
    public Accommodation updateProfile(Long ownerId, AccommodationProfileRequest request) {
        Accommodation accommodation = accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un alojamiento para editar. Regístrelo primero."));
        applyRequest(accommodation, request);
        accommodation.getOwner().setAccommodationName(request.name().trim());
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

    private void applyRequest(Accommodation a, AccommodationProfileRequest r) {
        a.setName(r.name().trim());
        a.setRoomCount(r.roomCount());
        a.setBathroomCount(r.bathroomCount());
        a.setGuestCapacity(r.guestCapacity());
        a.setBasePrice(r.basePrice());
        a.setLogoUrl(blankToNull(r.logoUrl()));
        a.setPrimaryColor(blankToNull(r.primaryColor()));
        a.setWelcomeMessage(blankToNull(r.welcomeMessage()));
        a.setEmail(blankToNull(r.email()));
        a.setPhone(blankToNull(r.phone()));
        a.setAddress(blankToNull(r.address()));
        a.setBankName(blankToNull(r.bankName()));
        a.setBankAccountHolder(blankToNull(r.bankAccountHolder()));
        a.setBankAccountNumber(blankToNull(r.bankAccountNumber()));
        a.setBankCbuCvu(blankToNull(r.bankCbuCvu()));
        a.setBankAlias(blankToNull(r.bankAlias()));
        a.setBankCuit(blankToNull(r.bankCuit()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
