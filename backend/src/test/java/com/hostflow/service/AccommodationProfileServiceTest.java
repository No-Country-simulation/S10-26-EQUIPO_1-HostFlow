package com.hostflow.service;

import com.hostflow.dto.response.CloudinaryUploadResult;
import com.hostflow.model.Accommodation;
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
import static org.mockito.ArgumentMatchers.any;
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
