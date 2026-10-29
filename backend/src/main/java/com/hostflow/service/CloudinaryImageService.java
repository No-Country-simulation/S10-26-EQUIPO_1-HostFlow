package com.hostflow.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hostflow.config.CloudinaryProperties;
import com.hostflow.dto.response.CloudinaryUploadResult;
import com.hostflow.exception.CloudinaryDeleteException;
import com.hostflow.exception.CloudinaryUploadException;
import com.hostflow.exception.InvalidImageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Servicio centralizado para la gestión de imágenes en Cloudinary.
 *
 * <p>Proporciona operaciones seguras y validadas para subir, eliminar y gestionar
 * recursos multimedia en la nube, garantizando el cumplimiento de las políticas
 * de tamaño y tipo de archivo de la plataforma HostFlow.</p>
 *
 * <p><strong>Características principales:</strong></p>
 * <ul>
 *   <li>Validación estricta de archivos (tamaño máximo 5 MB, tipo MIME de imagen).</li>
 *   <li>Soporte para subida a carpetas predeterminadas o personalizadas.</li>
 *   <li>Eliminación segura de recursos con manejo idempotente de errores.</li>
 *   <li>Manejo robusto de excepciones específicas de Cloudinary.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@Service
@Slf4j
public class CloudinaryImageService {

    /**
     * Tamaño máximo permitido para una imagen: 5 Megabytes (5 * 1024 * 1024 bytes).
     *
     * <p>Este límite se aplica tanto a la validación local como a la configuración
     * de Cloudinary para rechazar archivos pesados antes de consumir ancho de banda.</p>
     */
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    /**
     * Cliente de Cloudinary inyectado por Spring.
     * Configurado con las credenciales desde {@link CloudinaryProperties}.
     */
    private final Cloudinary cloudinary;

    /**
     * Carpeta predeterminada en Cloudinary para las subidas generales
     * (ej: galería del alojamiento, logotipos).
     */
    private final String defaultFolder;

    /**
     * Constructor del servicio.
     *
     * @param cloudinary el cliente configurado de Cloudinary
     * @param properties las propiedades de configuración de Cloudinary
     */
    public CloudinaryImageService(
            Cloudinary cloudinary,
            CloudinaryProperties properties) {
        this.cloudinary = cloudinary;
        this.defaultFolder = properties.getFolder();
    }

    /**
     * Sube una imagen a Cloudinary utilizando la carpeta predeterminada configurada.
     *
     * <p>Este método es ideal para recursos generales como la galería del alojamiento
     * o el logotipo, donde no se requiere una estructura de carpetas anidada específica.</p>
     *
     * @param file el archivo multipart a subir
     * @return el resultado de la subida con URL, publicId, formato y tamaño
     * @throws InvalidImageException    si el archivo no cumple con las validaciones
     * @throws CloudinaryUploadException si falla la comunicación o respuesta de Cloudinary
     */
    public CloudinaryUploadResult uploadImage(MultipartFile file) {
        return uploadImageToFolder(file, defaultFolder);
    }

    /**
     * 🆕 Sube una imagen a Cloudinary en una carpeta específica.
     *
     * <p>Este método permite una organización granular de los recursos en la nube,
     * creando estructuras como {@code hostflow/additional-services/{serviceId}}
     * para mantener los archivos relacionados juntos y facilitar su gestión o limpieza.</p>
     *
     * <p><strong>Parámetros de subida configurados:</strong></p>
     * <ul>
     *   <li>{@code folder}: la ruta de la carpeta destino en Cloudinary.</li>
     *   <li>{@code resource_type}: "image" para forzar el tratamiento como imagen.</li>
     *   <li>{@code use_filename}: true, para mantener el nombre original como base.</li>
     *   <li>{@code unique_filename}: true, para evitar colisiones agregando un hash.</li>
     *   <li>{@code overwrite}: false, para no sobrescribir accidentalmente archivos existentes.</li>
     * </ul>
     *
     * @param file   el archivo multipart a subir
     * @param folder la carpeta específica de Cloudinary donde se almacenará el recurso
     * @return el resultado de la subida con URL, publicId, formato y tamaño
     * @throws InvalidImageException    si el archivo no cumple con las validaciones
     * @throws CloudinaryUploadException si falla la comunicación o respuesta de Cloudinary
     */
    public CloudinaryUploadResult uploadImageToFolder(MultipartFile file, String folder) {
        // 1. Validar el archivo localmente antes de consumir recursos de red
        validateImage(file);

        try {
            log.debug("📤 Subiendo imagen a Cloudinary en carpeta: {}", folder);

            // 2. Ejecutar la subida a Cloudinary
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

            // 3. Extraer y validar los metadatos de la respuesta
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
                        "Cloudinary devolvió una respuesta incompleta o inesperada.",
                        null
                );
            }

