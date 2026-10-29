package com.hostflow.service;

import com.cloudinary.Cloudinary;
import com.hostflow.config.CloudinaryProperties;
import com.hostflow.exception.CloudinaryUploadException;
import com.hostflow.exception.InvalidImageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CloudinaryImageServiceTest {

    private Cloudinary cloudinary;
    private CloudinaryImageService service;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        CloudinaryProperties properties = mock(CloudinaryProperties.class);
        org.mockito.Mockito.when(properties.getFolder()).thenReturn("hostflow");
        service = new CloudinaryImageService(cloudinary, properties);
    }

    @Test
    void rechazaImagenInferiorA500KB() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pequena.jpg", "image/jpeg", new byte[499_999]);

        assertThrows(InvalidImageException.class, () -> service.uploadImage(file));
        verify(cloudinary, never()).uploader();
    }

    @Test
    void rechazaImagenSuperiorA2MB() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "grande.jpg", "image/jpeg", new byte[2_000_001]);

        assertThrows(InvalidImageException.class, () -> service.uploadImage(file));
        verify(cloudinary, never()).uploader();
    }

    @Test
    void aceptaElLimiteMinimoDe500KBParaLaValidacionDeTamano() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "minima.jpg", "image/jpeg", new byte[500_000]);

        // El mock no realiza una carga real: el error esperado ocurre después de validar el tamaño.
        assertThrows(CloudinaryUploadException.class, () -> service.uploadImage(file));
    }

    @Test
    void aceptaElLimiteMaximoDe2MBParaLaValidacionDeTamano() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "maxima.jpg", "image/jpeg", new byte[2_000_000]);

        // El mock no realiza una carga real: el error esperado ocurre después de validar el tamaño.
        assertThrows(CloudinaryUploadException.class, () -> service.uploadImage(file));
    }
}
