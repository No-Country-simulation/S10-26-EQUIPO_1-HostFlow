# Inventario del esquema de base de datos — migraciones Flyway V1–V7

## Resumen de migraciones

| Migración | Efecto |
|---|---|
| `V1__create_password_reset_tokens_table.sql` | Crea `password_reset_tokens` e índices por email y expiración. |
| `V2__create_accommodation_profile_and_gallery.sql` | Crea `owners`, `roles`, `owner_roles`, `accommodations` y `accommodation_images`. Añade compatibilidad para algunas columnas preexistentes de `accommodations`. |
| `V3__separate_accommodation_payment_methods.sql` | Crea `accommodation_payment_methods`, sus índices y migra columnas bancarias heredadas desde `accommodations` cuando están presentes las seis columnas esperadas. En ese caso las elimina del perfil. |
| `V4__add_cloudinary_public_id_to_accommodation_images.sql` | Añade `accommodation_images.cloudinary_public_id`. |
| `V5__add_cloudinary_public_id_to_accommodations.sql` | Añade `accommodations.logo_public_id`. |
| `V6__add_payment_method_type_specific_fields.sql` | Añade `account_type`, `user_identifier`, `wallet_phone` y `wallet_email` a `accommodation_payment_methods`. |
| `V7__add_accommodation_branding_fields.sql` | Añade `accent_color`, `tertiary_color` y `background_pattern`; actualiza valores vacíos de `primary_color` y establece defaults. |

## `password_reset_tokens` — V1

| Columna | Tipo SQL | Nulo | Default / restricción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. |
| `token` | `VARCHAR(10)` | No | Código/token de recuperación; el esquema admite hasta 10 caracteres. |
| `email` | `VARCHAR(150)` | No | Email solicitante. Índice `idx_password_reset_token_email`. |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP`. |
| `expires_at` | `TIMESTAMP` | No | Sin default SQL. Índice `idx_password_reset_token_expires`. |
| `used` | `BOOLEAN` | No | `FALSE`. |

No tiene FK a `owners`; la relación funcional se hace por email. Aunque el esquema permite hasta 10 caracteres, el flujo de recuperación documentado genera un código de seis dígitos.

## `owners` — V2

| Columna | Tipo SQL | Nulo | Default / restricción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. |
| `full_name` | `VARCHAR(150)` | No | — |
| `accommodation_name` | `VARCHAR(200)` | No | — |
| `email` | `VARCHAR(150)` | No | UNIQUE. |
| `password` | `VARCHAR(255)` | No | Hash de contraseña; nunca almacenar contraseña en texto plano. |
| `terms_accepted` | `BOOLEAN` | No | `FALSE`. |
| `enabled` | `BOOLEAN` | No | `TRUE`. |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP`. |
| `updated_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP`. |

## `roles` — V2

| Columna | Tipo SQL | Nulo | Default / restricción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. |
| `role_name` | `VARCHAR(50)` | No | UNIQUE; se persiste el nombre del rol. |

## `owner_roles` — V2

| Columna | Tipo SQL | Nulo | Default / restricción |
|---|---|---:|---|
| `owner_id` | `BIGINT` | No | Parte de PK compuesta; FK a `owners(id)` con `ON DELETE CASCADE`. |
| `role_id` | `BIGINT` | No | Parte de PK compuesta; FK a `roles(id)` con `ON DELETE CASCADE`. |

PK compuesta: `pk_owner_roles(owner_id, role_id)`. Índice adicional: `idx_owner_roles_role(role_id)`.

## `accommodations` — V2, V5 y V7

