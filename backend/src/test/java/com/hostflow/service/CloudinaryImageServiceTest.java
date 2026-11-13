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

    /** 500 KB exactos (500 * 1024). */
    private static final long MIN_BYTES = 512_000L;
    /** 2 MB exactos (2 * 1024 * 1024). */
    private static final long MAX_BYTES = 2_097_152L;

    private Cloudinary cloudinary;
    private CloudinaryImageService service;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        CloudinaryProperties properties = mock(CloudinaryProperties.class);
        org.mockito.Mockito.when(properties.getFolder()).thenReturn("hostflow");
        service = new CloudinaryImageService(cloudinary, properties, MIN_BYTES, MAX_BYTES);
    }

    @Test
    void rechazaImagenInferiorAlMinimo() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pequena.jpg", "image/jpeg", new byte[(int) MIN_BYTES - 1]);

        assertThrows(InvalidImageException.class, () -> service.uploadImage(file));
        verify(cloudinary, never()).uploader();
    }

    @Test
    void rechazaImagenSuperiorAlMaximo() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "grande.jpg", "image/jpeg", new byte[(int) MAX_BYTES + 1]);

        assertThrows(InvalidImageException.class, () -> service.uploadImage(file));
        verify(cloudinary, never()).uploader();
    }

    @Test
    void aceptaElLimiteMinimoParaLaValidacionDeTamano() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "minima.jpg", "image/jpeg", new byte[(int) MIN_BYTES]);

        // El mock no realiza una carga real: el error esperado ocurre después de validar el tamaño.
        assertThrows(CloudinaryUploadException.class, () -> service.uploadImage(file));
    }

    @Test
    void aceptaElLimiteMaximoParaLaValidacionDeTamano() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "maxima.jpg", "image/jpeg", new byte[(int) MAX_BYTES]);

        // El mock no realiza una carga real: el error esperado ocurre después de validar el tamaño.
        assertThrows(CloudinaryUploadException.class, () -> service.uploadImage(file));
    }
}
