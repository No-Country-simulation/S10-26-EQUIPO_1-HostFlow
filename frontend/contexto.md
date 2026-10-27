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
| `/panel/pagos` | Métodos de pago (ARS principal, USD/EUR secundarias, billeteras digitales) + **datos para transferencia** | sesión |
| `/panel/servicios` | **Servicios adicionales**: alta, edición, activación y precios | sesión |
| `/panel/perfil` | Perfil: **subir logo** + personalización de la empresa (3 colores, formas) | sesión |
| `/llegada/:lodgingId/:bookingId` | **Onboarding del huésped** (B2C, sin cuenta): ficha de llegada, alérgenos, servicios y **cotización opcional de lancha** | público (enlace único) |
| `/admin` | **Control interno**: cantidad de alojamientos, listado y búsqueda por dueño / ID / correo | solo `role: ADMIN` |

## Tema claro / oscuro

- El **modo claro está preseleccionado**; se cambia con el botón sol/luna (header, panel y pantallas de acceso).
- La preferencia se guarda en `localStorage` (`hostflow.theme`).
- La paleta y sus reglas están documentadas en **[docs/paleta-colores.md](docs/paleta-colores.md)**.
- Los tokens viven en `@theme` (`src/styles/globals.css`); la clase `.dark` en `<html>` los sobreescribe.

## Integración con el backend (Swagger)

- Contrato de API: **[docs/swagger-api.md](docs/swagger-api.md)** (rutas, cuerpos, respuestas y errores).
- `src/lib/api/endpoints.js` mapea cada operación a su ruta (contrato real del backend, ver
  `backend/src/main/java/com/hostflow/controller/`).
- `src/lib/api/httpApi.js` implementa las llamadas reales; `src/lib/api/demoApi.js` implementa el
  **mismo contrato** con datos en memoria.
- `src/lib/api/index.js` elige el adaptador según `VITE_API_URL`. Las pantallas nunca llaman a `fetch`.
- El backend expone el context-path `/api/v1`, así que `VITE_API_URL` debe incluirlo:
  `VITE_API_URL=http://localhost:8080/api/v1`.
- Documentación interactiva del backend: `GET /api/v1/swagger-ui.html` (OpenAPI en `/api/v1/api-docs`).

## Estructura

```
src/
  features/home/pages/          HomePage (presentación)
  features/dashboard/
    pages/      OwnerDashboardPage · BookingsPage · LodgingPage · AvailabilityPage · PaymentMethodsPage · ServicesPage · LodgingProfilePage
    components/ BookingSummaryCard · BookingDetailModal · BookingFormModal
  features/onboarding/
    pages/        OnboardingPage (ficha de llegada del huésped, B2C)
    components/   OnboardingShell (identidad del alojamiento) · BoatQuoteStep
  features/admin/pages/         AdminOverviewPage (control interno)
  features/auth/
    pages/        LoginPage · CreateAccountPage · PasswordRecoveryPage (OTP)
    context/      SessionContext (sesión y acciones)
  routes/         index.jsx (rutas) · guards/ProtectedRoute.jsx
  components/
    ui/index.jsx  sistema visual compartido · Modal.jsx (diálogo reutilizable)
    layout/       AuthenticatedAppLayout · AuthLayout · ScrollToTop
  lib/
    api/          index (fachada) · httpApi · demoApi · endpoints · httpClient · tokenStorage · errors
    state/        store.js (estado demo) · useStore.js (hook reactivo)
    config/env.js VITE_API_URL → DEMO_MODE
    theme/        ThemeContext (claro/oscuro) · LodgingTheme (colores del alojamiento, scoped)
    format.js     cn, isEmail, monedas, fechas, textos
  styles/globals.css sistema visual (paleta, tema, animaciones)
docs/             paleta-colores.md · swagger-api.md · boat-quote-contract.md
```

Los imports compartidos pueden usar los aliases configurados (`@`, `@components`,
`@features`, `@hooks`, `@lib`, `@styles` y `@utils`); dentro de `features/auth`, se
prefieren imports relativos para módulos propios de la feature.

## Notas

- La sección **Mockups** se eliminó: no forms parte de la presentación final.
- Las animaciones (`rise`, `fade`, `pop`, `float`, `marquee`) están en `src/styles/globals.css` y respetan
  `prefers-reduced-motion`.
- El estado de la demo se persiste en `sessionStorage` (`hostflow.app.v1`), por lo que recargar la
  página no pierde los cambios (incluida una clave nueva definida por OTP).
