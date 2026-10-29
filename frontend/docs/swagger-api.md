# HostFlow · Contrato de API (Swagger / OpenAPI)

El frontend consume el backend **Java Spring Boot** que vive en `backend/`. Este documento
resume el contrato real; la fuente de verdad es el propio backend:

- **Swagger UI**: `http://localhost:8080/api/v1/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api/v1/api-docs`
- **Context path**: `/api/v1` (`server.servlet.context-path` en `application.properties`)
- **Puerto**: `8080` · **CORS**: habilitado para `*` (ver `WebConfig`)

`VITE_API_URL` apunta a la **base con el context-path incluido**:

```env
VITE_API_URL=http://localhost:8080/api/v1
```

Las rutas de `src/lib/api/endpoints.js` son relativas a esa base.

- Formato: JSON. Errores con `{ "message": "..." }` (se muestra tal cual al usuario).
- Autenticación: `Authorization: Bearer <token>` (JWT, TTL 24 h).
- En modo **demo** (sin `VITE_API_URL`) el frontend usa `src/lib/api/demoApi.js`, que
  implementa el **mismo contrato** con datos en memoria.

---

## Auth

### `POST /auth/register` → 201

Cuerpo (`OwnerRegisterRequest`):

```json
{
  "fullName": "Martina Silva",
  "accommodationName": "Cabañas del Arroyo",
  "email": "anfitrion@hostflow.app",
  "password": "demo1234",
  "termsAccepted": true
}
```

Respuesta (`AuthResponse`):

```json
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "anfitrion@hostflow.app",
  "fullName": "Martina Silva",
  "roles": ["ROLE_OWNER"]
}
```

Errores: `400` (validación / correo ya registrado) · `500`.

### `POST /auth/login` → 200

Cuerpo (`LoginRequest`): `{ "email": "...", "password": "..." }` → `AuthResponse` (igual que arriba).
Errores: `401` (credenciales incorrectas) · `400` (datos inválidos).

### `POST /auth/forgot-password` → 200

Cuerpo (`ForgotPasswordRequest`): `{ "email": "..." }`

Respuesta (`PasswordResetResponse`):

```json
{ "message": "Si el correo está registrado, recibirás un código.", "email": "an••••@...", "expirationMinutes": 60 }
```

- El código tiene **6 dígitos**, expira en **60 minutos** y es de un solo uso.
- Por seguridad responde **200 siempre**, exista o no el correo (anti-enumeración de usuarios).

### `POST /auth/reset-password` → 200

Cuerpo (`ResetPasswordRequest`):

```json
{ "email": "anfitrion@hostflow.app", "code": "123456", "newPassword": "nueva1234" }
```

Respuesta: `PasswordResetResponse` (`message`, `email`, `expirationMinutes`).

Errores: `400` (código inválido / expirado / ya usado) · `404` (usuario no encontrado).

> El frontend valida la **confirmación de la nueva clave** en pantalla (2 inputs) y envía
> `newPassword` al backend.

---

## Dueños de alojamiento (requieren JWT)

Controlador `OwnerController` · `ROLE_OWNER` o `ROLE_ADMIN`, salvo donde se indica.

### `GET /owners/email/{email}` → 200

`OwnerResponse`:

```json
{
  "id": 1,
  "fullName": "Martina Silva",
  "accommodationName": "Cabañas del Arroyo",
  "email": "anfitrion@hostflow.app",
  "enabled": true,
  "roles": ["ROLE_OWNER"],
  "createdAt": "2026-10-19T18:20:16Z",
  "updatedAt": "2026-10-19T18:20:16Z"
}
```

Errores: `401` (sin token) · `403` (sin permisos) · `404` (no existe).

### `GET /owners/{id}` → 200 → `OwnerResponse`

### `GET /owners` → 200 → `OwnerResponse[]` — **solo `ROLE_ADMIN`**

Lista completa de dueños registrados. Es la fuente del **panel de control interno**
(`/admin` del frontend): cantidad de alojamientos y búsqueda por dueño, ID o correo
(el filtrado del lado del frontend usa este listado).