            // 4. Determinar la carpeta real asignada (Cloudinary a veces la normaliza)
            String resultFolder = assetFolder instanceof String
                    ? (String) assetFolder
                    : folder;

            log.debug("✅ Imagen subida exitosamente. Public ID: {}", publicId);

            // 5. Construir y retornar el objeto de resultado
            return new CloudinaryUploadResult(
                    (String) publicId,
                    (String) secureUrl,
                    (String) format,
                    ((Number) bytes).longValue(),
                    resultFolder
            );

        } catch (CloudinaryUploadException ex) {
            // Relanzar excepciones ya manejadas
            throw ex;
        } catch (Exception ex) {
            // Envolver cualquier otra excepción (ej: IOException, CloudinaryException)
            log.error("❌ Error inesperado al subir imagen a Cloudinary", ex);
            throw new CloudinaryUploadException(
                    "No fue posible cargar la imagen en el servicio de almacenamiento en la nube.",
                    ex
            );
        }
    }

    /**
     * Elimina de Cloudinary una imagen que fue cargada y administrada por HostFlow.
     *
     * <p>Utiliza el {@code publicId} del recurso para identificarlo de forma única
     * en la plataforma de Cloudinary y proceder con su eliminación permanente.</p>
     *
     * <p><strong>Comportamiento idempotente:</strong> Si el recurso ya no existe
     * en Cloudinary (estado "not found"), el método se considera exitoso, ya que
     * el objetivo final (que el recurso no exista) se ha cumplido.</p>
     *
     * @param publicId el identificador público único del recurso en Cloudinary
     * @throws IllegalArgumentException si el {@code publicId} es nulo o vacío
     * @throws CloudinaryDeleteException si falla la comunicación o Cloudinary rechaza la eliminación
     */
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("El publicId de Cloudinary es obligatorio para eliminar una imagen.");
        }

        try {
            log.debug("🗑️ Eliminando imagen de Cloudinary. Public ID: {}", publicId);

            Map<?, ?> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "invalidate", true // Invalida la URL en la CDN de Cloudinary inmediatamente
                    )
            );

            Object status = result.get("result");

            // "ok" = eliminado exitosamente
            // "not found" = ya no existía (éxito idempotente)
            if (!("ok".equals(status) || "not found".equals(status))) {
                throw new CloudinaryDeleteException(
                        "Cloudinary no confirmó la eliminación de la imagen (resultado: " + status + ").",
                        null
                );
            }

            log.debug("✅ Imagen eliminada exitosamente de Cloudinary.");

        } catch (CloudinaryDeleteException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("❌ Error inesperado al eliminar imagen de Cloudinary", ex);
            throw new CloudinaryDeleteException(
                    "No fue posible eliminar la imagen del servicio de almacenamiento en la nube.",
                    ex
            );
        }
    }

    /**
     * Valida que el archivo multipart cumpla con las políticas de seguridad y negocio.
     *
     * <p>Validaciones realizadas:</p>
     * <ol>
     *   <li>El archivo no debe ser nulo ni estar vacío.</li>
     *   <li>El tamaño no debe exceder los 5 MB ({@link #MAX_IMAGE_SIZE_BYTES}).</li>
     *   <li>El tipo MIME debe comenzar con "image/" (ej: image/jpeg, image/png).</li>
     * </ol>
     *
     * @param file el archivo a validar
     * @throws InvalidImageException si el archivo no cumple con alguna de las validaciones
     */
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new InvalidImageException(
                    "Debes seleccionar una imagen que no esté vacía."
            );
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new InvalidImageException(
                    String.format("La imagen no puede superar el tamaño máximo permitido de %.1f MB.",
                            MAX_IMAGE_SIZE_BYTES / (1024.0 * 1024.0))
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new InvalidImageException(
                    "El archivo seleccionado debe ser una imagen válida (JPEG, PNG, WEBP, etc.)."
            );
        }
    }
}