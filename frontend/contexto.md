# HostFlow · Frontend

MVP del frontend de **HostFlow** con el Home rediseñado, el módulo de autenticación completo
(Login · Registro · Restauración de clave con OTP) y el panel de gestión de alojamientos.

React 19 · Vite 8 · Tailwind CSS 4 · React Router 7 · Lucide.

## Cómo correrlo

```bash
pnpm install        # o npm install
pnpm dev            # http://localhost:5173
pnpm build          # genera dist/
```

Sin `VITE_API_URL` (ver `.env.example`) la app corre en **modo demo**: los datos viven en
memoria + `sessionStorage`, no se valida nada contra un servidor y se muestra un aviso visible.

### Credenciales de demostración

| Rol | Correo | Contraseña |
|---|---|---|
| Anfitrión | `anfitrion@hostflow.app` | `demo1234` |
| Administrador | `admin@hostflow.app` | `admin1234` |

En la pantalla de ingreso hay botones **“Demo anfitrión”** y **“Demo admin”** que entran
directamente (el acceso rápido ignora la contraseña para que la presentación nunca se bloquee
después de cambiar una clave).

## Rutas

| Ruta | Pantalla | Acceso |
|---|---|---|
| `/` | Home de presentación (minimalista moderno) | público |
| `/auth/login` | Ingreso | público |
| `/auth/registro` | Crear cuenta | público |
| `/auth/recuperar` | Recuperar clave: correo → **OTP 6 dígitos** → nueva clave + confirmación | público |
| `/panel` | **Panel** del anfitrión (próximas reservas, métricas, accesos rápidos) | sesión |
| `/panel/reservas` | Listado de reservas con filtros y búsqueda | sesión |
| `/panel/alojamiento` | Datos del alojamiento (nombre, habitaciones, baños, precio, imágenes) | sesión |
| `/panel/disponibilidad` | Calendario para **deshabilitar días** | sesión |
| `/panel/pagos` | Métodos de pago (ARS principal, USD/EUR secundarias, billeteras digitales) | sesión |
| `/panel/perfil` | Perfil: **subir logo** + personalización de la empresa (color, acento, formas) | sesión |
| `/admin` | **Control interno**: cantidad de alojamientos, listado y búsqueda por dueño / ID / correo | solo `role: ADMIN` |

## Tema claro / oscuro

- El **modo claro está preseleccionado**; se cambia con el botón sol/luna (header, panel y pantallas de acceso).
- La preferencia se guarda en `localStorage` (`hostflow.theme`).
- La paleta y sus reglas están documentadas en **[docs/paleta-colores.md](docs/paleta-colores.md)**.
- Los tokens viven en `@theme` (`src/index.css`); la clase `.dark` en `<html>` los sobreescribe.

## Integración con el backend (Swagger)

- Contrato de API: **[docs/swagger-api.md](docs/swagger-api.md)** (rutas, cuerpos, respuestas y errores).
- `src/services/endpoints.js` mapea cada operación a su ruta (`GET /v3/api-docs` del backend).
- `src/services/httpApi.js` implementa las llamadas reales; `src/services/demoApi.js` implementa el
  **mismo contrato** con datos en memoria.
- `src/services/index.js` elige el adaptador según `VITE_API_URL`. Las pantallas nunca llaman a `fetch`.
- Documentación interactiva del backend: `GET /swagger-ui/index.html`.

## Estructura

```
src/
  pages/          Home (presentación)
  pages/auth/     Login · Register · Recover (OTP)
  pages/app/      Dashboard · Bookings · Lodging · Availability · Payments · Profile
  pages/admin/    AdminDashboard (control interno)
  layouts/        AuthLayout
  components/     ui.jsx (sistema visual) · AppLayout · RequireAuth · BookingCard
  context/        ThemeContext (claro/oscuro) · SessionContext (sesión y acciones)
  data/           store.js (estado demo) · useStore.js (hook reactivo)
  services/       index (fachada) · httpApi · demoApi · endpoints · httpClient · tokenStorage · errors
  config/env.js   VITE_API_URL → DEMO_MODE
  lib/format.js   cn, isEmail, monedas, fechas, textos
  index.css       sistema visual (paleta, tema, animaciones)
docs/             paleta-colores.md · swagger-api.md
```

## Notas

- La sección **Mockups** se eliminó: no forms parte de la presentación final.
- Las animaciones (`rise`, `fade`, `pop`, `float`, `marquee`) están en `src/index.css` y respetan
  `prefers-reduced-motion`.
- El estado de la demo se persiste en `sessionStorage` (`hostflow.app.v1`), por lo que recargar la
  página no pierde los cambios (incluida una clave nueva definida por OTP).
