# Refactor frontend — fases de migración feature-based

## Objetivo general

Organizar gradualmente el frontend de HostFlow por responsabilidad y dominio, preservando
el comportamiento visible y los contratos actuales. El alcance prioriza movimientos de
archivos, imports y nombres internos; no incluye rediseño de interfaces ni cambios de
negocio.

Cada fase debe compilar y ser revisable de forma independiente. Deben conservarse el modo
demo, el uso de `sessionStorage`, el manejo del token, los adaptadores HTTP/demo, los
contratos de `SessionContext` y las URLs públicas existentes.

## Estado actual observado

- La fase 1 está implementada: autenticación reside bajo `src/features/auth/`, con las
  páginas `LoginPage.jsx`, `CreateAccountPage.jsx`, `PasswordRecoveryPage.jsx` y el
  `SessionContext.jsx` de la feature.
- Home y las pantallas del panel del anfitrión están bajo `src/features/home/` y
  `src/features/dashboard/` tras completar la fase 2.
- La administración está bajo `src/features/admin/pages/AdminOverviewPage.jsx`.
- Las rutas están bajo `src/routes/index.jsx` y el guard está en `src/routes/guards/`.
- Los layouts compartidos están en `src/components/layout/`; las primitivas visuales en
  `src/components/ui/index.jsx`.
- La tarjeta de reserva está dentro de `features/dashboard/components/`.
- API, estado compartido y configuración de entorno están bajo `src/lib/api/`,
  `src/lib/state/` y `src/lib/config/` tras completar la fase 4.
- Tema, estilos globales y utilidades están bajo `src/lib/theme/`, `src/styles/` y
  `src/utils/` tras completar la fase 5.
- Los aliases `@`, `@components`, `@features`, `@hooks`, `@lib`, `@styles` y `@utils`
  están configurados en `vite.config.ts` y `jsconfig.json`.
- `contexto.md` describe actualmente la estructura posterior a la fase 5.

## Estructura de referencia al cerrar la fase 1

Este árbol conserva el estado histórico inmediatamente posterior a la primera fase; no
representa la estructura final del proyecto.

```text
src/
├── features/
│   └── auth/
│       ├── context/
│       │   └── SessionContext.jsx
│       └── pages/
│           ├── LoginPage.jsx
│           ├── CreateAccountPage.jsx
│           └── PasswordRecoveryPage.jsx
├── context/
│   └── ThemeContext.jsx
├── routes.jsx
├── components/
├── services/
├── data/
├── config/
└── lib/
```

Las carpetas y archivos no relacionados con autenticación permanecen donde están en
esta fase. `services/` y `data/` siguen siendo compartidos y no se trasladan.

## Fase 1 — módulo de autenticación (completada)

Se completó el movimiento de las pantallas de autenticación a `features/auth/pages/`, el
movimiento de `SessionContext` a `features/auth/context/`, la actualización de imports y
la documentación de estructura. La implementación del contexto, el almacenamiento y las
acciones de autenticación se conservaron.

Las URLs conservadas son `/auth/login`, `/auth/registro` y `/auth/recuperar`. La
validación ejecutada fue `pnpm build` desde `frontend/`, con resultado correcto.

## Fases siguientes

Cada fase se debe implementar por separado. No adelantar movimientos de fases posteriores
ni mezclar refactor estructural con cambios funcionales.

### Fase 2 — páginas públicas y panel del anfitrión (completada)

**Objetivo:** agrupar pantallas relacionadas bajo features de negocio, sin alterar cómo
leen o guardan los datos.

**Movimientos propuestos:**

- Home de presentación: `pages/Home.jsx` → `features/home/pages/HomePage.jsx`.
- Pantallas de `pages/app/` → `features/dashboard/pages/`:
  - `Dashboard.jsx` → `OwnerDashboardPage.jsx`
  - `Bookings.jsx` → `BookingsPage.jsx`
  - `Lodging.jsx` → `LodgingPage.jsx`
  - `Availability.jsx` → `AvailabilityPage.jsx`
  - `Payments.jsx` → `PaymentMethodsPage.jsx`
  - `Profile.jsx` → `LodgingProfilePage.jsx`
