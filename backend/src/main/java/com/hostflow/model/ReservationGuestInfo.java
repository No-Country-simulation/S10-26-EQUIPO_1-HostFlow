package com.hostflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Datos personales y preferencias declarados para una estadía concreta. */
@Entity
@Table(name = "reservation_guest_info",
        uniqueConstraints = @UniqueConstraint(name = "uk_reservation_guest_info_reservation", columnNames = "reservation_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationGuestInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_reservation_guest_info_reservation"))
    private Reservation reservation;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "document_number", nullable = false, length = 40)
    private String documentNumber;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "phone", nullable = false, length = 40)
    private String phone;

    @Column(name = "preferences", length = 2000)
    private String preferences;

    @ElementCollection
    @CollectionTable(name = "reservation_guest_allergies",
            joinColumns = @JoinColumn(name = "reservation_guest_info_id"))
    @Column(name = "allergy_name", nullable = false, length = 100)
    @Builder.Default
    private List<String> allergies = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
