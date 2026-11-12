-- ==============================================================================
-- HostFlow - Migración V13: Tablas/columnas requeridas por entidades JPA
--            que aún no estaban en Flyway (validate falla al arrancar).
-- ==============================================================================
-- Incluye:
--   - accommodation_temporary_disables
--   - email_verification_tokens
--   - login_attempts
--   - columnas de owners: email_verified, blocked_until, disabled_at
-- Idempotente. No altera el módulo de reservas ya alineado en V11/V12.
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. owners: campos de seguridad / estado de cuenta
-- ------------------------------------------------------------------------------
ALTER TABLE owners ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE owners ADD COLUMN IF NOT EXISTS blocked_until TIMESTAMP;
ALTER TABLE owners ADD COLUMN IF NOT EXISTS disabled_at TIMESTAMP;

COMMENT ON COLUMN owners.email_verified IS 'Indica si el correo del propietario fue verificado.';
COMMENT ON COLUMN owners.blocked_until IS 'Bloqueo temporal por intentos de login fallidos.';
COMMENT ON COLUMN owners.disabled_at IS 'Fecha de deshabilitación permanente de la cuenta (si aplica).';

-- ------------------------------------------------------------------------------
-- 2. accommodation_temporary_disables
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accommodation_temporary_disables (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    disabled_from TIMESTAMP NOT NULL,
    disabled_until TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_accommodation_temporary_disables_owner
        FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_owner_active
    ON accommodation_temporary_disables(owner_id, is_active);
CREATE INDEX IF NOT EXISTS idx_disable_dates
    ON accommodation_temporary_disables(disabled_from, disabled_until);
CREATE INDEX IF NOT EXISTS idx_owner_created
    ON accommodation_temporary_disables(owner_id, created_at);

COMMENT ON TABLE accommodation_temporary_disables IS
'Desactivaciones temporales del alojamiento (mantenimiento, temporada, etc.).';

-- ------------------------------------------------------------------------------
-- 3. email_verification_tokens
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_token_unique
    ON email_verification_tokens(token);
CREATE INDEX IF NOT EXISTS idx_email
    ON email_verification_tokens(email);

COMMENT ON TABLE email_verification_tokens IS
'Tokens de verificación de correo para registro de propietarios.';

-- ------------------------------------------------------------------------------
-- 4. login_attempts
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS login_attempts (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT,
    email VARCHAR(100) NOT NULL,
    full_name VARCHAR(150),
    role VARCHAR(50),
    success BOOLEAN NOT NULL,
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    blocked_until TIMESTAMP,
    attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_email_attempt
    ON login_attempts(email, attempt_at);
CREATE INDEX IF NOT EXISTS idx_owner_id
    ON login_attempts(owner_id);

COMMENT ON TABLE login_attempts IS
'Auditoría de intentos de inicio de sesión (éxito, fallo y bloqueos).';
