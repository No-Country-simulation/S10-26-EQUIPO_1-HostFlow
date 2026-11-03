# Integración Backend ↔ Frontend

> Rama: **`feat/integracion-back-front`** · Fecha: 01/11/2026

Este documento deja constancia de la integración entre el frontend React/Vite
(`frontend/`) y el backend Spring Boot (`backend/`): qué contrato se verificó,
cómo levantar el stack completo y qué esperar mientras el backend sigue en
desarrollo.

---

## 1. Cómo corre el stack completo

### 1.1 Requisitos

| Software | Para qué | Verificación |
|---|---|---|
| **JDK 17** | Compilar/ejecutar el backend | `java -version` |
| **PostgreSQL 13+** | Base de datos del backend | `psql --version` |
| **Node 18+ / pnpm** | Frontend Vite | `pnpm -v` |

> El backend incluye Maven Wrapper: **no** hace falta instalar Maven.

### 1.2 Backend (terminal 1)

```powershell
# 1. Base de datos (una sola vez)
createdb hostflow_dev            # o desde pgAdmin

# 2. Variables de entorno
cd backend
copy .env.example .env           # editar DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD

# 3. Arrancar
.\mvnw.cmd spring-boot:run       # en Linux/Mac: ./mvnw spring-boot:run
```

- Context path: `http://localhost:8080/api/v1`
- Swagger UI: <http://localhost:8080/api/v1/swagger-ui.html>

### 1.3 Frontend (terminal 2)

```powershell
cd frontend
pnpm install
copy .env.example .env           # y poner VITE_API_URL=http://localhost:8080/api/v1
pnpm dev                         # http://localhost:5173
```

### 1.4 Modo demo vs. modo real

| | Modo **demo** (default) | Modo **real** |
|---|---|---|
| Se activa cuando | `VITE_API_URL` **no** está definida | `VITE_API_URL=http://localhost:8080/api/v1` |
| Datos | En memoria (`src/lib/api/demoApi.js`), nada viaja por red | Llamadas HTTP reales (`src/lib/api/httpApi.js`) |
| Login | Credenciales fijas (ver abajo) | Credenciales creadas vía `POST /auth/register` |
| Banner | Se muestra "Modo demostración" en pantalla | Sin banner |

**Credenciales de demo** (solo modo demo):

- Anfitrión: `anfitrion@hostflow.app` / `demo1234`
- Admin: `admin@hostflow.app` / `admin1234`

> **Nota:** `frontend/.env` **no** se versiona y no se crea por defecto a
> propósito: si el backend no está levantado y `VITE_API_URL` apunta a él,
> el login falla (no hay fallback para `login`). Con la app en modo demo todo
> funciona sin backend.

---

## 2. Contrato verificado contra el backend

Fuente de verdad: `backend/src/main/java/com/hostflow/controller/`
(`AuthController`, `OwnerController`) y `.../dto/`.

### 2.1 Auth — `AuthController` ✅ coincide con `endpoints.js`

| Frontend (`endpoints.js`) | Backend (`AuthController`) | Estado |
|---|---|---|
| `POST /auth/login` | `@PostMapping("/auth/login")` → `AuthResponse` | ✅ |
| `POST /auth/register` | `@PostMapping("/auth/register")` → `AuthResponse` | ✅ |
| `POST /auth/forgot-password` | `@PostMapping("/auth/forgot-password")` | ✅ |
| `POST /auth/reset-password` | `@PostMapping("/auth/reset-password")` | ✅ |

- **`LoginRequest`**: `{ email, password }` → lo que envía `httpApi.js`. ✅
- **`AuthResponse`**: `{ token, tokenType, userId, email, fullName, roles }` →
  `keepToken()` en `src/lib/api/index.js` consume `result.token` y `result.user`.
  ✅ (`tokenType` se ignora, no rompe)
- **`resetPassword`**: el front envía `password` **y** `newPassword` con el
  mismo valor (el backend exige `newPassword`). ✅
- **`OwnerRegisterRequest`** (registro): `{ fullName, accommodationName,
  email, password, termsAccepted }` → coincide con lo que arma
  `CreateAccountPage.jsx`. ✅

### 2.2 Owners — `OwnerController` ✅

| Frontend | Backend | Estado |
|---|---|---|
| `GET /owners/email/{email}` | `@GetMapping("/email/{email}")` | ✅ |
| `GET /owners/{id}` | `@GetMapping("/{id}")` | ✅ |
| `GET /owners` (admin) | `@GetMapping` + `ROLE_ADMIN` | ✅ |
| `PATCH /owners/{id}/status?enabled=` | `@PatchMapping("/{id}/status")` + `ROLE_ADMIN` | ✅ |

`OwnerResponse` coincide con el mapeo de `httpApi.js`. ✅

### 2.3 CORS ✅

`WebConfig` permite `*` origins con `GET, POST, PUT, DELETE, PATCH, OPTIONS`,
así que el dev server de Vite (`http://localhost:5173`) puede hablar con el
backend sin configuración extra. **En producción hay que restringir el
origen** (comentario ya presente en `WebConfig`).

### 2.4 Módulos aún no implementados en el backend

`/lodgings/**`, `/bookings/**`, `/onboarding/**`, `/admin/stats` están
**declarados como contrato reservado** en `endpoints.js`. Comportamiento:

1. `httpApi.js` detecta `404 / 405 / 501 / status 0` ("este endpoint todavía
   no existe") → `withLocalFallback` opera sobre el **store local**.
2. Los errores reales (`400`, `401`, `500`…) **se propagan** y la pantalla los
   muestra: no se enmascaran.
3. Si la operación no está declarada en absoluto (`ApiNotConfiguredError`), el
   `op()` de `src/lib/api/index.js` cae al store sin romper la app.

Esto permite usar reservas, calendario, servicios, pagos y admin con el
backend parcialmente implementado, sin pantallas rotas.

---

## 3. Checklist de pruebas

### Frontend (sin backend — modo demo)

- [x] `pnpm build` compila sin errores
- [ ] Home (`/`): hero con escena SVG animada, galería polaroid, testimonio,
      marquee, reveals al hacer scroll
- [ ] Login con credenciales demo (anfitrión y admin)
- [ ] `/panel/disponibilidad`: celdas de reserva en los 4 tonos del color de
      marca elegido; panel lateral **por reserva** (nombre, huéspedes, día,
      botón "Ver detalles" → `BookingDetailModal`)
- [ ] Días bloqueados: alta/baja desde el panel compacto
- [ ] Cambio de tema claro/oscuro y cambio de marca de colores: los tonos del
      calendario se recalculan
- [ ] Onboarding del huésped (enlace de llegada)

### Stack completo (backend + frontend)

- [ ] `POST /auth/login` real desde el UI con usuario creado por registro
- [ ] El JWT se guarda y se envía en Authorization (`Authorization: Bearer …`)
- [ ] `GET /owners/email/{email}` devuelve el perfil tras login
- [ ] Los endpoints no implementados caen al store local sin romper pantallas
- [ ] Swagger UI responde en `/api/v1/swagger-ui.html`

---

## 4. Archivos tocados en esta rama

| Archivo | Cambio |
|---|---|
| `.gitignore` (raíz) | Nuevo: ignora `node_modules/`, `dist/`, `target/`, `.env*`, logs |
| `README.md` (raíz) | Sección "Correr el stack completo" (back + front) |
| `frontend/docs/integracion-back-front.md` | Este documento |

La integración de código (mapeo de endpoints, fallback y token) **ya estaba
implementada** en `src/lib/api/*`; esta rama la verifica contra el backend real
y documenta cómo levantarla.
