-- Guarda el identificador exacto del logo administrado por Cloudinary.
-- Nullable para mantener compatibilidad con alojamientos ya existentes.
ALTER TABLE accommodations
    ADD COLUMN IF NOT EXISTS logo_public_id VARCHAR(255);
