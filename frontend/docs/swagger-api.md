# HostFlow · Contrato de API (Swagger / OpenAPI)

El frontend consume el backend **Java Spring Boot** que vive en `backend/`. Este documento
resume el contrato y su consumo desde el cliente frontend:

- **Swagger UI**: `http://localhost:8080/api/v1/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api/v1/api-docs`
- **Context path**: `/api/v1` (`server.servlet.context-path` en `application.properties`)
- **Puerto backend**: `8080`
- **CORS**: configurado para el frontend

`VITE_API_URL` apunta a la **base con el context-path incluido**:

```env
VITE_API_URL=http://localhost:8080/api/v1
```

Las rutas de `src/lib/api/endpoints.js` son relativas a esa base.

- Formato: JSON (y `multipart/form-data` para subidas de imágenes).
- Autenticación: `Authorization: Bearer <token>` (JWT).
- En modo **demo** (sin `VITE_API_URL`) el frontend usa `src/lib/api/demoApi.js`.
- Con `VITE_API_URL` configurada, el frontend usa `src/lib/api/httpApi.js` y sincroniza el estado con el backend.

---

## Módulos Integrados

### 1. Autenticación (`/auth/*`)
- `POST /auth/login`: Ingreso con correo y contraseña. Retorna JWT y datos del usuario (`AuthResponse`).
- `POST /auth/register`: Registro de anfitrión (`OwnerRegisterRequest`). Retorna JWT y rol `ROLE_OWNER`.
- `POST /auth/forgot-password`: Genera y envía código OTP de 6 dígitos.
- `POST /auth/reset-password`: Restablece contraseña con `{ email, code, newPassword }`.
- `GET /auth/verify-email`: Verificación de correo.

### 2. Perfil y Galería de Alojamiento (`/owners/accommodation/*`)
- `GET /owners/accommodation/profile`: Obtiene el perfil completo del alojamiento (`AccommodationProfileResponse`).
- `POST /owners/accommodation/profile`: Crea el perfil inicial si aún no existe.
- `PATCH /owners/accommodation/profile`: Modifica datos generales, capacidad, precios y estilo visual.
- `GET /owners/accommodation/gallery`: Lista las fotos de la galería.
- `POST /owners/accommodation/gallery/upload`: Carga de imagen multipart a Cloudinary.
- `DELETE /owners/accommodation/gallery/{imageId}`: Elimina imagen de la galería.
- `POST /owners/accommodation/profile/logo`: Carga y reemplazo del logotipo.

### 3. Disponibilidad y Desactivaciones (`/owners/accommodation/*`)
- `GET /owners/accommodation/status`: Estado actual de disponibilidad.
- `GET /owners/accommodation/history`: Historial de desactivaciones temporales.
- `POST /owners/accommodation/disable`: Desactiva temporalmente el alojamiento (`disabledFrom`, `disabledUntil`, `reason`).
- `POST /owners/accommodation/enable`: Reactiva el alojamiento.
- `GET /owners/accommodation/availability`: Consulta disponibilidad por fecha.

### 4. Monedas y Métodos de Pago (`/owners/accommodation/*`)
- `GET /owners/accommodation/currencies`: Lista monedas habilitadas y cotizaciones.
- `PUT /owners/accommodation/currencies`: Habilita/deshabilita monedas (USD, EUR) y tasas.
- `GET /owners/accommodation/payment-methods`: Lista los métodos de pago configurados.
- `POST /owners/accommodation/payment-methods`: Agrega nuevo método de pago.
- `PATCH /owners/accommodation/payment-methods/{id}`: Modifica estado o datos del método.
- `DELETE /owners/accommodation/payment-methods/{id}`: Elimina método de pago.

### 5. Reservas del Propietario (`/owners/reservations/*`)
- `GET /owners/reservations`: Listado de reservas del alojamiento propio.
- `GET /owners/reservations/{id}`: Detalle de reserva.
- `POST /owners/reservations/{id}/cancel`: Cancelación con motivo.
- `POST /owners/reservations/{id}/complete`: Finalización de estadía post check-out.
- `POST /owners/reservations/{id}/reactivate-link`: Regenera enlace de confirmación.

### 6. Métricas y Estadísticas
- `GET /owners/dashboard/metrics`: Métricas del anfitrión (reservas activas, completadas, ingresos).
- `GET /admin/dashboard/metrics`: Métricas globales del administrador.

### 7. Panel de Administración (`/owners/*`)
- `GET /owners`: Lista completa de dueños registrados (solo `ROLE_ADMIN`).
- `PATCH /owners/{id}/status?enabled={true|false}`: Activa o pausa la cuenta de un anfitrión.
