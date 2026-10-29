package com.hostflow.model;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa una imagen asociada a un servicio adicional.
 *
 * <p>Cada servicio adicional puede tener hasta 3 imágenes, almacenadas
 * en Cloudinary dentro de la carpeta {@code hostflow/additional-services/{serviceId}/}.
 * Las imágenes se suben mediante el endpoint
 * {@code POST /owners/accommodation/additional-services/{serviceId}/images}.</p>
 *
 * <p><strong>Restricciones de negocio:</strong></p>
 * <ul>
 *   <li>Máximo 3 imágenes por servicio (validado en el servicio).</li>
 *   <li>Tamaño mínimo: 500 KB (evita imágenes de baja calidad).</li>
 *   <li>Tamaño máximo: 2 MB (optimiza tiempos de carga).</li>
 *   <li>Solo se aceptan archivos de tipo imagen (image/*).</li>
 * </ul>
 *
 * <p><strong>Relaciones:</strong></p>
 * <ul>
 *   <li>ManyToOne con {@link AdditionalService} (un servicio tiene muchas imágenes).</li>
 *   <li>Si se elimina el servicio, las imágenes se eliminan en cascada.</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 * @see AdditionalService
 */
@Entity
@Table(name = "additional_service_images",
        indexes = {
                @Index(name = "idx_additional_service_images_service",
                        columnList = "additional_service_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalServiceImage {

    /** Identificador único de la imagen (clave primaria). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Servicio adicional al que pertenece esta imagen.
     *
     * <p>Relación ManyToOne obligatoria. Si el servicio se elimina,
     * la imagen también se elimina (cascade = ALL, orphanRemoval = true
     * en el lado del servicio).</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "additional_service_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_additional_service_images_service"))
    private AdditionalService additionalService;

    /**
     * URL HTTPS pública de la imagen en Cloudinary.
     *
     * <p>Ejemplo: {@code https://res.cloudinary.com/vhuspscf/image/upload/v1234/hostflow/additional-services/5/img1.jpg}</p>
     */
    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    /**
     * Identificador público del recurso en Cloudinary.
     *
     * <p>Se utiliza para eliminar la imagen de Cloudinary cuando
     * el propietario la borra o elimina el servicio.</p>
     *
     * <p>Ejemplo: {@code hostflow/additional-services/5/abc123xyz}</p>
     */
    @Column(name = "cloudinary_public_id", nullable = false, length = 500)
    private String cloudinaryPublicId;

    /**
     * Carpeta de Cloudinary donde se almacenó la imagen.
     *
     * <p>Por defecto: {@code hostflow/additional-services/{serviceId}}.</p>
     */
    @Column(name = "cloudinary_folder", length = 255)
    private String cloudinaryFolder;

    /**
     * Tamaño del archivo en bytes.
     *
     * <p>Se utiliza para validar las restricciones de tamaño
     * (500 KB ≤ size ≤ 2 MB) antes de subir a Cloudinary.</p>
     */
    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    /**
     * Tipo MIME del archivo (ej: {@code image/jpeg}, {@code image/png}).
     */
    @Column(name = "mime_type", length = 50)
    private String mimeType;

    /**
     * Orden de la imagen dentro del servicio (0, 1, 2).
     *
     * <p>La primera imagen subida tiene order=0, la segunda order=1, etc.
     * Se utiliza para mostrar las imágenes en el orden en que fueron subidas.</p>
     */
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    /** Fecha y hora de creación del registro. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}