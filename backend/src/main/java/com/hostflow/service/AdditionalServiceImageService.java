package com.hostflow.service;

import com.hostflow.dto.response.AdditionalServiceImageResponse;
import com.hostflow.dto.response.CloudinaryUploadResult;
import com.hostflow.exception.BusinessException;
import com.hostflow.exception.ResourceNotFoundException;
import com.hostflow.model.Accommodation;
import com.hostflow.model.AdditionalService;
import com.hostflow.model.AdditionalServiceImage;
import com.hostflow.model.Owner;
import com.hostflow.repository.AccommodationRepository;
import com.hostflow.repository.AdditionalServiceImageRepository;
import com.hostflow.repository.AdditionalServiceRepository;
import com.hostflow.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Servicio para gestionar las imágenes de los servicios adicionales.
 *
 * <p><strong>🆕 Mejora de arquitectura (v2.1):</strong></p>
 * <p>Se agregaron métodos que aceptan el {@code ownerEmail} en lugar del {@code ownerId},
 * delegando la resolución del Owner y Accommodation al servicio y eliminando la necesidad
 * de que los controladores inyecten repositorios directamente.</p>
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Subir imágenes a Cloudinary (límites de tamaño en app.images.* vía CloudinaryImageService).</li>
 *   <li>Limitar a máximo 3 imágenes por servicio.</li>
 *   <li>Eliminar imágenes de Cloudinary y de la base de datos.</li>
 *   <li>Reemplazar imágenes existentes.</li>
 * </ul>
 *
 * <p><strong>Restricciones de imágenes:</strong></p>
 * <ul>
 *   <li>Tamaño máximo configurable en app.images.max-size-bytes (2 MB por defecto).</li>
 *   <li>Se rechazan archivos vacíos.</li>
 *   <li>Tipo: solo archivos de imagen (image/*).</li>
 *   <li>Cantidad máxima: 3 imágenes por servicio.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdditionalServiceImageService {



    /** Cantidad máxima de imágenes por servicio. */
    private static final int MAX_IMAGES_PER_SERVICE = 3;

    private final AdditionalServiceRepository serviceRepository;
    private final AdditionalServiceImageRepository imageRepository;
    private final AccommodationRepository accommodationRepository;
    private final OwnerRepository ownerRepository; // 🆕 Agregado para resolver por email
    private final CloudinaryImageService cloudinaryImageService;

    // =========================================================================
    // 🆕 MÉTODOS PÚBLICOS QUE ACEPTAN EMAIL (Para controladores refactorizados)
    // =========================================================================

    /**
     * Lista todas las imágenes de un servicio adicional, resolviendo el propietario por email.
     *
     * @param ownerEmail Email del propietario autenticado
     * @param serviceId  ID del servicio adicional
     * @return lista de imágenes en formato DTO
     */
    @Transactional(readOnly = true)
    public List<AdditionalServiceImageResponse> listImagesByEmail(String ownerEmail, Long serviceId) {
        AdditionalService service = getServiceForOwnerByEmail(ownerEmail, serviceId);
        return imageRepository.findAllByAdditionalService_IdOrderByOrderIndexAsc(service.getId())
                .stream()
                .map(AdditionalServiceImageResponse::from)
                .toList();
    }

    /**
     * Sube una nueva imagen para un servicio adicional, resolviendo el propietario por email.
     *
     * @param ownerEmail Email del propietario autenticado
     * @param serviceId  ID del servicio adicional
     * @param file       archivo de imagen a subir
     * @return la imagen subida en formato DTO
     */
    @Transactional
    public AdditionalServiceImageResponse uploadImageByEmail(String ownerEmail, Long serviceId, MultipartFile file) {
        AdditionalService service = getServiceForOwnerByEmail(ownerEmail, serviceId);
        return uploadImageInternal(service, file);
    }

    /**
     * Elimina una imagen de un servicio adicional, resolviendo el propietario por email.
     *
     * @param ownerEmail Email del propietario autenticado
     * @param serviceId  ID del servicio adicional
     * @param imageId    ID de la imagen a eliminar
     */
    @Transactional
    public void deleteImageByEmail(String ownerEmail, Long serviceId, Long imageId) {
        AdditionalService service = getServiceForOwnerByEmail(ownerEmail, serviceId);
        deleteImageInternal(service, imageId);
    }

    // =========================================================================
    // MÉTODOS ORIGINALES (Se mantienen para compatibilidad con Tests)
    // =========================================================================

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
     * @param ownerId   ID del propietario autenticado
     * @param serviceId ID del servicio adicional
     * @param file      archivo de imagen a subir
     * @return la imagen subida en formato DTO
     */
    @Transactional
    public AdditionalServiceImageResponse uploadImage(Long ownerId, Long serviceId, MultipartFile file) {
        AdditionalService service = getServiceForOwner(ownerId, serviceId);
        return uploadImageInternal(service, file);
    }

    /**
     * Elimina una imagen de un servicio adicional.
     *
     * @param ownerId   ID del propietario autenticado
     * @param serviceId ID del servicio adicional
     * @param imageId   ID de la imagen a eliminar
     */
    @Transactional
    public void deleteImage(Long ownerId, Long serviceId, Long imageId) {
        AdditionalService service = getServiceForOwner(ownerId, serviceId);
        deleteImageInternal(service, imageId);
    }

    // =========================================================================
    // LÓGICA DE NEGOCIO INTERNA (Reutilizable - Principio DRY)
    // =========================================================================

    /**
     * Lógica interna para subir una imagen a un servicio adicional.
     *
     * @param service el servicio adicional al que pertenece la imagen
     * @param file    archivo de imagen a subir
     * @return la imagen subida en formato DTO
     */
    private AdditionalServiceImageResponse uploadImageInternal(AdditionalService service, MultipartFile file) {
        // 1. Validar límite de imágenes
        long currentCount = imageRepository.countByAdditionalService_Id(service.getId());
        if (currentCount >= MAX_IMAGES_PER_SERVICE) {
            throw new BusinessException(
                    String.format("El servicio ya tiene el máximo de %d imágenes permitidas.", MAX_IMAGES_PER_SERVICE));
        }

        // 2. Validar el archivo
        validateImageFile(file);

        // 3. Subir a Cloudinary en carpeta específica del servicio
        String cloudinaryFolder = "hostflow/additional-services/" + service.getId();
        CloudinaryUploadResult uploaded = cloudinaryImageService.uploadImageToFolder(file, cloudinaryFolder);

        // 4. Crear y persistir el registro de la imagen
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
     * Lógica interna para eliminar una imagen de un servicio adicional.
     *
     * @param service el servicio adicional al que pertenece la imagen
     * @param imageId ID de la imagen a eliminar
     */
    private void deleteImageInternal(AdditionalService service, Long imageId) {
        // 1. Buscar la imagen
        AdditionalServiceImage image = imageRepository
                .findByIdAndAdditionalService_Id(imageId, service.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró esa imagen en el servicio adicional."));

        // 2. Eliminar de Cloudinary
        try {
            cloudinaryImageService.deleteImage(image.getCloudinaryPublicId());
        } catch (Exception e) {
            log.warn("⚠️ No se pudo eliminar la imagen de Cloudinary (publicId: {}). " +
                    "Se eliminará solo el registro de la BD.", image.getCloudinaryPublicId(), e);
        }

        // 3. Eliminar de la base de datos
        imageRepository.delete(image);

        // 4. Reordenar imágenes restantes
        reorderImages(service.getId());

        log.info("🗑️ Imagen eliminada del servicio '{}' (ID: {}): imageId={}",
                service.getName(), service.getId(), imageId);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Reordena las imágenes de un servicio después de una eliminación.
     *
     * <p>Garantiza que los orderIndex sean consecutivos (0, 1, 2)
     * después de eliminar una imagen intermedia.</p>
     *
     * @param serviceId ID del servicio adicional
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
     * Obtiene un servicio adicional validando que pertenezca al propietario por ID.
     *
     * @param ownerId   ID del propietario
     * @param serviceId ID del servicio adicional
     * @return la entidad AdditionalService
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
     * 🆕 Obtiene un servicio adicional validando que pertenezca al propietario por Email.
     * Centraliza la lógica de búsqueda para evitar duplicación en los controladores.
     *
     * @param ownerEmail Email del propietario
     * @param serviceId  ID del servicio adicional
     * @return la entidad AdditionalService
     */
    private AdditionalService getServiceForOwnerByEmail(String ownerEmail, Long serviceId) {
        Owner owner = ownerRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el propietario con email: " + ownerEmail));

        Accommodation accommodation = accommodationRepository.findByOwner_Id(owner.getId())
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

        // El tamaño (min/max) lo valida CloudinaryImageService con app.images.*-size-bytes
    }
}
