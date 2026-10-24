-- Guarda el identificador del recurso administrado por Cloudinary para poder eliminarlo de forma segura.
-- Es nullable para conservar compatibilidad con imágenes existentes y URLs externas agregadas manualmente.
ALTER TABLE accommodation_images
    ADD COLUMN IF NOT EXISTS cloudinary_public_id VARCHAR(255);
