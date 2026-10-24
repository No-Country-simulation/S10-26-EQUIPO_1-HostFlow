package com.hostflow.service;

import com.hostflow.dto.response.CloudinaryUploadResult;
import com.hostflow.model.Accommodation;
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.OwnerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AccommodationProfileRollbackIntegrationTest {

    private static final String OLD_LOGO_URL =
            "https://res.cloudinary.com/test/old-logo.png";

    private static final String OLD_PUBLIC_ID =
            "hostflow/old-logo";

    @Autowired
    private AccommodationProfileService accommodationProfileService;

    @Autowired
    private AccommodationRepository accommodationRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @MockBean
    private CloudinaryImageService cloudinaryImageService;

    @Test
    void uploadLogo_WhenDatabaseUpdateFails_RollsBackAndDeletesNewLogo() {
        // Arrange: crear un propietario real en H2.
        Owner owner = new Owner();
        owner.setFullName("Propietario de prueba");
        owner.setAccommodationName("Alojamiento de prueba");
        owner.setEmail("rollback-" + UUID.randomUUID() + "@example.test");
        owner.setPassword("test-password");
        owner.setTermsAccepted(true);
        owner.setEnabled(true);

        owner = ownerRepository.saveAndFlush(owner);

        // Crear un alojamiento cuyo logo anterior ya está guardado.
        Accommodation accommodation = new Accommodation();
        accommodation.setOwner(owner);
        accommodation.setName("Alojamiento de prueba");
        accommodation.setLogoUrl(OLD_LOGO_URL);
        accommodation.setLogoPublicId(OLD_PUBLIC_ID);

        accommodation = accommodationRepository.saveAndFlush(accommodation);

        Long ownerId = owner.getId();
        Long accommodationId = accommodation.getId();

        // Simular una imagen subida a Cloudinary.
        // El publicId supera el límite de 255 caracteres de logo_public_id.
        String newPublicId = "x".repeat(256);

        CloudinaryUploadResult uploadResult = new CloudinaryUploadResult(
                newPublicId,
                "https://res.cloudinary.com/test/new-logo.png",
                "png",
                1024L,
                "hostflow"
        );

        when(cloudinaryImageService.uploadImage(any(MultipartFile.class)))
                .thenReturn(uploadResult);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "new-logo.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        // Act + Assert: la persistencia debe fallar y la transacción revertirse.
        assertThatThrownBy(
                () -> accommodationProfileService.uploadLogo(ownerId, file)
        ).isInstanceOf(RuntimeException.class);

        // Consultar nuevamente la BD, fuera de la transacción fallida.
        Accommodation persisted = accommodationRepository
                .findById(accommodationId)
                .orElseThrow();

        // El logo anterior debe permanecer intacto.
        assertThat(persisted.getLogoUrl()).isEqualTo(OLD_LOGO_URL);
        assertThat(persisted.getLogoPublicId()).isEqualTo(OLD_PUBLIC_ID);

        // La limpieza remota debe afectar al recurso nuevo, no al anterior.
        verify(cloudinaryImageService).uploadImage(any(MultipartFile.class));
        verify(cloudinaryImageService).deleteImage(newPublicId);
        verify(cloudinaryImageService, never()).deleteImage(OLD_PUBLIC_ID);
    }
}
