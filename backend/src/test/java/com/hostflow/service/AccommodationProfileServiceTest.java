package com.hostflow.service;

import com.hostflow.dto.response.CloudinaryUploadResult;
import com.hostflow.exception.BusinessException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.AccommodationImage;
import com.hostflow.model.Owner;
import com.hostflow.dto.request.AccommodationProfileRequest;
import com.hostflow.repository.AccommodationImageRepository;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.OwnerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class AccommodationProfileServiceTest {

    private final AccommodationRepository accommodationRepository =
            mock(AccommodationRepository.class);
    private final AccommodationImageRepository imageRepository =
            mock(AccommodationImageRepository.class);
    private final OwnerRepository ownerRepository =
            mock(OwnerRepository.class);
    private final CloudinaryImageService cloudinaryImageService =
            mock(CloudinaryImageService.class);

    private final AccommodationProfileService service =
            new AccommodationProfileService(
                    accommodationRepository,
                    imageRepository,
                    ownerRepository,
                    cloudinaryImageService
            );

    @AfterEach
    void limpiarSincronizacion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }



    @Test
    void creaPerfilConValoresDeMarcaPredeterminadosCuandoSeOmiten() {
        Owner owner = Owner.builder()
                .id(1L)
                .fullName("Propietario de prueba")
                .accommodationName("Cabañas del Río")
                .build();
        when(accommodationRepository.existsByOwner_Id(1L)).thenReturn(false);
        when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(accommodationRepository.save(any(Accommodation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccommodationProfileRequest request = new AccommodationProfileRequest();
        request.setName("Cabañas del Río");

        Accommodation saved = service.createProfile(1L, request);

        assertEquals("#0fa3a3", saved.getPrimaryColor());
        assertEquals("#d4f53c", saved.getAccentColor());
        assertEquals("#f4b8c4", saved.getTertiaryColor());
        assertEquals("none", saved.getBackgroundPattern());
    }

    @Test
    void patchDeColorDeAcentoNoModificaLosOtrosCamposDeMarca() {
        Accommodation accommodation = new Accommodation();
        accommodation.setPrimaryColor("#123456");
        accommodation.setAccentColor("#d4f53c");
        accommodation.setTertiaryColor("#f4b8c4");
        accommodation.setBackgroundPattern("waves");
        when(accommodationRepository.findByOwner_Id(1L)).thenReturn(Optional.of(accommodation));
        when(accommodationRepository.save(any(Accommodation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccommodationProfileRequest request = new AccommodationProfileRequest();
        request.setAccentColor("#abcdef");

        Accommodation saved = service.updateProfile(1L, request);

        assertEquals("#123456", saved.getPrimaryColor());
        assertEquals("#abcdef", saved.getAccentColor());
        assertEquals("#f4b8c4", saved.getTertiaryColor());
        assertEquals("waves", saved.getBackgroundPattern());
    }

    @Test
    void rechazaLaQuintaImagenDeGaleriaAntesDeSubirla() {
        Accommodation accommodation = new Accommodation();
        // En producción la entidad ya está persistida y tiene ID; el mock debe reflejarlo.
        accommodation.setId(100L);
        when(accommodationRepository.findByOwner_IdForUpdate(1L)).thenReturn(Optional.of(accommodation));
        when(imageRepository.countByAccommodation_Id(100L)).thenReturn(4L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.uploadGalleryImage(1L,
                        new MockMultipartFile("file", "imagen.jpg", "image/jpeg", new byte[512_000])));

        assertEquals("GALLERY_IMAGE_LIMIT_EXCEEDED", exception.getErrorCode());
        verify(cloudinaryImageService, never()).uploadImage(any());
        verify(imageRepository, never()).save(any());
    }

    @Test
    void permiteSubirImagenCuandoLaGaleriaTieneTresImagenes() {
        Accommodation accommodation = new Accommodation();
        accommodation.setId(100L);
        when(accommodationRepository.findByOwner_IdForUpdate(1L)).thenReturn(Optional.of(accommodation));
        when(imageRepository.countByAccommodation_Id(100L)).thenReturn(3L);
        when(cloudinaryImageService.uploadImage(any())).thenReturn(new CloudinaryUploadResult(
                "hostflow/galeria-4", "https://res.cloudinary.com/demo/galeria-4.jpg", "jpg", 512_000L, "hostflow"));
        when(imageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccommodationImage saved = service.uploadGalleryImage(1L,
                new MockMultipartFile("file", "imagen.jpg", "image/jpeg", new byte[512_000]));

        assertEquals("hostflow/galeria-4", saved.getCloudinaryPublicId());
        verify(cloudinaryImageService).uploadImage(any());
        verify(imageRepository).save(any());
    }

    @Test
    void eliminaElLogoNuevoCuandoFallaElGuardado() {
        // Arrange: alojamiento con un logo anterior.
        Accommodation accommodation = new Accommodation();
        accommodation.setLogoUrl("https://ejemplo.test/logo-anterior.png");
        accommodation.setLogoPublicId("hostflow/logo-anterior");

        when(accommodationRepository.findByOwner_Id(1L))
                .thenReturn(Optional.of(accommodation));

        when(cloudinaryImageService.uploadImage(any()))
                .thenReturn(new CloudinaryUploadResult(
                        "hostflow/logo-nuevo",
                        "https://ejemplo.test/logo-nuevo.png",
                        "png",
                        1024L,
                        "hostflow"
                ));

        when(accommodationRepository.save(any(Accommodation.class)))
                .thenThrow(new RuntimeException("Fallo de persistencia simulado"));

        TransactionSynchronizationManager.initSynchronization();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        // Act: el guardado falla después de la subida simulada.
        assertThrows(RuntimeException.class,
                () -> service.uploadLogo(1L, file));

        // Simula la finalización de una transacción con rollback.
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();

        synchronizations.forEach(sync ->
                sync.afterCompletion(
                        TransactionSynchronization.STATUS_ROLLED_BACK
                )
        );

        // Assert: se intenta borrar el logo nuevo, no el anterior.
        verify(cloudinaryImageService).deleteImage("hostflow/logo-nuevo");
        verify(cloudinaryImageService, never())
                .deleteImage("hostflow/logo-anterior");
    }
}
