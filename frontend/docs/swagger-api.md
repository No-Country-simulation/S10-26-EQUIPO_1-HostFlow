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

Las rutas de `src/services/endpoints.js` son relativas a esa base.

- Formato: JSON. Errores con `{ "message": "..." }` (se muestra tal cual al usuario).
- Autenticación: `Authorization: Bearer <token>` (JWT, TTL 24 h).
- En modo **demo** (sin `VITE_API_URL`) el frontend usa `src/services/demoApi.js`, que
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

## Pendiente de definir en el backend

Estos módulos todavía **no existen** en `backend/`. Mientras tanto la capa de servicios
(`src/services/index.js`) cae al **store local** (`src/data/store.js`), de modo que las
pantallas siguen siendo usables; cuando el backend los implemente, alcanza con agregar las
rutas en `src/services/endpoints.js` y los métodos en `src/services/httpApi.js`: las
pantallas ya consumen la capa de servicios y no cambian.

| Módulo | Rutas reservadas en `endpoints.js` |
|---|---|
| Alojamiento (datos, imágenes, precio) | `/lodgings/mine` |
| Disponibilidad (días bloqueados) | `/lodgings/mine/blocked-days` |
| Reservas | `/lodgings/mine/bookings`, `/bookings/{id}` |
| Métodos de pago | `/lodgings/mine/payment-methods` |
| Estadísticas de administración | `/admin/stats` |