### `PATCH /owners/{id}/status?enabled=true` → 200 → `OwnerResponse` — **solo `ROLE_ADMIN`**

Activa o desactiva la cuenta del dueño. `enabled=false` impide el inicio de sesión,
pero conserva los datos.

---

## Módulos reservados (contrato definido, backend pendiente)

Estos módulos **todavía no existen** en `backend/`, pero su contrato ya está
definido en `src/lib/api/endpoints.js`. Mientras tanto, `src/lib/api/httpApi.js`
detecta que el endpoint aún no responde (404 / 405 / 501, o falta de conexión)
y opera sobre el store local (`src/lib/state/store.js`), de modo que ninguna
pantalla se rompe. Cuando el backend los publique, la app pasa a usarlos sin
tocar las pantallas.

| Módulo | Rutas reservadas |
|---|---|
| Alojamiento (datos, imágenes, precio) | `GET`/`PUT /lodgings/mine` · `PUT /lodgings/mine/logo` · `POST`/`DELETE /lodgings/mine/images` · `PUT /lodgings/mine/brand` |
| Disponibilidad (días bloqueados) | `GET`/`PUT /lodgings/mine/blocked-days` · `PATCH /lodgings/mine/blocked-days/{date}` |
| Reservas | `GET`/`POST /lodgings/mine/bookings` · `PATCH /bookings/{id}` · `PATCH /bookings/{id}/status` · `PATCH /bookings/{id}/payment-status` · `POST /bookings/{id}/onboarding-link` |
| Onboarding del huésped (B2C, **público**, sin token) | `GET`/`POST /onboarding/{lodgingId}/{bookingId}` |
| Cotización de transporte en lancha | `POST`/`DELETE /bookings/{id}/boat-quote` (ver [boat-quote-contract.md](boat-quote-contract.md)) |
| Servicios adicionales | `GET`/`POST /lodgings/mine/services` · `PATCH`/`DELETE /lodgings/mine/services/{id}` · `PATCH /lodgings/mine/services/{id}/status` |
| Métodos de pago (máximo 4 por alojamiento) | `GET`/`POST /lodgings/mine/payment-methods` · `PATCH`/`DELETE /lodgings/mine/payment-methods/{id}` · `PATCH /lodgings/mine/payment-methods/{id}/status` · `PUT /lodgings/mine/payment-methods/currency` · `PUT /lodgings/mine/payment-methods/wallets` · `PUT /lodgings/mine/bank-details` |
| Estadísticas de administración (solo `ROLE_ADMIN`) | `GET /admin/stats` |

### Notas de integración

- **Alojamiento**: `GET /lodgings/mine` devuelve el alojamiento del dueño
  autenticado. Si el endpoint todavía no existe, el front completa lo único que
  el backend sí sabe (nombre del alojamiento y titularidad) desde
  `GET /owners/email/{email}`.
- **Métodos de pago**: el límite de **4 métodos** se respeta tanto en la
  interfaz como en `httpApi` y en el store, por si el backend lo mandara mal.
  Cada método lleva `label`, `type` (`WALLET` · `BANK` · `CARD`), `enabled`,
  `keys` (credenciales/librerías) y `notes`. El onboarding del huésped lista los
  métodos **habilitados** y permite copiar sus credenciales.
- **Disponibilidad**: las fechas usan el formato ISO `YYYY-MM-DD`. El calendario
  del panel marca como **ocupados** los días cubiertos por una reserva activa
  (además de los bloqueos manuales) y el alta de reservas valida el solape.
- **Administración**: `GET /owners` es la fuente del listado; el estado de un
  alojamiento se alterna habilitando o deshabilitando su dueño
  (`PATCH /owners/{id}/status?enabled=`). `GET /admin/stats` devuelve
  `{ lodgings, active, paused, hosts, bookings }`; sin él, el front calcula las
  métricas con el listado de dueños y muestra "—" en las que no puede derivar.
- **Onboarding B2C**: el `GET` público es el que permite que el huésped vea, en
  su propio dispositivo, el alojamiento, la reserva, los servicios y las
  formas de pago con sus credenciales (el POST registra la ficha completada).

