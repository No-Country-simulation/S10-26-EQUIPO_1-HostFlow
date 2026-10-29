package com.hostflow.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hostflow.config.CloudinaryProperties;
import com.hostflow.dto.response.CloudinaryUploadResult;
import com.hostflow.exception.CloudinaryUploadException;
import com.hostflow.exception.CloudinaryDeleteException;
import com.hostflow.exception.InvalidImageException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryImageService {

    private static final long MIN_IMAGE_SIZE_BYTES = 500_000L;
    private static final long MAX_IMAGE_SIZE_BYTES = 2_000_000L;

    private final Cloudinary cloudinary;
    private final String folder;

    public CloudinaryImageService(
            Cloudinary cloudinary,
            CloudinaryProperties properties) {
        this.cloudinary = cloudinary;
        this.folder = properties.getFolder();
    }

    public CloudinaryUploadResult uploadImage(MultipartFile file) {
        validateImage(file);

        try {
            Map<?, ?> uploaded = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "use_filename", true,
                            "unique_filename", true,
                            "overwrite", false
                    )
            );

            Object publicId = uploaded.get("public_id");
            Object secureUrl = uploaded.get("secure_url");
            Object format = uploaded.get("format");
            Object bytes = uploaded.get("bytes");
            Object assetFolder = uploaded.get("asset_folder");

            if (!(publicId instanceof String)
                    || !(secureUrl instanceof String)
                    || !(format instanceof String)
                    || !(bytes instanceof Number)) {
                throw new CloudinaryUploadException(
                        "Cloudinary devolvió una respuesta incompleta.",
                        null
                );
            }

            String resultFolder = assetFolder instanceof String
                    ? (String) assetFolder
                    : folder;

            return new CloudinaryUploadResult(
                    (String) publicId,
                    (String) secureUrl,
                    (String) format,
                    ((Number) bytes).longValue(),
                    resultFolder
            );

        } catch (CloudinaryUploadException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CloudinaryUploadException(
                    "No fue posible cargar la imagen en Cloudinary.",
                    ex
            );
        }
    }


    /** Elimina de Cloudinary una imagen que fue cargada y administrada por HostFlow. */
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("El publicId de Cloudinary es obligatorio.");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "invalidate", true
                    )
            );
            Object status = result.get("result");

            // "not found" se considera éxito idempotente: el recurso ya no existe en Cloudinary.
            if (!("ok".equals(status) || "not found".equals(status))) {
                throw new CloudinaryDeleteException(
                        "Cloudinary no confirmó la eliminación de la imagen (resultado: " + status + ").",
                        null
                );
            }
        } catch (CloudinaryDeleteException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CloudinaryDeleteException(
                    "No fue posible eliminar la imagen de Cloudinary.",
                    ex
            );
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new InvalidImageException(
                    "Debes seleccionar una imagen que no esté vacía."
            );
        }

        if (file.getSize() < MIN_IMAGE_SIZE_BYTES) {
            throw new InvalidImageException(
                    "La imagen debe pesar al menos 500 KB."
            );
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new InvalidImageException(
                    "La imagen no puede superar los 2 MB."
            );
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new InvalidImageException(
                    "El archivo debe ser una imagen."
            );
        }
    }
}
