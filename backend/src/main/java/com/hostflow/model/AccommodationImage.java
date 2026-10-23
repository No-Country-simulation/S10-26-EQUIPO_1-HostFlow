package com.hostflow.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "accommodation_images", indexes = @Index(name = "idx_accommodation_images_accommodation", columnList = "accommodation_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccommodationImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_accommodation_images_accommodation"))
    private Accommodation accommodation;
    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
