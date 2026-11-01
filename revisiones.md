# Revisiones para el PR — Merge HF-20

> Basado en `tests/api-tests/GUIA-VALIDACION-PENDIENTES-HF-20.md`.
> Se omiten los ítems ya verificados de entorno/prerrequisitos (JDK, PostgreSQL,
> `curl`, `psql`, `.env`, arranque `dev`) y la preparación de bases (3.2), porque no
> generan observaciones.
> Evidencias: `tests/api-tests/evidencias/` (`p2-fresh.log`, `p2-upgrade.log`).
> Orden: primero lo funcional (alto peso), después documentación.

---

## 1. Funcionales — bloqueantes

### 1.1 Migraciones Flyway no aplican en base limpia

**Severidad:** bloqueante (despliegue desde cero).

Ejecuté el perfil `prod` contra PostgreSQL real. En una base nueva, `V2` falla con
`SQL State 42P01` (`undefined_table`) porque no existe `owners`. Ninguna migración
crea `owners`, `roles` ni `owner_roles`; `V1` solo crea `password_reset_tokens`.
Evidencia: `tests/api-tests/evidencias/p2-fresh.log`.

**Acción:** agregar una migración base con `owners`, `roles` y `owner_roles` antes de
`V2`, con el DDL exacto que espera JPA.

### 1.2 Migraciones Flyway no aplican sobre el esquema actual (`V3`)

**Severidad:** bloqueante (upgrade de base existente).

Sobre una base creada por Hibernate, Flyway hace baseline en `V1` y `V3` falla con
`SQL State 42703`: `ERROR: no existe la columna a.bank_name`. `V3` hace
`INSERT ... SELECT a.bank_name ...` asumiendo columnas `bank_*` que la entidad ya no
genera. Evidencia: `tests/api-tests/evidencias/p2-upgrade.log`.

**Acción:** hacer `V3` tolerante a la ausencia de `bank_*` (verificar
`information_schema.columns` antes del `SELECT`, o agregarlas con
`ALTER TABLE ... ADD COLUMN IF NOT EXISTS` en `V2`).

---

## 2. Funcionales — API / HTTP

### 2.1 `POST` multipart sin cuerpo responde `500`

**Severidad:** importante.

`POST /owners/accommodation/gallery/upload` sin body devuelve
`500 INTERNAL_ERROR`; debería ser `415 Unsupported Media Type` (o `400`). El
`GlobalExceptionHandler` atrapa `HttpMediaTypeNotSupportedException` con el handler
genérico `Exception`.

**Acción:** agregar un `@ExceptionHandler` específico y un test que lo cubra.

### 2.2 Sin token o token inválido responde `403`, no `401`

**Severidad:** importante (contrato de seguridad).

Los endpoints protegidos responden `403` tanto sin token como con token inválido; no
hay `AuthenticationEntryPoint` configurado. **Acción:** si el contrato espera `401`,
configurarlo; si se acepta `403`, documentarlo. Hoy no hay tests que lo validen.


---

## 3. Documentación

### 3.1 Límite de archivo: doc 5 MB vs código 500 KB–2 MB

**Severidad:** menor.

`backend/docs/gestion-alojamiento.md:173` indica hasta **5 MB**, pero el código valida
**500 KB–2 MB** (`CloudinaryImageService.java:19-20`) y el multipart es **2 MB por
archivo** (`application.properties:192`).

**Acción:** corregir la doc a 500 KB–2 MB (o ajustar el código si el límite real debe
ser 5 MB).

### 3.2 `POST /gallery` documentado pero inexistente

**Severidad:** menor.

La doc describe `POST /owners/accommodation/gallery` para registrar una URL externa,
pero solo existe `POST /gallery/upload`. `AccommodationImageRequest` quedó sin uso. El
flujo real es: subir archivo → Cloudinary → guardar `image_url` + `cloudinary_public_id`.

**Acción:** quitar esa sección de la doc y borrar el DTO sin uso, o implementar el
endpoint si se quiere soportar URLs externas.

### 3.3 `AccommodationController` documentado pero inexistente

**Severidad:** menor.

`backend/docs/gestion-alojamiento.md` (líneas 14, 16 y 223) referencia un
`AccommodationController` para la desactivación temporal, pero ese controlador no
existe y no hay entidad asociada.

**Acción:** quitar las menciones o aclarar que la desactivación temporal queda fuera
de este merge.


