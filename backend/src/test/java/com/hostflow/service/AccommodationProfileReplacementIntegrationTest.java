package com.hostflow.service;

import com.hostflow.dto.response.CloudinaryUploadResult;
import com.hostflow.model.Accommodation;
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.OwnerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AccommodationProfileReplacementIntegrationTest {

    private static final String OLD_PUBLIC_ID = "hostflow/test/old-logo";
    private static final String OLD_LOGO_URL = "https://res.cloudinary.com/test/image/upload/old-logo.png";
    private static final String NEW_PUBLIC_ID = "hostflow/test/new-logo";
    private static final String NEW_LOGO_URL = "https://res.cloudinary.com/test/image/upload/new-logo.png";

    @Autowired
    private AccommodationProfileService accommodationProfileService;

    @Autowired
    private AccommodationRepository accommodationRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @MockBean
    private CloudinaryImageService cloudinaryImageService;

    private Long ownerId;
    private Long accommodationId;

    @BeforeEach
    void setUp() {
        Owner owner = Owner.builder()
                .fullName("Integration Test Owner")
                .accommodationName("Integration Test Accommodation")
                .email("logo-replacement-integration@test.local")
                .password("not-used-in-this-test")
                .termsAccepted(true)
                .enabled(true)
                .build();
        owner = ownerRepository.saveAndFlush(owner);
        ownerId = owner.getId();

        Accommodation accommodation = new Accommodation();
        accommodation.setOwner(owner);
        accommodation.setName("Integration Test Accommodation");
        accommodation.setLogoUrl(OLD_LOGO_URL);
        accommodation.setLogoPublicId(OLD_PUBLIC_ID);
        accommodation = accommodationRepository.saveAndFlush(accommodation);
        accommodationId = accommodation.getId();
    }

    @AfterEach
    void tearDown() {
        if (accommodationId != null) {
            accommodationRepository.deleteById(accommodationId);
        }
        if (ownerId != null) {
            ownerRepository.deleteById(ownerId);
        }
    }

    @Test
    void uploadLogo_whenReplacementSucceeds_persistsNewLogoAndDeletesOldAssetAfterCommit() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "new-logo.png", "image/png", new byte[]{1, 2, 3}
        );
        when(cloudinaryImageService.uploadImage(any()))
                .thenReturn(new CloudinaryUploadResult(
                        NEW_PUBLIC_ID, NEW_LOGO_URL, "png", 3L, "hostflow/test"
                ));

        // La eliminación se ejecuta en afterCompletion(COMMITTED). Al consultar la BD
        // desde aquí, el nuevo logo debe estar ya confirmado y visible.
        AtomicReference<String> logoUrlAtDeletion = new AtomicReference<>();
        AtomicReference<String> publicIdAtDeletion = new AtomicReference<>();
        doAnswer(invocation -> {
            Accommodation persisted = accommodationRepository.findById(accommodationId)
                    .orElseThrow(() -> new IllegalStateException("El alojamiento no quedó persistido"));
            logoUrlAtDeletion.set(persisted.getLogoUrl());
            publicIdAtDeletion.set(persisted.getLogoPublicId());
            return null;
        }).when(cloudinaryImageService).deleteImage(OLD_PUBLIC_ID);

        Accommodation result = accommodationProfileService.uploadLogo(ownerId, file);

        assertThat(result.getLogoUrl()).isEqualTo(NEW_LOGO_URL);
        assertThat(result.getLogoPublicId()).isEqualTo(NEW_PUBLIC_ID);

        Accommodation persisted = accommodationRepository.findById(accommodationId).orElseThrow();
        assertThat(persisted.getLogoUrl()).isEqualTo(NEW_LOGO_URL);
        assertThat(persisted.getLogoPublicId()).isEqualTo(NEW_PUBLIC_ID);

        assertThat(logoUrlAtDeletion.get()).isEqualTo(NEW_LOGO_URL);
        assertThat(publicIdAtDeletion.get()).isEqualTo(NEW_PUBLIC_ID);

        verify(cloudinaryImageService).uploadImage(file);
        verify(cloudinaryImageService).deleteImage(OLD_PUBLIC_ID);
        verify(cloudinaryImageService, never()).deleteImage(NEW_PUBLIC_ID);
    }
}