- Mantener `pages/admin/AdminDashboard.jsx` fuera de la feature del anfitrión en esta
  fase; su ubicación se resuelve en la fase 3.

**Imports:** las pantallas usan aliases para módulos compartidos (`@components/ui`,
`@lib/format`, `@features/auth/context/SessionContext`) y relativos solo para módulos que
vivan dentro de la misma feature. `@/` sigue disponible para `config/`, `services/` y
`data/`.

**Criterios de aceptación:** rutas y nombres de URL sin cambios; no mover ni reescribir
`store.js`, `useStore.js` ni los adaptadores de servicio; build correcto y comprobación
manual de Home, navegación al panel, reservas y pantallas de alojamiento.

Se completó el movimiento y renombre de las siete páginas a `features/home/pages/` y
`features/dashboard/pages/`. Se actualizaron sus imports con aliases compartidos y se
mantuvieron las rutas públicas y privadas. `pnpm build` terminó correctamente y no se
encontraron referencias de código a las antiguas ubicaciones. No se realizó prueba manual
de la UI.

### Fase 3 — layouts, componentes compartidos y administración (completada)

**Objetivo:** dejar clara la diferencia entre pantallas de negocio, layouts de navegación
y componentes visuales compartidos.

**Movimientos propuestos:**

- `components/ui.jsx` → `components/ui/index.jsx` o módulos bajo `components/ui/`, eligiendo
  una sola opción para esta fase. La alternativa de módulos individuales mejora la
  localización de cada primitiva, pero requiere actualizar todas sus importaciones.
- `components/AppLayout.jsx` y `layouts/AuthLayout.jsx` → `components/layout/`, con los
  nombres `AuthenticatedAppLayout.jsx` y `AuthLayout.jsx`, respectivamente.
- `components/BookingCard.jsx` → `features/dashboard/components/BookingSummaryCard.jsx`;
  mantener `StatusBadge` junto a la tarjeta hasta justificar su reutilización.
- `components/RequireAuth.jsx` → `routes/guards/ProtectedRoute.jsx`; conservar el control
  de sesión/rol y el manejo actual del evento de sesión expirada.
- `pages/admin/AdminDashboard.jsx` → `features/admin/pages/AdminOverviewPage.jsx`, porque
  el control interno y el rol `ADMIN` son un dominio distinto al panel del anfitrión.
- Mover `routes.jsx` a `routes/index.jsx` solo si también se actualizan el import en
  `App.jsx` y los imports de páginas, layouts y guardia. Mantener la tabla de rutas en un
  único punto; no cambiar paths ni estrategia de routing.

**Criterios de aceptación:** rutas públicas, privadas y de administrador idénticas; el
guard sigue redirigiendo de la misma manera; build correcto y revisión de navegación con
ambos roles de demostración.

Se eligió conservar el sistema visual como un único módulo en
`components/ui/index.jsx`, sin dividir ni cambiar sus exports. Los layouts se movieron a
`components/layout/`: `AppLayout` se renombró a `AuthenticatedAppLayout` y el layout de
autenticación conserva el nombre `AuthLayout`. La tarjeta se movió a
`features/dashboard/components/BookingSummaryCard.jsx`,
el guard a `routes/guards/ProtectedRoute.jsx`, la pantalla administrativa a
`features/admin/pages/AdminOverviewPage.jsx` y la definición de rutas a `routes/index.jsx`.
Los nombres de los componentes y sus imports se actualizaron; las rutas URL y las
condiciones de acceso se conservaron. `pnpm build` y `git diff --check` pasaron. No se
realizó prueba manual de la UI.

### Fase 4 — infraestructura compartida (API, estado y configuración) (completada)

**Objetivo:** hacer más explícito qué módulos son infraestructura compartida, sin cambiar
el contrato entre pantallas y la capa de datos.

**Movimientos propuestos:**

