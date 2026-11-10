# HostFlow — Rutas del frontend y destino del enlace de reserva

> **Fuentes revisadas:** ZIP actual del frontend, ZIP actual del backend, documentación interna del frontend y documentación oficial de Vercel.  
> **Importante:** el frontend y backend todavía no están integrados. Este documento identifica rutas reales, rutas que faltan y la configuración recomendada; no afirma que el onboarding del huésped ya funcione en Vercel.

## 1. Conclusión ejecutiva

El enlace de reserva debe llevar al **frontend**, no directamente al endpoint REST. El frontend debe leer el `token` de la URL y consultar el backend para mostrar los datos del onboarding.

El dominio Vercel que compartiste para el demo es `https://hostflow-deploy.vercel.app`. Las direcciones que identificaste son:

- Página principal: [https://hostflow-deploy.vercel.app/](https://hostflow-deploy.vercel.app/)
- Panel de reservas: [https://hostflow-deploy.vercel.app/panel/reservas](https://hostflow-deploy.vercel.app/panel/reservas)
- Formulario simulado del huésped: [https://hostflow-deploy.vercel.app/llegada/lod_1/bk_mwa06xx0](https://hostflow-deploy.vercel.app/llegada/lod_1/bk_mwa06xx0)

La ruta que el backend ya espera utilizar para el flujo real es:

```text
https://hostflow-deploy.vercel.app/reservations/confirm?token={TOKEN}
```

**Importante:** el formulario `/llegada/lod_1/bk_mwa06xx0` es la URL del flujo simulado que compartiste. No debemos asumir que acepta el token de onboarding del backend: usa segmentos de ruta (`lod_1` y `bk_mwa06xx0`), mientras que el backend genera un enlace con `?token=...`. Son contratos de URL distintos.

Además, en el ZIP del frontend revisado **no aparece** `/reservations/confirm`, ni una pantalla de onboarding conectada al backend. El router del ZIP tiene un catch-all que redirige las rutas desconocidas a `/auth/login`. Por tanto, el dominio ya está identificado, pero la página real de confirmación todavía debe implementarse e integrarse.

## 2. Rutas reales encontradas en el frontend

El router actual (`src/routes/index.jsx`) define:

| Ruta | Estado | Acceso |
|---|---|---|
| `/` | Existe: Home de presentación | Público |
| `/auth/login` | Existe | Público |
| `/auth/registro` | Existe | Público |
| `/auth/recuperar` | Existe | Público |
| `/panel` | Existe | Protegido |
| `/panel/reservas` | Existe: listado del propietario | Protegido |
| `/panel/alojamiento` | Existe | Protegido |
| `/panel/disponibilidad` | Existe | Protegido |
| `/panel/pagos` | Existe | Protegido |
| `/panel/perfil` | Existe | Protegido |
| `/admin` | Existe | Protegido para ADMIN |
| `/llegada/:lodgingId/:bookingId` | URL del formulario simulado compartida para el despliegue: `/llegada/lod_1/bk_mwa06xx0`. No aparece en el router del ZIP revisado; confirmar si el despliegue usa otro commit o si la ruta la resuelve otra capa. | Demo de huésped |
| `/reservations/confirm` | **No existe en el ZIP revisado** | Debe ser público; destino recomendado para el enlace real con token |
| `/reservas/confirmar` | **No existe** | Alternativa, no seleccionada |

El router incluye:

```jsx
{ path: "*", element: <Navigate to="/auth/login" replace /> }
```

Así que si el enlace actual llega a una ruta desconocida, React Router lo enviará al login en lugar de mostrar el onboarding.

## 3. Qué hace el backend con la URL

En `ReservationService.java` se lee esta propiedad:

```java
@Value("${app.reservations.frontend-confirm-url:http://localhost:3000/reservations/confirm}")
private String frontendConfirmUrl;
```

Al generar el enlace, el servicio concatena el token:

```java
String confirmationUrl = frontendConfirmUrl + "?token=" + token;
```

La propiedad también aparece en `src/main/resources/application.properties`:

```properties
app.reservations.frontend-confirm-url=http://localhost:3000/reservations/confirm
```

**Observación local:** el contexto del frontend documenta Vite en `http://localhost:5173`, no en `localhost:3000`. El valor actual de `application.properties` no coincide con el puerto documentado para el frontend local, salvo que el equipo lo ejecute con un puerto alternativo.

## 4. Endpoints reales del backend (no confundir con las páginas)

El controlador `PublicReservationController` tiene:

| Método | Endpoint backend | Propósito |
|---|---|---|
| GET | `/api/v1/public/reservations/confirm?token=...` | Consultar datos para renderizar onboarding |
| POST | `/api/v1/public/reservations/confirm?token=...` | Enviar onboarding completo |

Estos endpoints no son direcciones para colocar en el correo. Son llamadas que realizará la página del frontend después de abrir el enlace.

La URL pública final tendrá dos partes distintas:

```text
Página web:
https://hostflow-deploy.vercel.app/reservations/confirm?token={TOKEN}

API:
https://{DOMINIO-BACKEND}/api/v1/public/reservations/confirm?token={TOKEN}
```

## 5. Estado de integración del frontend

El ZIP actual confirma que:

- `VITE_API_URL` vacío activa `DEMO_MODE`.
- `src/lib/api/index.js` selecciona `demoApi` cuando no hay `VITE_API_URL`.
- `src/lib/api/endpoints.js` solo define operaciones de autenticación y propietarios; no tiene endpoints de reservas.
- No hay una feature de onboarding de huésped conectada al backend ni una página que lea `token` desde la URL.
- No se encontró `vercel.json` en la raíz del frontend entregado.
- El ZIP no contiene el dominio Vercel, pero el usuario confirmó que el demo está publicado en `https://hostflow-deploy.vercel.app`.
- El usuario compartió una ruta de formulario simulado `/llegada/lod_1/bk_mwa06xx0`; esa ruta no aparece en el router del ZIP revisado. Esto puede indicar que el deployment usa una versión distinta del código, que la ruta se genera en otro módulo o que el ZIP no corresponde exactamente al deployment.

Por tanto, **el dominio sí está identificado**, pero todavía no se puede considerar que la ruta del formulario simulado sea un destino válido para el enlace real con token del backend.

## 6. Recomendación de ruta

Mantener `/reservations/confirm` por ahora.

Motivos:
1. Es la ruta que el backend ya tiene configurada.
2. No hay una ruta actual con la que entre en conflicto.
3. Permite que el correo y el backend permanezcan estables mientras se construye la página.
4. La página puede añadirse como ruta pública, fuera de `ProtectedRoute`.

No recomiendo cambiar ahora a `/reservas/confirmar` solo por traducir el path: obligaría a modificar la configuración backend y no aporta una ventaja funcional.

La pantalla futura debe:
1. Leer `token` de `window.location` / React Router.
2. Validar que el token esté presente.
3. Hacer GET al endpoint público para recuperar datos.
4. Mostrar onboarding y enviar POST al mismo endpoint.
5. Mostrar estados de enlace vencido, inválido, usado o reserva cancelada.
6. No exigir login del propietario ni del huésped.

## 7. Configuración por ambiente recomendada

Sí conviene usar variables/configuración. El backend ya está preparado para ello mediante `@Value`.

### Backend local

El frontend documenta Vite en `5173`, por lo que la propiedad local recomendada es:

```properties
app.reservations.frontend-confirm-url=http://localhost:5173/reservations/confirm
```

### Backend desplegado

Configurar en el entorno de producción del backend:

```text
APP_RESERVATIONS_FRONTEND_CONFIRM_URL=https://hostflow-deploy.vercel.app/reservations/confirm
```

Spring Boot mapea esa variable de entorno a `app.reservations.frontend-confirm-url`.

No conviene hardcodear el dominio de Vercel en `ReservationService.java`. La misma compilación del backend debe poder usar localhost, preview o producción según el entorno.

### Frontend

Cuando se integre contra el backend real, configurar en Vercel:

```text
VITE_API_URL=https://{DOMINIO-BACKEND}/api/v1
```

Esta variable es distinta de `APP_RESERVATIONS_FRONTEND_CONFIRM_URL`:
- `APP_RESERVATIONS_FRONTEND_CONFIRM_URL`: backend → dirección del navegador que abre el huésped.
- `VITE_API_URL`: frontend → dirección base de la API.

`VITE_API_URL` se incorpora durante el build de Vite, así que debe definirse en el entorno de Vercel y generar un nuevo deployment.

## 8. Configuración de Vercel para rutas profundas

El frontend es Vite + React Router en modo SPA. En el ZIP no aparece `vercel.json`.

La documentación oficial de Vercel indica que, para una SPA Vite, hay que reescribir las rutas profundas a `index.html`; de lo contrario, abrir directamente una URL como `/reservations/confirm?token=...` puede devolver 404 antes de que React Router cargue.

Configuración recomendada cuando se agregue la página, en `frontend/vercel.json` (raíz del proyecto que Vercel construye):

```json
{
  "$schema": "https://openapi.vercel.sh/vercel.json",
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

Referencia oficial: [Vite on Vercel — Using Vite to make SPAs](https://vercel.com/docs/frameworks/frontend/vite).

## 9. Dominio Vercel y enlaces compartidos

El dominio de producción/demo fue proporcionado directamente por el usuario:

`https://hostflow-deploy.vercel.app`

| Uso | URL compartida | Interpretación |
|---|---|---|
| Home | [hostflow-deploy.vercel.app/](https://hostflow-deploy.vercel.app/) | Página principal del demo |
| Panel de reservas | [hostflow-deploy.vercel.app/panel/reservas](https://hostflow-deploy.vercel.app/panel/reservas) | Panel del propietario; coincide con la ruta `/panel/reservas` del ZIP |
| Formulario simulado de huésped | [hostflow-deploy.vercel.app/llegada/lod_1/bk_mwa06xx0](https://hostflow-deploy.vercel.app/llegada/lod_1/bk_mwa06xx0) | Flujo de demostración con identificadores en la ruta; no está documentado en el router del ZIP revisado |

No pude inspeccionar el contenido desplegado desde la herramienta de navegación en esta revisión; por ello, las tres URLs se registran como enlaces compartidos por el usuario, no como una verificación técnica independiente.

### Diferencia clave: demo frente a enlace real

La URL simulada usa este patrón:

```text
/llegada/{lodgingId}/{bookingId}
```

El backend, en cambio, genera un token de onboarding y el servicio lo añade como query parameter. Para mantener el contrato ya implementado en backend, la dirección recomendada del correo es:

```text
https://hostflow-deploy.vercel.app/reservations/confirm?token={TOKEN}
```

No cambiaría `ReservationService` para apuntar a `/llegada/...` hasta confirmar que esa página puede recibir el token, consultar `GET /api/v1/public/reservations/confirm?token=...` y enviar el onboarding mediante POST. El identificador de reserva de demo (`bk_mwa06xx0`) no debe tratarse como si fuera el token de seguridad.

## 10. Qué falta para cerrar esta decisión

- [x] Confirmar el dominio de producción de Vercel: `https://hostflow-deploy.vercel.app` (informado por el usuario).
- [ ] Confirmar si el proyecto de Vercel está conectado al mismo commit/código que el ZIP revisado y cuál es su Root Directory. La ruta `/llegada/lod_1/bk_mwa06xx0` compartida no aparece en el router del ZIP.
- [ ] Crear la página pública `/reservations/confirm` (no reutilizar `/llegada/:lodgingId/:bookingId` sin adaptar explícitamente su contrato para recibir y validar el token del backend).
- [ ] Implementar las llamadas GET y POST al backend en el adaptador API.
- [ ] Configurar el rewrite SPA de Vercel y probar acceso directo con query string.
- [ ] Configurar `APP_RESERVATIONS_FRONTEND_CONFIRM_URL` en el backend desplegado.
- [ ] Configurar `VITE_API_URL` en Vercel cuando la integración esté lista.
- [ ] Probar un enlace real completo desde correo en un navegador sin sesión iniciada.

## 11. Referencias

- Código del frontend: `src/routes/index.jsx`, `src/lib/api/endpoints.js`, `src/lib/api/index.js`, `src/lib/config/env.js`, `.env.example`, `contexto.md`.
- Código del backend: `ReservationService.java`, `PublicReservationController.java`, `application.properties`.
- [Documentación oficial de Vercel para Vite](https://vercel.com/docs/frameworks/frontend/vite).
- [Documentación oficial de Vercel sobre rewrites](https://vercel.com/docs/routing/rewrites).
