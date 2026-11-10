package com.hostflow.model;

import com.hostflow.enums.Currency;
import com.hostflow.enums.PaymentStatus;
import com.hostflow.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Reserva registrada por el propietario. Las fechas se bloquean desde su creación,
 * independientemente de que el huésped haya completado el onboarding o de que el
 * enlace enviado por correo haya vencido.
 */
@Entity
@Table(name = "reservations",
        indexes = {
                @Index(name = "idx_reservations_accommodation", columnList = "accommodation_id"),
                @Index(name = "idx_reservations_guest", columnList = "guest_id"),
                @Index(name = "idx_reservations_status", columnList = "status"),
                @Index(name = "idx_reservations_dates", columnList = "check_in_date, check_out_date"),
                @Index(name = "idx_reservations_confirmation_code", columnList = "confirmation_code"),
                @Index(name = "idx_reservations_payment_status", columnList = "payment_status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código público de referencia de la reserva. */
    @Column(name = "confirmation_code", nullable = false, unique = true, length = 20)
    private String confirmationCode;

    // Relaciones principales
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservations_accommodation"))
    private Accommodation accommodation;

    /** Se vincula al perfil Guest cuando el onboarding se completa y valida. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id",
            foreignKey = @ForeignKey(name = "fk_reservations_guest"))
    private Guest guest;

    // Contacto preliminar informado por el propietario al crear la reserva
    @Column(name = "guest_name", nullable = false, length = 200)
    private String guestName;

    @Column(name = "guest_email", nullable = false, length = 150)
    private String guestEmail;

    @Column(name = "guest_phone", length = 40)
    private String guestPhone;

    // Enlaces: se conserva un registro por cada emisión/renovación
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("createdAt DESC, id DESC")
    @Builder.Default
    private List<ReservationLink> links = new ArrayList<>();

    // Datos completos declarados durante el onboarding (máximo uno por reserva)
    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ReservationGuestInfo guestInfo;

    // Detalles de servicios adicionales y precios congelados para esta reserva
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReservationAdditionalService> additionalServices = new ArrayList<>();

    // Estadía
    @Column(name = "check_in_date", nullable = false)
    private LocalDateTime checkIn;

    @Column(name = "check_out_date", nullable = false)
    private LocalDateTime checkOut;

    @Column(name = "nights", nullable = false)
    private Integer nights;

    @Column(name = "number_of_guests", nullable = false)
    @Builder.Default
    private Integer guestCount = 1;

    // Importes: se guarda la moneda y los precios aplicados al momento de reservar
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private Currency currency = Currency.ARS;

    @Column(name = "price_per_night", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerNight;

    /** Subtotal del alojamiento: noches × precio por noche. */
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "additional_services_total", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal additionalServicesTotal = BigDecimal.ZERO;

    /** Total final; no incluye impuestos automáticos no definidos por el requisito. */
    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // Estado operativo de la reserva
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    // Estado de pago separado del estado de la reserva
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_marked_by_owner_id",
            foreignKey = @ForeignKey(name = "fk_reservations_payment_marked_by_owner"))
    private Owner paymentMarkedBy;

    // Servicios y observaciones
    @Column(name = "boat_transfer_requested", nullable = false)
    @Builder.Default
    private Boolean boatTransferRequested = false;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Compatibilidad transitoria con reservas creadas por el servicio anterior. */
    @PrePersist
    void prepareForPersistence() {
        if (guest != null) {
            if (guestName == null || guestName.isBlank()) guestName = guest.getFullName();
            if (guestEmail == null || guestEmail.isBlank()) guestEmail = guest.getEmail();
            if (guestPhone == null) guestPhone = guest.getPhone();
        }
        if (currency == null && accommodation != null && accommodation.getBaseCurrency() != null) {
            currency = accommodation.getBaseCurrency();
        }
        if (additionalServicesTotal == null) additionalServicesTotal = BigDecimal.ZERO;
        if (paymentStatus == null) paymentStatus = PaymentStatus.PENDING;
        if (boatTransferRequested == null) boatTransferRequested = false;
        if (nights == null && checkIn != null && checkOut != null) nights = calculateNights();
    }

    /** Número de noches redondeado por días calendario, no por bloques de 24 horas. */
    public int calculateNights() {
        if (checkIn == null || checkOut == null) {
            return 0;
        }
        return Math.toIntExact(ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate()));
    }

    /** Una reserva bloquea fechas mientras esté pendiente o confirmada. */
    public boolean isActive() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED;
    }

    /** Regla de intervalos semiabiertos: check-out puede coincidir con otro check-in. */
    public boolean overlapsWith(LocalDateTime newCheckIn, LocalDateTime newCheckOut) {
        return checkIn != null && checkOut != null && newCheckIn != null && newCheckOut != null
                && checkIn.isBefore(newCheckOut) && checkOut.isAfter(newCheckIn);
    }

    /** Mantiene la relación bidireccional al añadir un enlace histórico. */
    public void addLink(ReservationLink link) {
        if (link == null) return;
        links.add(link);
        link.setReservation(this);
    }

    public void setGuestInfo(ReservationGuestInfo guestInfo) {
        this.guestInfo = guestInfo;
        if (guestInfo != null) guestInfo.setReservation(this);
    }

    public void addAdditionalService(ReservationAdditionalService service) {
        if (service == null) return;
        additionalServices.add(service);
        service.setReservation(this);
    }

    /*
     * Compatibilidad temporal con consumidores existentes mientras se adapta la API.
     * Estos campos NO se persisten y no forman parte del nuevo modelo de dominio.
     * Deben eliminarse al actualizar ReservationService y ReservationResponse.
     */
    @Deprecated
    @Transient
    private GuestPaymentMethod paymentMethod;

    @Deprecated
    @Transient
    public BigDecimal getTaxes() {
        return BigDecimal.ZERO;
    }
}