| Columna | Tipo SQL | Nulo | Default / restricción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. |
| `owner_id` | `BIGINT` | No | UNIQUE; FK `fk_accommodations_owner` a `owners(id)` con `ON DELETE CASCADE`. |
| `name` | `VARCHAR(200)` | No | Nombre comercial. |
| `room_count` | `INTEGER` | Sí | — |
| `bathroom_count` | `INTEGER` | Sí | — |
| `guest_capacity` | `INTEGER` | Sí | — |
| `base_price` | `NUMERIC(12,2)` | Sí | — |
| `logo_url` | `VARCHAR(2048)` | Sí | URL del logotipo. |
| `logo_public_id` | `VARCHAR(255)` | Sí | ID de Cloudinary; añadido por V5. |
| `primary_color` | `VARCHAR(20)` | Sí | V7 establece default `'#0fa3a3'` y rellena filas nulas/vacías. |
| `accent_color` | `VARCHAR(7)` | No | Default `'#d4f53c'`; V7. |
| `tertiary_color` | `VARCHAR(7)` | No | Default `'#f4b8c4'`; V7. |
| `background_pattern` | `VARCHAR(20)` | No | Default `'none'`; V7. |
| `welcome_message` | `TEXT` | Sí | — |
| `email` | `VARCHAR(150)` | Sí | Email de contacto. |
| `phone` | `VARCHAR(40)` | Sí | Teléfono de contacto. |
| `address` | `TEXT` | Sí | Dirección. |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP`. |
| `updated_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP`; V2 agrega la columna si falta. |

**Columnas bancarias heredadas:** V2 las declara inicialmente (`bank_name`, `bank_account_holder`, `bank_account_number`, `bank_cbu_cvu`, `bank_alias`, `bank_cuit`), pero V3 las elimina si detecta las seis juntas y migra sus valores a `accommodation_payment_methods`. No deben documentarse como columnas finales garantizadas de `accommodations` después de V3.

## `accommodation_images` — V2 y V4

| Columna | Tipo SQL | Nulo | Default / restricción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. |
| `accommodation_id` | `BIGINT` | No | FK `fk_accommodation_images_accommodation` a `accommodations(id)` con `ON DELETE CASCADE`. |
| `image_url` | `VARCHAR(2048)` | No | URL; el archivo binario no se guarda en esta tabla. |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP`. |
| `cloudinary_public_id` | `VARCHAR(255)` | Sí | ID de Cloudinary; añadido por V4. NULL para URL externa o registros antiguos sin ID. |

Índice: `idx_accommodation_images_accommodation(accommodation_id)`.

## `accommodation_payment_methods` — V3 y V6

| Columna | Tipo SQL | Nulo | Default / restricción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. |
| `accommodation_id` | `BIGINT` | No | FK `fk_payment_methods_accommodation` a `accommodations(id)` con `ON DELETE CASCADE`. |
| `payment_type` | `VARCHAR(30)` | No | Valores documentados: `BANK_TRANSFER`, `DIGITAL_WALLET`, `CASH`, `CARD`, `OTHER`. |
| `display_name` | `VARCHAR(120)` | No | Nombre visible. |
| `provider_name` | `VARCHAR(150)` | Sí | Proveedor. |
| `bank_name` | `VARCHAR(150)` | Sí | Banco o entidad. |
| `account_holder` | `VARCHAR(200)` | Sí | Titular. |
| `account_type` | `VARCHAR(80)` | Sí | Tipo de cuenta; V6. |
| `account_number` | `VARCHAR(100)` | Sí | Número de cuenta. |
| `cbu_cvu` | `VARCHAR(100)` | Sí | CBU/CVU. |
| `alias` | `VARCHAR(100)` | Sí | Alias de cuenta. |
| `tax_id` | `VARCHAR(30)` | Sí | Identificador fiscal. |
| `user_identifier` | `VARCHAR(200)` | Sí | Usuario/ID de billetera digital; V6. |
| `wallet_phone` | `VARCHAR(50)` | Sí | Teléfono de billetera digital; V6. |
| `wallet_email` | `VARCHAR(254)` | Sí | Correo de billetera digital; V6. |
| `instructions` | `TEXT` | Sí | Instrucciones. |
| `is_default` | `BOOLEAN` | No | `FALSE`. Índice único parcial por alojamiento cuando es TRUE. |
| `is_active` | `BOOLEAN` | No | `TRUE`. |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP`. |
| `updated_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP`. |

Índices: `idx_payment_methods_accommodation(accommodation_id)` y `uk_payment_methods_one_default_per_accommodation(accommodation_id) WHERE is_default = TRUE`. V3 incluye lógica condicional para migrar datos bancarios heredados de `accommodations` si las seis columnas existen juntas.


