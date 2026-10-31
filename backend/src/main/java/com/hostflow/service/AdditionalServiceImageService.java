package com.hostflow.service;


import com.hostflow.dto.response.AdditionalServiceImageResponse;
import com.hostflow.dto.response.CloudinaryUploadResult;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.AdditionalService;
import com.hostflow.model.AdditionalServiceImage;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.AdditionalServiceImageRepository;
import com.hostflow.repository.AdditionalServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Servicio para gestionar las imágenes de los servicios adicionales.
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Subir imágenes a Cloudinary con validación de tamaño (500 KB - 2 MB).</li>
 *   <li>Limitar a máximo 3 imágenes por servicio.</li>
 *   <li>Eliminar imágenes de Cloudinary y de la base de datos.</li>
 *   <li>Reemplazar imágenes existentes.</li>
 * </ul>
 *
 * <p><strong>Restricciones de imágenes:</strong></p>
 * <ul>
 *   <li>Tamaño mínimo: 500 KB (evita imágenes de baja calidad).</li>
 *   <li>Tamaño máximo: 2 MB (optimiza tiempos de carga).</li>
 *   <li>Tipo: solo archivos de imagen (image/*).</li>
 *   <li>Cantidad máxima: 3 imágenes por servicio.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdditionalServiceImageService {

    /** Tamaño mínimo permitido: 500 KB en bytes. */
    private static final long MIN_IMAGE_SIZE_BYTES = 500L * 1024;

    /** Tamaño máximo permitido: 2 MB en bytes. */
    private static final long MAX_IMAGE_SIZE_BYTES = 2L * 1024 * 1024;

    /** Cantidad máxima de imágenes por servicio. */
    private static final int MAX_IMAGES_PER_SERVICE = 3;

    private final AdditionalServiceRepository serviceRepository;
    private final AdditionalServiceImageRepository imageRepository;
    private final AccommodationRepository accommodationRepository;
    private final CloudinaryImageService cloudinaryImageService;

    /**
     * Lista todas las imágenes de un servicio adicional.
     *
     * @param ownerId   ID del propietario autenticado
     * @param serviceId ID del servicio adicional
     * @return lista de imágenes en formato DTO
     */
    @Transactional(readOnly = true)
    public List<AdditionalServiceImageResponse> listImages(Long ownerId, Long serviceId) {
        AdditionalService service = getServiceForOwner(ownerId, serviceId);
        return imageRepository.findAllByAdditionalService_IdOrderByOrderIndexAsc(service.getId())
                .stream()
                .map(AdditionalServiceImageResponse::from)
                .toList();
    }

    /**
     * Sube una nueva imagen para un servicio adicional.
     *
     * <p>Flujo:</p>
     * <ol>
     *   <li>Valida que el servicio pertenezca al propietario.</li>
     *   <li>Valida que no se exceda el límite de 3 imágenes.</li>
     *   <li>Valida el tamaño del archivo (500 KB - 2 MB).</li>
     *   <li>Sube la imagen a Cloudinary en la carpeta
     *       {@code hostflow/additional-services/{serviceId}}.</li>
     *   <li>Persiste el registro en la base de datos.</li>
     * </ol>
     *
     * @param ownerId   ID del propietario autenticado
     * @param serviceId ID del servicio adicional
     * @param file      archivo de imagen a subir
     * @return la imagen subida en formato DTO
     * @throws BusinessException si se excede el límite de imágenes o el tamaño
     */
    @Transactional
    public AdditionalServiceImageResponse uploadImage(Long ownerId, Long serviceId, MultipartFile file) {
        // 1. Validar que el servicio pertenezca al propietario
        AdditionalService service = getServiceForOwner(ownerId, serviceId);

        // 2. Validar límite de imágenes
        long currentCount = imageRepository.countByAdditionalService_Id(service.getId());
        if (currentCount >= MAX_IMAGES_PER_SERVICE) {
            throw new BusinessException(
                    String.format("El servicio ya tiene el máximo de %d imágenes permitidas.",
                            MAX_IMAGES_PER_SERVICE));
        }

        // 3. Validar el archivo
        validateImageFile(file);

        // 4. Subir a Cloudinary en carpeta específica del servicio
        String cloudinaryFolder = "hostflow/additional-services/" + service.getId();
        CloudinaryUploadResult uploaded = cloudinaryImageService.uploadImageToFolder(file, cloudinaryFolder);

        // 5. Crear y persistir el registro de la imagen
        AdditionalServiceImage image = AdditionalServiceImage.builder()
                .additionalService(service)
                .imageUrl(uploaded.secureUrl())
                .cloudinaryPublicId(uploaded.publicId())
                .cloudinaryFolder(cloudinaryFolder)
                .fileSizeBytes(uploaded.bytes())
                .mimeType(file.getContentType())
                .orderIndex((int) currentCount)
                .build();

        AdditionalServiceImage saved = imageRepository.save(image);

        log.info("🖼️ Imagen subida para servicio '{}' (ID: {}): {}",
                service.getName(), service.getId(), uploaded.secureUrl());

        return AdditionalServiceImageResponse.from(saved);
    }

    /**
     * Elimina una imagen de un servicio adicional.
     *
     * <p>Flujo:</p>
     * <ol>
     *   <li>Valida que la imagen pertenezca al servicio del propietario.</li>
     *   <li>Elimina el recurso de Cloudinary.</li>
     *   <li>Elimina el registro de la base de datos.</li>
     *   <li>Reordena las imágenes restantes (orderIndex).</li>
     * </ol>
     *
     * @param ownerId   ID del propietario autenticado
     * @param serviceId ID del servicio adicional
     * @param imageId   ID de la imagen a eliminar
     */
    @Transactional
    public void deleteImage(Long ownerId, Long serviceId, Long imageId) {
        // 1. Validar que el servicio pertenezca al propietario
        AdditionalService service = getServiceForOwner(ownerId, serviceId);

        // 2. Buscar la imagen
        AdditionalServiceImage image = imageRepository
                .findByIdAndAdditionalService_Id(imageId, service.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró esa imagen en el servicio adicional."));

        // 3. Eliminar de Cloudinary
        try {
            cloudinaryImageService.deleteImage(image.getCloudinaryPublicId());
        } catch (Exception e) {
            log.warn("⚠️ No se pudo eliminar la imagen de Cloudinary (publicId: {}). " +
                    "Se eliminará solo el registro de la BD.", image.getCloudinaryPublicId(), e);
        }

        // 4. Eliminar de la base de datos
        imageRepository.delete(image);

        // 5. Reordenar imágenes restantes
        reorderImages(service.getId());

        log.info("🗑️ Imagen eliminada del servicio '{}' (ID: {}): imageId={}",
                service.getName(), service.getId(), imageId);
    }

    /**
     * Reordena las imágenes de un servicio después de una eliminación.
     *
     * <p>Garantiza que los orderIndex sean consecutivos (0, 1, 2)
     * después de eliminar una imagen intermedia.</p>
     */
    private void reorderImages(Long serviceId) {
        List<AdditionalServiceImage> images = imageRepository
                .findAllByAdditionalService_IdOrderByOrderIndexAsc(serviceId);
        for (int i = 0; i < images.size(); i++) {
            images.get(i).setOrderIndex(i);
        }
        imageRepository.saveAll(images);
    }

    /**
     * Obtiene un servicio adicional validando que pertenezca al propietario.
     */
    private AdditionalService getServiceForOwner(Long ownerId, Long serviceId) {
        Accommodation accommodation = accommodationRepository.findByOwner_Id(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registra primero los datos generales del alojamiento."));

        return serviceRepository.findByIdAndAccommodation_Id(serviceId, accommodation.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ese servicio adicional en tu alojamiento."));
    }

    /**
     * Valida el archivo de imagen según las restricciones de negocio.
     *
     * @param file el archivo a validar
     * @throws BusinessException si el archivo no cumple las restricciones
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new BusinessException("Debes seleccionar una imagen que no esté vacía.");
        }

        // Validar tipo MIME
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessException("El archivo debe ser una imagen (JPEG, PNG, WEBP, etc.).");
        }

        // Validar tamaño mínimo (500 KB)
        if (file.getSize() < MIN_IMAGE_SIZE_BYTES) {
            throw new BusinessException(
                    String.format("La imagen es demasiado pequeña (%.1f KB). " +
                                    "El tamaño mínimo es 500 KB.",
                            file.getSize() / 1024.0));
        }

        // Validar tamaño máximo (2 MB)
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BusinessException(
                    String.format("La imagen supera el tamaño máximo permitido (%.1f MB). " +
                                    "El tamaño máximo es 2 MB.",
                            file.getSize() / (1024.0 * 1024.0)));
        }
    }
}