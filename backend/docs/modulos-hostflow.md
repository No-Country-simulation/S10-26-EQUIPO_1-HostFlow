# HostFlow API - Documentación Profesional de Módulos

> **Versión:** 2.1.0 - Actualizada según `clase_total.txt`
> **Stack:** Spring Boot 3.2.5 | Java 17 | PostgreSQL | JWT | Cloudinary | Swagger
> **Base URL:** `http://localhost:8080/api/v1`
> **Swagger UI:** `http://localhost:8080/api/v1/swagger-ui/index.html`
> **Auth:** `Authorization: Bearer <token>` - 24h de validez

---

## Tabla de Contenido
1. [Guía Rápida de Pruebas](#1-guía-rápida-de-pruebas)
2. [Módulo Autenticación](#2-módulo-autenticación---auth)
3. [Módulo Dueños](#3-módulo-dueños---owners)
4. [Módulo Gestión de Alojamiento - Estado](#4-módulo-gestión-de-alojamiento---estado-temporal)
5. [Módulo Perfil del Alojamiento](#5-módulo-perfil-del-alojamiento)
6. [Módulo Galería y Logo](#6-módulo-galería-y-logo)
7. [Módulo Monedas](#7-módulo-monedas---currencies)
8. [Módulo Métodos de Pago del Alojamiento](#8-módulo-métodos-de-pago-del-alojamiento)
9. [Módulo Servicios Adicionales](#9-módulo-servicios-adicionales)
10. [Módulo Reservas del Dueño](#10-módulo-reservas-del-dueño)
11. [Módulo Huéspedes Público](#11-módulo-huéspedes-público)
12. [Módulo Precios Públicos](#12-módulo-precios-públicos)
13. [Módulo Reservas Públicas](#13-módulo-reservas-públicas)
14. [Manejo de Errores y Seguridad](#14-manejo-de-errores-y-seguridad)

---

## 1. Guía Rápida de Pruebas

### 1.1 Orden obligatorio (dependencias)

```
1. Auth -> Register -> Verificar email -> Login (guardar token)
2. Perfil -> POST /owners/accommodation/profile (sin esto todo lo demás da 404)
3. Monedas / Métodos de Pago / Servicios Adicionales
4. Reservas Dueño / Huéspedes Público / Precios Públicos
5. Admin -> requiere UPDATE email_verified = true
```

### 1.2 Obtener tokens sin correo
```sql
-- Token verificación email
SELECT token, expires_at FROM email_verification_tokens 
WHERE email='tu@email.com' ORDER BY created_at DESC LIMIT 1;

-- Código 6 dígitos reset password
SELECT token, expires_at FROM password_reset_tokens 
WHERE email='tu@email.com' ORDER BY created_at DESC LIMIT 1;

-- Verificar admin manualmente
UPDATE owners SET email_verified=true WHERE email='adminhostflow@gmail.com';
```

### 1.3 Herramientas
- **Swagger:** Authorize -> pegar solo el token (sin Bearer).
- **Postman:** Variable `{{baseUrl}}` + `{{token}}` + Header `Authorization: Bearer {{token}}`.

---

## 2. Módulo Autenticación - `/auth`

Autenticación completa con verificación por correo, bloqueo por intentos fallidos y recuperación con código de 6 dígitos.

| Endpoint | Método | Auth | Qué hace (explicación fácil) |
|---|---|---|---|
| `/auth/register` | POST | No | Crea un dueño. Envía email de verificación y devuelve JWT (aunque aún no verificado). Crea Owner con ROLE_OWNER. |
| `/auth/login` | POST | No | Inicia sesión. Valida email+password, verifica que email esté verificado, revisa bloqueo por 5 intentos fallidos. Si todo ok, devuelve JWT + datos del owner. |
| `/auth/verify-email?token=xxx` | GET | No | Activa la cuenta. Marca email_verified=true y borra el token. Si token expirado o usado -> 400. |
| `/auth/resend-verification` | POST | No | Reenvía email si no se verificó. Body: `{email}`. Genera nuevo token y envía correo. |
| `/auth/forgot-password` | POST | No | Inicia reseteo. Genera código de 6 dígitos (10 min validez), guarda en password_reset_tokens, envía por email. Siempre responde 200 aunque email no exista (seguridad). |
| `/auth/reset-password` | POST | No | Completa reseteo. Body: `{email, code, newPassword}`. Valida código, cambia password encriptada con BCrypt, invalida todos los códigos anteriores, desbloquea login si estaba bloqueado. |

### Cómo probar cada uno

**POST /auth/register**
```json
{
  "name": "Carlos Lopez",
  "email": "carlos@test.com",
  "password": "Secure123!",
  "phone": "+573001234567"
}
```
- Prueba ok: Esperas 201 + `token` + `owner.emailVerified=false`. Luego verifica en DB y haz GET /verify-email.
- Prueba error: Email duplicado -> 400 "El correo ya está registrado". Password débil -> 400 validación.

**POST /auth/login**
```json
{"email":"carlos@test.com","password":"Secure123!"}
```
- Ok: 200 + token. Guardar para siguientes módulos.
- Sin verificar email: 400 "Debes verificar tu correo".
- Password mal 5 veces: 5to intento bloquea 15 min -> 400 "Cuenta bloqueada temporalmente".
- Probar bloqueo: hacer 5 logins malos y luego uno bueno -> debe seguir bloqueado.

**GET /auth/verify-email**
- En navegador: `http://localhost:8080/api/v1/auth/verify-email?token=TOKEN_DE_DB`
- Ok: 200 "Correo verificado". Luego login debe funcionar.
- Token reutilizado: 400 "Token ya usado".
- Token expirado (24h): 400 "Token expirado".

**POST /auth/forgot-password y reset-password**
```json
// forgot
{"email":"carlos@test.com"}
// reset
{"email":"carlos@test.com","code":"123456","newPassword":"Nueva123!"}
```
- Flujo: forgot -> leer código en DB -> reset con código.
- Código erróneo: 400 "Código inválido".
- Código usado: 400.
- Después de reset, login con nueva password debe funcionar y desbloquear.

---

## 3. Módulo Dueños - `/owners`

Solo ROLE_ADMIN. Gestión de usuarios del sistema.

| Endpoint | Método | Auth | Qué hace |
|---|---|---|---|
| `/owners/email/{email}` | GET | ADMIN | Busca un dueño por email exacto. Devuelve OwnerResponse. Útil para soporte. |
| `/owners/{id}` | GET | ADMIN | Busca por ID. |
| `/owners` | GET | ADMIN | Lista todos los dueños (sin paginación). |
| `/owners/{id}/status` | PATCH | ADMIN | Activa/desactiva cuenta. Body: `{active: true/false}`. Un dueño desactivado no puede hacer login aunque tenga token válido, pero tokens viejos siguen técnicamente válidos hasta expirar (validar en pruebas). |

### Pruebas
1. Loguearte como admin (recuerda hacer `email_verified=true` en DB).
2. `GET /owners` -> 200 lista.
3. `GET /owners/email/carlos@test.com` -> 200.
4. `PATCH /owners/5/status` con `{"active":false}` -> 200, owner desactivado.
5. Intentar login con ese dueño -> 400 "Cuenta desactivada".
6. Reactivar con `{"active":true}` -> login vuelve a funcionar.
7. Probar con token ROLE_OWNER -> debe dar 403 o 500 (manejador genérico).

---

## 4. Módulo Gestión de Alojamiento - Estado Temporal - `/owners/accommodation`

Controla si tu alojamiento está abierto o cerrado temporalmente.

| Endpoint | Método | Auth | Qué hace |
|---|---|---|---|
| `/disable` | POST | OWNER | Desactiva alojamiento. Body: `{reason, startDate, endDate}`. Crea registro en `accommodation_temporary_disable`. |
| `/enable` | POST | OWNER | Reactiva inmediatamente. Borra el estado activo o marca `is_active=false`. |
| `/status` | GET | OWNER | Dice si está activo, desactivado y hasta cuándo. |
| `/history` | GET | OWNER | Historial de todas las desactivaciones (activas y pasadas). |
| `/availability?date=2026-12-01` | GET | OWNER | Verifica si una fecha específica está disponible (útil para calendario). |

### Pruebas
```json
// POST /disable
{
  "reason": "Mantenimiento",
  "startDate": "2026-12-01T00:00:00",
  "endDate": "2026-12-10T23:59:59"
}
```
- Ok: 200 + objeto con fechas.
- Sin perfil creado: 404 "Registra primero los datos generales".
- Fechas invertidas (end < start): 400 validación.
- `GET /status` después de disable -> `active=false` + reason.
- `POST /enable` -> `GET /status` debe volver a `active=true`.
- `GET /history` -> debe listar 1 registro.
- `GET /availability?date=2026-12-05` -> false (está desactivado).
- `GET /availability?date=2026-12-20` -> true.

---

## 5. Módulo Perfil del Alojamiento - `/owners/accommodation/profile`

Es el módulo base. Sin él nada más funciona.

| Endpoint | Método | Auth | Qué hace |
|---|---|---|---|
| `/profile` | POST | OWNER | Crea el alojamiento. Body: nombre, descripción, dirección, basePrice (en moneda base), capacidad, etc. Solo uno por owner. |
| `/profile` | GET | OWNER | Obtiene tu perfil completo. |
| `/profile` | PATCH | OWNER | Actualiza parcial o total. |

### Pruebas
```json
// POST /profile
{
  "name": "Casa HostFlow Chía",
  "description": "Casa campestre con piscina",
  "address": "Vereda Fonqueta, Chía",
  "city": "Chía",
  "basePrice": 250000,
  "maxGuests": 6,
  "bedrooms": 3,
  "bathrooms": 2
}
```
- Primer POST -> 201. Segundo POST mismo owner -> 400 "Ya tienes un alojamiento".
- GET -> 200 con tu accommodationId (guárdalo para módulos públicos).
- PATCH con `{"basePrice":300000}` -> 200 y cambia precio.
- Probar sin token -> 403.
- Probar con owner diferente -> no ve el perfil del otro (404 o su propio perfil).

---

## 6. Módulo Galería y Logo

Gestión de imágenes con Cloudinary. Carpeta configurada en `cloudinary.folder=hostflow`.

| Endpoint | Método | Auth | Qué hace |
|---|---|---|---|
| `/gallery` | GET | OWNER | Lista imágenes actuales con url y orderIndex. |
| `/gallery` | POST | OWNER | Añade imagen por URL (si ya está subida). |
| `/gallery/upload` | POST | OWNER | Sube archivo real. `multipart/form-data` campo `file`. Valida 2MB máx por imagen, 5MB límite servidor, tipos jpg/png/webp. Guarda en Cloudinary y DB. |
| `/profile/logo` | POST | OWNER | Sube logo. Mismo flujo que galería pero solo 1 logo (reemplaza anterior). |
| `/gallery/{imageId}` | DELETE | OWNER | Borra imagen: elimina en Cloudinary + DB y reordena. |

### Pruebas
1. `POST /gallery/upload` con archivo 500KB jpg -> 201 + url cloudinary.
2. Subir 6 imágenes -> la 6ta debe fallar si límite es 5 (verificar mensaje).
3. Subir archivo .pdf -> 400 "Formato no permitido".
4. Subir archivo 3MB -> 400 "supera 2 MB".
5. `GET /gallery` -> lista ordenada.
6. `DELETE /gallery/1` -> 204, luego GET debe mostrar reordenado 0,1,2...
7. Probar Cloudinary credenciales inválidas -> 502 "No fue posible procesar imagen".

---

## 7. Módulo Monedas - `/owners/accommodation/currencies`

Permite aceptar pagos en diferentes monedas y convertir precios.

| Endpoint | Método | Auth | Qué hace |
|---|---|---|---|
| `/currencies` | GET | OWNER | Lista monedas aceptadas (ARS, USD, EUR, etc) con tasa efectiva. |
| `/currencies` | PUT | OWNER | Crea/actualiza moneda. Body: `{currencyCode, isAccepted, customRate?}`. Si customRate null, usa tasa del proveedor externo cacheada en `exchange_rate_cache`. |

### Pruebas
```json
// PUT /currencies
{
  "currencyCode": "USD",
  "isAccepted": true,
  "customRate": 950.5
}
```
- GET inicial -> lista vacía o solo ARS (moneda base).
- PUT USD -> 200. GET debe incluir USD con effectiveRate = customRate.
- PUT USD con customRate null -> debe calcular con API externa (o cache).
- PUT moneda inválida "XYZ" -> 400 validación.
- Desactivar: `{"currencyCode":"USD","isAccepted":false}` -> 200 y ya no aparece en precios públicos.

---

## 8. Módulo Métodos de Pago del Alojamiento - `/owners/accommodation/payment-methods`

Define cómo te pueden pagar (transferencia, MercadoPago, etc).

| Endpoint | Método | Auth | Qué hace |
|---|---|---|---|
| `/payment-methods` | GET | OWNER | Lista tus métodos. |
| `/payment-methods` | POST | OWNER | Crea método. Body: `{paymentType, displayName, providerName, bankName, accountHolder, accountNumber}` |
| `/payment-methods/{methodId}` | PATCH | OWNER | Actualiza. |
| `/payment-methods/{methodId}` | DELETE | OWNER | Borra. |

### Pruebas
```json
// POST
{
  "paymentType": "BANK_TRANSFER",
  "displayName": "Bancolombia Ahorros",
  "bankName": "Bancolombia",
  "accountHolder": "Carlos Lopez",
  "accountNumber": "123456789"
}
```
- POST -> 201 con ID.
- GET -> lista con 1.
- PATCH -> cambiar displayName.
- DELETE -> 204. GET posterior vacío.
- Probar con type inválido -> 400 o 500 por enum.

---

## 9. Módulo Servicios Adicionales - `/owners/accommodation/additional-services`

Ejemplo: Desayuno, Transporte, Tour. Cada servicio puede tener hasta 3 imágenes.

| Endpoint | Método | Auth | Qué hace |
|---|---|---|---|
| `/additional-services` | GET | OWNER | Lista tus servicios. |
| `/additional-services` | POST | OWNER | Crea servicio. Body: `{name, description, basePrice}` |
| `/{serviceId}` | PATCH | OWNER | Actualiza (ojo: exige name y basePrice aunque sea PATCH). |
| `/{serviceId}` | DELETE | OWNER | Borra servicio (no borra imágenes en Cloudinary - hallazgo). |
| `/{serviceId}/images` | GET | OWNER | Lista imágenes del servicio. |
| `/{serviceId}/images` | POST | OWNER | Sube imagen (multipart). Máx 3 por servicio. |
| `/{serviceId}/images/{imageId}` | DELETE | OWNER | Borra imagen y reordena índices. Si falla Cloudinary, igual borra en DB (204). |

### Pruebas
```json
// POST /additional-services
{"name":"Desayuno","description":"Desayuno campesino","basePrice":25000}
```
- Crear -> 201. Crear 2 más -> GET debe traer 3.
- Subir imagen a servicio 1 -> 201 con orderIndex 0.
- Subir 3 imágenes -> 4ta debe fallar 400 "máximo 3 imágenes".
- Subir imagen a servicio de otro owner -> 404.
- `DELETE /{serviceId}/images/{imageId}` del medio -> GET posterior debe reordenar 0,1.
- `DELETE /{serviceId}` -> 204, GET lista ya no lo trae, pero verificar Cloudinary aún tiene archivos (bug conocido).

---

## 10. Módulo Reservas del Dueño - `/owners/reservations`

Gestión de reservas creadas por el dueño o confirmadas desde público.

| Endpoint | Método | Auth | Qué hace |
|---|---|---|---|
| `/owners/reservations` | POST | OWNER | Crea reserva manual. Body: fechas, guestId, servicios adicionales, total. Verifica disponibilidad (no choque con disable ni con otras reservas). Genera link de confirmación. |
| `/owners/reservations` | GET | OWNER | Lista todas tus reservas. |
| `/{reservationId}` | GET | OWNER | Detalle de una reserva. |
| `/{reservationId}/cancel` | POST | OWNER | Cancela reserva. |
| `/{reservationId}/reactivate-link` | POST | OWNER | Regenera link de pago/confirmación expirado. |
| `/check-availability?startDate=&endDate=` | GET | OWNER | Verifica disponibilidad para rango. |

### Pruebas
```json
// POST /owners/reservations
{
  "guestId": 1,
  "startDate": "2026-12-15T14:00:00",
  "endDate": "2026-12-18T11:00:00",
  "additionalServiceIds": [1],
  "totalPrice": 850000
}
```
- Sin guest existente -> 404 o crear guest primero vía /public/guests.
- Rango que choca con disable -> 400 "Alojamiento no disponible".
- Rango que choca con otra reserva -> 400.
- Ok -> 201 con link.
- GET lista -> debe aparecer.
- POST /cancel -> estado cambia a CANCELLED.
- POST /reactivate-link -> nuevo link.
- GET /check-availability con rango libre -> true, con rango ocupado -> false.

---

## 11. Módulo Huéspedes Público - `/public/guests`

Público, sin JWT. Para que el flujo de reserva no requiera login del huésped.

| Endpoint | Método | Auth | Qué hace |
|---|---|---|---|
| `/public/guests` | POST | No | Crea o recupera huésped por email (idempotente). Body: name, email, phone. Si email existe, devuelve existente. |
| `/email/{email}` | GET | No | Busca huésped por email. |
| `/{guestId}/payment-methods` | GET | No | Lista métodos de pago guardados del huésped (para reutilizar). |
| `/{guestId}/payment-methods` | POST | No | Guarda método de pago del huésped. |
| `/{guestId}/payment-methods/{methodId}` | DELETE | No | Borra método del huésped. |

### Pruebas
```json
// POST /public/guests
{"name":"Ana Maria","email":"ana@test.com","phone":"+573001111222"}
```
- Primera vez -> 201 nuevo guest.
- Segunda vez mismo email -> 200 mismo guest (idempotente).
- GET /email/ana@test.com -> 200.
- POST /{guestId}/payment-methods con datos bancarios -> 201.
- GET payment-methods -> lista 1.
- DELETE -> 204.

---

## 12. Módulo Precios Públicos - `/public/accommodations/{accommodationId}`

Público, sin JWT. Muestra precios convertidos según monedas aceptadas.

| Endpoint | Método | Auth | Qué hace |
|---|---|---|---|
| `/base-price?currency=USD` | GET | No | Devuelve precio base convertido a moneda solicitada. Usa tasa efectiva (custom o cache). Query params: currency (ARS,USD,EUR). Sin param, devuelve en moneda base y todas las convertidas. |
| `/additional-services?currency=USD` | GET | No | Lista servicios adicionales con precios convertidos. |

### Pruebas
1. Sin perfil -> 404.
2. `GET /public/accommodations/1/base-price` -> 200 lista con `originalPrice` ARS y `convertedPrice` por cada moneda aceptada.
3. `GET /public/accommodations/1/base-price?currency=USD` -> solo USD convertido.
4. Si accommodation sin basePrice -> puede dar 500 (hallazgo).
5. `GET /additional-services` -> lista servicios con precios convertidos.
6. Probar con currency no aceptada -> 400 o lista vacía.
7. Verificar cálculo: basePrice 250000 ARS / tasa 950 = ~263 USD.

---

## 13. Módulo Reservas Públicas - `/public/reservations`

Flujo de confirmación para huéspedes.

| Endpoint | Método | Auth | Qué hace |
|---|---|---|---|
| `/confirm?token=xxx` | GET | No | Obtiene detalles de reserva por token de link (enlace enviado por email). Valida expiración. |
| `/confirm` | POST | No | Confirma reserva. Body: `{token}`. Cambia estado a CONFIRMED y bloquea fechas. |

### Pruebas
1. Crear reserva desde módulo dueño -> copiar `confirmationToken` de respuesta.
2. `GET /public/reservations/confirm?token=TOKEN` -> 200 detalles.
3. `POST /public/reservations/confirm` con `{"token":"TOKEN"}` -> 200 CONFIRMED.
4. Intentar confirmar mismo token 2 veces -> 400 "Ya confirmada" o "Token usado".
5. Token expirado -> 400 "Link expirado", luego usar `/owners/reservations/{id}/reactivate-link` para regenerar.
6. Intentar reservar fechas ya confirmadas desde otro guest -> debe fallar disponibilidad.

---

## 14. Manejo de Errores y Seguridad

### Formato estándar
```json
{
  "timestamp": "2026-10-30T10:15:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Explicación clara",
  "errorCode": "OPCIONAL"
}
```

### Tabla de códigos
| Código | Cuándo |
|---|---|
| 200 | OK, PATCH/GET exitoso |
| 201 | Creación POST exitosa |
| 204 | Borrado exitoso sin cuerpo |
| 400 | Regla de negocio, validación, token expirado |
| 401 | Credenciales inválidas (login) - aunque en este proyecto algunos responden 400 |
| 403 | Sin token o token de otro rol |
| 404 | Recurso no existe o no es tuyo |
| 500 | Error inesperado o parámetro faltante por GlobalExceptionHandler |
| 502 | Cloudinary falló |

### Checklist de seguridad para probar
- Intentar acceder a `/owners/accommodation/*` sin token -> 403.
- Usar token de owner A para ver datos de owner B -> 404 (protegido por ownerId).
- Login con SQL injection `' OR '1'='1` -> 400 validación.
- Subir archivo .exe como imagen -> 400.
- Probar 6 intentos de login fallidos -> bloqueo 15 min.
- Token JWT manipulado (cambiar una letra) -> 500 o 403 (filtro JWT).
- Dueño desactivado con PATCH status -> token viejo aún funciona hasta expirar (verificar).

---

## Anexos - Ubicación del código

- **Controllers:** `com.hostflow.controller.*`
- **Services:** `AuthService, OwnerService, AccommodationTemporaryDisableService, AcceptedCurrencyService, CurrencyConversionService, ExchangeRateProviderService, AccommodationProfileService, AdditionalServiceService, CloudinaryImageService, ReservationService, GuestService, PublicPricingService`
- **Models:** `Owner, Accommodation, AcceptedCurrency, ExchangeRateCache, AdditionalService, AdditionalServiceImage, Guest, Reservation, AccommodationTemporaryDisable`
- **Config:** `SecurityConfig, JwtAuthenticationFilter, CloudinaryConfig, DataInitializer, SwaggerConfig`
- **application.properties:** `jwt.expiration=86400000, cloudinary.folder=hostflow`

> Documento generado automáticamente desde `clase_total.txt` - Revisado y explicado para QA y desarrolladores junior.