- `services/` → `lib/api/` como un conjunto: fachada (`index.js`), `httpApi.js`,
  `demoApi.js`, `httpClient.js`, `endpoints.js`, `tokenStorage.js` y `errors.js`.
- `data/store.js` y `data/useStore.js` → `lib/state/`, siempre juntos para conservar el
  vínculo entre store y hook.
- `config/env.js` → `lib/config/env.js`.
- Mantener `api` como puerta de acceso a datos que usan las features; las pantallas no
  deben empezar a llamar directamente a `fetch` ni a los adaptadores.
- No dividir todavía `api` por feature: las operaciones y el modo demo comparten contrato
  y estado.

**Imports:** desde features, usar aliases (`@lib/api`, `@lib/state`, `@lib/config`).
Dentro de `lib/api/` y `lib/state/`, conservar imports relativos para dependencias
internas cercanas.

**Criterios de aceptación:** mismo selector demo/HTTP, mismas operaciones y errores,
misma gestión de token y persistencia; build correcto. Comprobar modo demo y configuración
con `VITE_API_URL` sin cambiar el backend.

Se movieron los servicios a `src/lib/api/`, el store y su hook a `src/lib/state/`, y la
configuración de entorno a `src/lib/config/env.js`. Se actualizaron los imports de
features, componentes, guardias y dependencias internas; el contrato y la selección entre
adaptador demo/HTTP se conservaron. También se actualizaron las rutas de código citadas en
`contexto.md` y `docs/swagger-api.md`. `pnpm build` y `git diff --check` pasaron. No se
realizó una prueba manual con y sin `VITE_API_URL`.

### Fase 5 — tema, estilos, utilidades y recursos estáticos (completada)

**Objetivo:** separar recursos globales transversales de la lógica de las features.

**Movimientos propuestos:**

- `index.css` → `styles/globals.css`; actualizar únicamente el import desde `main.jsx`.
- `context/ThemeContext.jsx` → `lib/theme/ThemeContext.jsx`, manteniéndolo global porque se
  consume en Home, autenticación, componentes compartidos y panel.
- `lib/format.js` → `utils/format.js`, porque contiene funciones puras de formato y
  validación; mantener las funciones juntas y actualizar imports.
- No hay imágenes o SVG propios bajo `src/` que trasladar. `public/favicon.svg` se conserva
  en `public/` porque es un recurso estático servido desde la raíz; las fuentes continúan
  importándose desde sus paquetes npm.

**Criterios de aceptación:** persistencia de preferencia del tema en `localStorage` con la
misma clave; estilos, tokens, animaciones y `prefers-reduced-motion` intactos; build y
comprobación visual de modo claro/oscuro en rutas públicas y privadas.

Se movieron `index.css` a `styles/globals.css`, `ThemeContext.jsx` a `lib/theme/ThemeContext.jsx`
y `format.js` a `utils/format.js`. Se actualizaron imports a los aliases `@styles`, `@lib` y
`@utils`, así como la documentación de estructura. No se cambió la clave `hostflow.theme`,
la lógica de tema ni el contenido de estilos. `pnpm build` y `git diff --check` pasaron.
No se hizo una comprobación visual manual del tema.

### Fase 6 — cierre, consistencia y evaluación de nuevos dominios (en progreso)

**Objetivo:** asegurar que la estructura completa sea navegable y que no queden ubicaciones
antiguas ni imports redundantes.

**Tareas:**

- Actualizar `contexto.md` y la sección de estructura de este documento con el árbol final
  realmente implementado.
- Buscar imports que apunten a rutas previas, referencias a nombres anteriores y carpetas
  vacías; eliminar solo directorios vacíos que hayan quedado del refactor.
- Revisar nombres de archivos, componentes y exports contra la tabla de nombres sugeridos.
- Ejecutar `pnpm build`, `git diff --check` y los tests disponibles. Hacer una prueba manual
  de Home, login, registro, recuperación OTP, panel de anfitrión, administración y tema.
