-- HostFlow: esquema base de propietarios, roles, perfil del alojamiento y galería.
-- Las tablas owners/roles/owner_roles deben existir antes de crear la FK de accommodations.
-- IF NOT EXISTS permite trabajar sobre una BD donde Hibernate ya creó estas tablas,
-- sin reemplazarlas ni borrar sus registros.

CREATE TABLE IF NOT EXISTS owners (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    accommodation_name VARCHAR(200) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS owner_roles (
    owner_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_owner_roles PRIMARY KEY (owner_id, role_id),
    CONSTRAINT fk_owner_roles_owner FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE,
    CONSTRAINT fk_owner_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_owner_roles_role ON owner_roles(role_id);

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

COMMENT ON TABLE owners IS 'Propietarios registrados en HostFlow';
COMMENT ON TABLE roles IS 'Roles de seguridad del sistema';
COMMENT ON TABLE owner_roles IS 'Relación muchos a muchos entre propietarios y roles';
COMMENT ON TABLE accommodations IS 'Datos generales, capacidad, precio y datos de contacto/comerciales del alojamiento';
COMMENT ON TABLE accommodation_images IS 'Galería de imágenes del alojamiento; almacena URLs, no archivos binarios';
