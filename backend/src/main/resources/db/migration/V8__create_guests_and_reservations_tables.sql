-- Versión optimizada: crea guests con todas las columnas finales
CREATE TABLE IF NOT EXISTS guests (
                                      id BIGSERIAL PRIMARY KEY,
                                      first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(40),
    document_number VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_guests_email ON guests(email);
CREATE UNIQUE INDEX IF NOT EXISTS idx_guests_document_number_unique
    ON guests(document_number) WHERE document_number IS NOT NULL;

COMMENT ON COLUMN guests.document_number IS
    'Número de documento de identidad (DNI, CC, pasaporte). Solo números, 5-12 dígitos.';