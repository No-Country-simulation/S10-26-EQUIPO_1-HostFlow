-- HostFlow: perfil general del alojamiento y galería de imágenes.
-- La tabla accommodations es independiente de accommodation_temporary_disables:
-- editar datos del perfil no activa ni desactiva la disponibilidad.

CREATE TABLE IF NOT EXISTS accommodations (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    room_count INTEGER,
    bathroom_count INTEGER,
    guest_capacity INTEGER,
    base_price NUMERIC(12,2),
    logo_url VARCHAR(2048),
    primary_color VARCHAR(20),
    welcome_message TEXT,
    email VARCHAR(150),
    phone VARCHAR(40),
    address TEXT,
    bank_name VARCHAR(150),
    bank_account_holder VARCHAR(200),
    bank_account_number VARCHAR(100),
    bank_cbu_cvu VARCHAR(22),
    bank_alias VARCHAR(100),
    bank_cuit VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accommodations_owner FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE
);

-- Mantiene compatible una tabla accommodations preexistente creada desde el SQL propuesto.
ALTER TABLE accommodations ADD COLUMN IF NOT EXISTS room_count INTEGER;
ALTER TABLE accommodations ADD COLUMN IF NOT EXISTS bathroom_count INTEGER;
ALTER TABLE accommodations ADD COLUMN IF NOT EXISTS guest_capacity INTEGER;
ALTER TABLE accommodations ADD COLUMN IF NOT EXISTS base_price NUMERIC(12,2);
ALTER TABLE accommodations ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS accommodation_images (
    id BIGSERIAL PRIMARY KEY,
    accommodation_id BIGINT NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accommodation_images_accommodation
        FOREIGN KEY (accommodation_id) REFERENCES accommodations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_accommodation_images_accommodation
    ON accommodation_images(accommodation_id);

COMMENT ON TABLE accommodations IS 'Datos generales, capacidad, precio y datos de contacto/comerciales del alojamiento';
COMMENT ON TABLE accommodation_images IS 'Galería de imágenes del alojamiento; almacena URLs, no archivos binarios';