- Considerar `features/onboarding/` únicamente cuando se agreguen pantallas y lógica reales
  de onboarding B2C. No crear una feature vacía por anticipado.
- Crear `hooks/`, `types/` u otros subdirectorios solo al tener una responsabilidad o
  implementación concreta que ubicar allí.

**Criterios de aceptación:** build y tests disponibles pasan; no hay imports rotos ni
duplicación de puntos de entrada; se preservan URLs, roles, API/demo, almacenamiento y UX.

### Estructura final

Al concluir la fase 6, la estructura activa de `src/` es:

```text
src/
├── components/
│   ├── layout/
│   │   ├── AuthLayout.jsx
│   │   └── AuthenticatedAppLayout.jsx
│   └── ui/
│       └── index.jsx
├── features/
│   ├── admin/pages/AdminOverviewPage.jsx
│   ├── auth/
│   │   ├── context/SessionContext.jsx
│   │   └── pages/
│   │       ├── CreateAccountPage.jsx
│   │       ├── LoginPage.jsx
│   │       └── PasswordRecoveryPage.jsx
│   ├── dashboard/
│   │   ├── components/BookingSummaryCard.jsx
│   │   └── pages/
│   │       ├── AvailabilityPage.jsx
│   │       ├── BookingsPage.jsx
│   │       ├── OwnerDashboardPage.jsx
│   │       ├── LodgingPage.jsx
│   │       ├── LodgingProfilePage.jsx
│   │       └── PaymentMethodsPage.jsx
│   └── home/pages/HomePage.jsx
├── lib/
│   ├── api/
│   │   ├── demoApi.js
│   │   ├── endpoints.js
│   │   ├── errors.js
│   │   ├── httpApi.js
│   │   ├── httpClient.js
│   │   ├── index.js
│   │   └── tokenStorage.js
│   ├── config/env.js
│   ├── state/
│   │   ├── store.js
│   │   └── useStore.js
│   └── theme/ThemeContext.jsx
├── routes/
│   ├── guards/ProtectedRoute.jsx
│   └── index.jsx
├── styles/globals.css
├── utils/format.js
├── App.jsx
└── main.jsx
```

No se agrega una carpeta `assets/` vacía: no se encontraron imágenes o SVG propios dentro
de `src/`. Los archivos públicos, incluido `public/favicon.svg`, permanecen en `public/`.

El nombre del archivo y del componente del dashboard del anfitrión queda unificado en
`OwnerDashboardPage.jsx`; las rutas públicas y privadas permanecen sin cambios.

La revisión encontró directorios vacíos heredados de los movimientos; se eliminaron solo
los directorios vacíos `config/`, `services/`, `layouts/`, `data/`, `pages/` (incluidos
sus subdirectorios vacíos) y `context/`, además de `features/auth/components/`. Se
conservó `assets/` si ya existía como directorio vacío, para no borrar una ubicación
potencialmente intencional ajena a esos movimientos.

No hay archivos de test frontend ni script `test` declarado en `package.json`. La
validación automatizada disponible se limita al build de Vite y a `git diff --check`.
La revisión manual visual debe registrarse por separado si se realiza en el navegador.

## Política de imports para todas las fases

- **Relativos:** entre archivos cercanos de una misma feature o módulo, como una página y
  su contexto local (`../context/SessionContext`) o archivos internos de `lib/api/`.
- **Aliases existentes:** desde una feature a otra feature o hacia módulos compartidos.
  Usar el alias más específico disponible (`@features`, `@components`, `@lib`, etc.) y
  `@/` para carpetas bajo `src/` que no tengan alias propio.
- **No crear carpetas vacías** `api/`, `hooks/`, `types/` ni aliases adicionales sin una
  necesidad concreta.
- **No cambiar:** nombres de rutas URL, contratos de `actions`, estado, persistencia,
  contenido visual, validaciones, lógica OTP ni integración con backend/demo.

## Validación para cada fase

Desde `frontend/`:

```bash
pnpm build
```

Cada fase se considera completa cuando:

