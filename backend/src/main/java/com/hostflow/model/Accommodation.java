package com.hostflow.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accommodations", uniqueConstraints = @UniqueConstraint(name = "uk_accommodations_owner_id", columnNames = "owner_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Accommodation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_accommodations_owner"))
    private Owner owner;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "room_count") private Integer roomCount;
    @Column(name = "bathroom_count") private Integer bathroomCount;
    @Column(name = "guest_capacity") private Integer guestCapacity;
    @Column(name = "base_price", precision = 12, scale = 2) private BigDecimal basePrice;

    @Column(name = "logo_url", length = 2048) private String logoUrl;
    @Column(name = "primary_color", length = 20) private String primaryColor;
    @Column(name = "welcome_message", columnDefinition = "TEXT") private String welcomeMessage;
    @Column(length = 150) private String email;
    @Column(length = 40) private String phone;
    @Column(columnDefinition = "TEXT") private String address;
    @Column(name = "bank_name", length = 150) private String bankName;
    @Column(name = "bank_account_holder", length = 200) private String bankAccountHolder;
    @Column(name = "bank_account_number", length = 100) private String bankAccountNumber;
    @Column(name = "bank_cbu_cvu", length = 22) private String bankCbuCvu;
    @Column(name = "bank_alias", length = 100) private String bankAlias;
    @Column(name = "bank_cuit", length = 20) private String bankCuit;

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    @Builder.Default
    private List<AccommodationImage> images = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
