-- ============================================================
-- Migración V1: Creación de tabla de tokens de recuperación
-- ============================================================
-- Este script se ejecutará automáticamente en Desarrollo y Producción
-- la primera vez que se inicie la aplicación.
-- ============================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
                                                     id BIGSERIAL PRIMARY KEY,
                                                     token VARCHAR(10) NOT NULL,
    email VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
    );

-- Índices para optimizar las búsquedas
CREATE INDEX IF NOT EXISTS idx_password_reset_token_email ON password_reset_tokens(email);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_expires ON password_reset_tokens(expires_at);

COMMENT ON TABLE password_reset_tokens IS 'Almacena tokens de recuperación de contraseña de un solo uso con expiración';