- el build termina correctamente;
- no quedan imports apuntando a las ubicaciones anteriores de los archivos trasladados;
- React Router conserva las mismas rutas, accesos y guardias;
- los flujos afectados por la fase mantienen su comportamiento;
- modo demo, token y persistencia conservan el comportamiento actual en `sessionStorage`
  o `localStorage`, según corresponda;
- el diff no contiene cambios de lógica funcional fuera de las actualizaciones
  necesarias de imports.

La prueba manual debe cubrir al menos las pantallas y los flujos afectados por esa fase.
En las fases que afectan autenticación o rutas, probar el acceso demo de anfitrión y
administrador, la navegación protegida y el recorrido de recuperación de contraseña. No
hace falta cambiar ni limpiar datos guardados para validar el refactor.

## Sugerencias de nombres por responsabilidad

La convención recomendada es sufijo `Page` para pantallas conectadas al router, nombres
de dominio para componentes visuales y `ProtectedRoute` para el guard de acceso. Aplicar
cada nombre sugerido al migrar el archivo correspondiente, sin renombrar componentes que
quedan fuera de la fase en curso.

| Nombre actual | Nombre sugerido | Motivo |
|---|---|---|
| `Login` | `LoginPage` | Es una pantalla pública con formulario y navegación, no un control reutilizable. |
| `Register` | `CreateAccountPage` | Expresa la tarea del usuario; `Register` no aclara qué registra. |
| `Recover` | `PasswordRecoveryPage` | Explicita que la pantalla recupera la contraseña e incluye el flujo OTP. |
| `Home` | `HomePage` | Deja claro que es la página pública de presentación. |
| `Dashboard` | `OwnerDashboardPage` | Identifica el panel del propietario y lo distingue del panel de administración. |
| `Bookings` | `BookingsPage` | Identifica la pantalla de listado/gestión de reservas. Mantener “Booking” también en el código evita mezclar el contrato actual de `bookings`. |
| `Lodging` | `LodgingPage` | Identifica la pantalla que gestiona datos del alojamiento. |
| `Availability` | `AvailabilityPage` | Identifica la pantalla de disponibilidad/calendario. |
| `Payments` | `PaymentMethodsPage` | Es más preciso: la pantalla configura monedas y métodos de pago, no procesa pagos. |
| `Profile` | `LodgingProfilePage` | Aclara que el perfil corresponde a la marca/empresa del alojamiento, no al perfil personal del usuario. |
| `AdminDashboard` | `AdminOverviewPage` | Hace explícita su responsabilidad de control interno y evita la ambigüedad de “dashboard”. |
| `BookingCard` | `BookingSummaryCard` | Describe el resumen visible de una reserva y sus acciones. |
| `RequireAuth` | `ProtectedRoute` | Nombra el patrón de enrutamiento; el componente también aplica el rol `ADMIN`. |
| `AuthLayout` | `AuthLayout` (conservado) | Nombre conciso y claro para el layout compartido por las páginas públicas de autenticación. |
| `AppLayout` | `AuthenticatedAppLayout` | Expresa que es el shell privado usado por anfitrión y administrador. |

Estas sugerencias no implican modificar copy visible ni URLs. En particular, conservar
`/auth/registro` aunque el archivo pase a llamarse `CreateAccountPage.jsx` separa el
nombre interno de la ruta pública y evita romper enlaces existentes.

## Reglas para todas las fases

- Cambiar una fase por commit o unidad de revisión, ejecutar sus validaciones antes de
  avanzar y evitar mezclar movimientos funcionales distintos.
- No cambiar JSX a TSX ni agregar dependencias como parte de este refactor.
- No alterar almacenamiento, datos demo, llamadas HTTP, endpoints, permisos, UX ni textos.
- No crear archivos `index.js` solo como barriles de exportación salvo que mejoren imports
  de forma concreta y se compruebe que no crean ciclos.
- Preferir imports relativos entre archivos cercanos de una misma feature e imports con
  alias entre features o hacia infraestructura global.
