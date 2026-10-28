-- Configuración de marca del alojamiento para personalización del frontend.
-- Los valores existentes se conservan; las nuevas columnas reciben los valores acordados por defecto.
ALTER TABLE accommodations
    ADD COLUMN IF NOT EXISTS accent_color VARCHAR(7) NOT NULL DEFAULT '#d4f53c',
    ADD COLUMN IF NOT EXISTS tertiary_color VARCHAR(7) NOT NULL DEFAULT '#f4b8c4',
    ADD COLUMN IF NOT EXISTS background_pattern VARCHAR(20) NOT NULL DEFAULT 'none';

-- Los alojamientos existentes sin color principal configurado adoptan el valor por defecto.
UPDATE accommodations
SET primary_color = '#0fa3a3'
WHERE primary_color IS NULL OR BTRIM(primary_color) = '';

ALTER TABLE accommodations
    ALTER COLUMN primary_color SET DEFAULT '#0fa3a3',
    ALTER COLUMN accent_color SET DEFAULT '#d4f53c',
    ALTER COLUMN tertiary_color SET DEFAULT '#f4b8c4',
    ALTER COLUMN background_pattern SET DEFAULT 'none';
