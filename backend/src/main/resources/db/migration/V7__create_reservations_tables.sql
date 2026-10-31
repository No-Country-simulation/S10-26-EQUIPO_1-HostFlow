-- ============================================================================
-- Migración V6: Creación de tablas para el sistema de reservas
-- ============================================================================
-- Esta migración crea las tablas necesarias para gestionar:
-- - Huéspedes (guests)
-- - Métodos de pago de huéspedes (guest_payment_methods)
-- - Reservas (reservations)
-- - Enlaces de confirmación (reservation_links)
--
-- @author HostFlow Team
-- @version 1.0.0
-- ============================================================================

-- ============================================================================
-- 1. TABLA: guests (Huéspedes)
-- ============================================================================
CREATE TABLE guests (
                        id BIGSERIAL PRIMARY KEY,
                        full_name VARCHAR(200) NOT NULL,
                        email VARCHAR(150) NOT NULL UNIQUE,
                        phone VARCHAR(40),
                        document_number VARCHAR(50),
                        is_active BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para optimizar búsquedas
CREATE INDEX idx_guests_email ON guests(email);
CREATE INDEX idx_guests_full_name ON guests(full_name);

-- ============================================================================
-- 2. TABLA: guest_payment_methods (Métodos de pago de huéspedes)
-- ============================================================================
CREATE TABLE guest_payment_methods (
                                       id BIGSERIAL PRIMARY KEY,
                                       guest_id BIGINT NOT NULL,
                                       payment_type VARCHAR(30) NOT NULL,
                                       display_name VARCHAR(120) NOT NULL,
                                       provider_name VARCHAR(100),
                                       account_number VARCHAR(100),
                                       account_holder VARCHAR(200),
                                       is_default BOOLEAN NOT NULL DEFAULT FALSE,
                                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                       expiration_date VARCHAR(10),
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_guest_payment_methods_guest
                                           FOREIGN KEY (guest_id)
                                               REFERENCES guests(id)
                                               ON DELETE CASCADE
);

-- Índices
CREATE INDEX idx_guest_payment_methods_guest ON guest_payment_methods(guest_id);
CREATE INDEX idx_guest_payment_methods_default ON guest_payment_methods(guest_id, is_default);

-- ============================================================================
-- 3. TABLA: reservations (Reservas)
-- ============================================================================
CREATE TABLE reservations (
                              id BIGSERIAL PRIMARY KEY,
                              confirmation_code VARCHAR(20) NOT NULL UNIQUE,
                              accommodation_id BIGINT NOT NULL,
                              guest_id BIGINT NOT NULL,
                              payment_method_id BIGINT,
                              check_in TIMESTAMP NOT NULL,
                              check_out TIMESTAMP NOT NULL,
                              nights INTEGER NOT NULL,
                              price_per_night DECIMAL(12, 2) NOT NULL,
                              subtotal DECIMAL(12, 2) NOT NULL,
                              taxes DECIMAL(12, 2) DEFAULT 0,
                              total_amount DECIMAL(12, 2) NOT NULL,
                              status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                              guest_count INTEGER NOT NULL DEFAULT 1,
                              special_requests TEXT,
                              cancellation_reason VARCHAR(500),
                              cancelled_at TIMESTAMP,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_reservations_accommodation
                                  FOREIGN KEY (accommodation_id)
                                      REFERENCES accommodations(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_reservations_guest
                                  FOREIGN KEY (guest_id)
                                      REFERENCES guests(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_reservations_payment_method
                                  FOREIGN KEY (payment_method_id)
                                      REFERENCES guest_payment_methods(id)
                                      ON DELETE SET NULL,

                              CONSTRAINT chk_reservation_status
                                  CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'COMPLETED')),

                              CONSTRAINT chk_reservation_nights
                                  CHECK (nights > 0),

                              CONSTRAINT chk_reservation_dates
                                  CHECK (check_out > check_in)
);

-- Índices críticos para rendimiento
CREATE INDEX idx_reservations_accommodation ON reservations(accommodation_id);
CREATE INDEX idx_reservations_guest ON reservations(guest_id);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_dates ON reservations(check_in, check_out);
CREATE INDEX idx_reservations_confirmation_code ON reservations(confirmation_code);

-- Índice compuesto para verificación rápida de disponibilidad
CREATE INDEX idx_reservations_availability
    ON reservations(accommodation_id, status, check_in, check_out)
    WHERE status IN ('PENDING', 'CONFIRMED');

-- ============================================================================
-- 4. TABLA: reservation_links (Enlaces de confirmación)
-- ============================================================================
CREATE TABLE reservation_links (
                                   id BIGSERIAL PRIMARY KEY,
                                   reservation_id BIGINT NOT NULL UNIQUE,
                                   token VARCHAR(100) NOT NULL UNIQUE,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   expires_at TIMESTAMP NOT NULL,
                                   used BOOLEAN NOT NULL DEFAULT FALSE,
                                   used_at TIMESTAMP,
                                   reactivation_count INTEGER NOT NULL DEFAULT 0,
                                   last_reactivated_at TIMESTAMP,
                                   confirmation_url VARCHAR(2048),

                                   CONSTRAINT fk_reservation_links_reservation
                                       FOREIGN KEY (reservation_id)
                                           REFERENCES reservations(id)
                                           ON DELETE CASCADE
);

-- Índices
CREATE INDEX idx_reservation_links_token ON reservation_links(token);
CREATE INDEX idx_reservation_links_reservation ON reservation_links(reservation_id);
CREATE INDEX idx_reservation_links_expires ON reservation_links(expires_at);

-- ============================================================================
-- FIN DE LA MIGRACIÓN
-- ============================================================================