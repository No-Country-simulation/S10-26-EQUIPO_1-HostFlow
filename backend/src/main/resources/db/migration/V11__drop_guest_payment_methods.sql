-- ==============================================================================
-- HostFlow - Migración V11: Eliminar legado de métodos de pago del huésped
-- ==============================================================================
-- Contexto:
--   El flujo de producto no gestiona métodos de pago del huésped (pago fuera de
--   HostFlow; solo se informan los del alojamiento). El código Java ya no mapea
--   GuestPaymentMethod ni reservations.payment_method_id (Fase C).
--
-- Alcance (solo legado de huésped):
--   1) Quitar FK y columna payment_method_id de reservations
--   2) Eliminar tabla guest_payment_methods
--
-- NO toca:
--   - accommodation_payment_methods (métodos del owner/alojamiento)
--   - guests, guest_allergies, reservation_links, reservation_additional_services
--   - resto de columnas de reservations
--
-- Idempotencia: segura en BD vacía o con datos (IF EXISTS / DROP IF EXISTS).
-- PostgreSQL.
-- ==============================================================================

-- 1. Liberar reservas del vínculo opcional a guest_payment_methods
ALTER TABLE reservations
    DROP CONSTRAINT IF EXISTS fk_reservations_payment_method;

ALTER TABLE reservations
    DROP COLUMN IF EXISTS payment_method_id;

-- 2. Eliminar tabla de métodos de pago del huésped
--    (índices idx_guest_payment_methods_* se eliminan con la tabla)
DROP TABLE IF EXISTS guest_payment_methods;
