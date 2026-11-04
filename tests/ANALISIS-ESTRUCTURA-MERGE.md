# Análisis de estructura — Merge `ce4f2c5`

**Rama:** `QA-review`
**Merge analizado:** `ce4f2c5` — *Merge branch 'feat/front/reservations-and-client-onboarding'*
**Base:** `9a76102` (QA-review) + `a329f43` (feat/front)
**Alcance:** 50 archivos, +8660 / -605 líneas (todo frontend).

---

## Resumen ejecutivo

`npm run build` compila correctamente: **no hay errores de sintaxis, imports rotos ni
dependencias faltantes**. Los problemas detectados son **arquitectónicos y de
organización del código**, no de compilación.

| # | Tipo de error | Severidad |
|---|---|---|
| 1 | Violación de capas (UI → store directo, `httpApi` → store demo) | Alta |
| 2 | Archivos monolito / falta de separación de responsabilidades | Alta |
| 3 | Duplicación de lógica / múltiples fuentes de verdad | Media |
| 4 | Convenciones de imports y estructura inconsistentes | Media |
| 5 | Documentación de estructura desalineada con el código | Baja |
| 6 | Ubicación de documentos y artefactos | Baja |

---

## 1. Violación de capas (severidad: alta)

La arquitectura documentada en `frontend/contexto.md` establece que las pantallas
importan `api` (fachada) y **nunca** hablan con `fetch` ni con el store directamente.

La realidad del merge la contradice:

- **11 archivos de `features/` importan `useStore`/`store` directamente**, salteando la
  fachada `@lib/api`:
  - `features/admin/pages/AdminOverviewPage.jsx`
  - `features/dashboard/components/BookingDetailModal.jsx`
  - `features/dashboard/components/BookingFormModal.jsx`
  - `features/dashboard/pages/AvailabilityPage.jsx`
  - `features/dashboard/pages/BookingsPage.jsx`
  - `features/dashboard/pages/LodgingPage.jsx`
  - `features/dashboard/pages/LodgingProfilePage.jsx`
  - `features/dashboard/pages/OwnerDashboardPage.jsx`
  - `features/dashboard/pages/PaymentMethodsPage.jsx`
  - `features/dashboard/pages/ServicesPage.jsx`
  - `features/onboarding/pages/OnboardingPage.jsx`
- Solo `features/auth/context/SessionContext.jsx` usa la fachada `@lib/api`.

Además:

- `src/lib/api/httpApi.js:26-27` — el adaptador **real** importa `actions`, `store`,
  `MAX_IMAGES` y `MAX_PAYMENT_METHODS` del **store de demostración**. Esto acopla la
  capa HTTP con el estado demo y difumina la frontera real/demo.
- `src/routes/guards/ProtectedRoute.jsx:4` importa `@lib/api/httpClient` directamente,
  saltándose la fachada `index.js`.

**Efecto:** no existe una frontera real `UI → servicio/hook → datos`; demo y backend
real están entrelazados y el reemplazo de un adaptador no es transparente.

---

## 2. Archivos monolito / falta de separación de responsabilidades (severidad: alta)

Archivos que superan ampliamente un tamaño razonable y concentran demasiadas
responsabilidades:

| Archivo | Líneas |
|---|---|
| `features/onboarding/pages/OnboardingPage.jsx` | 1393 |
| `features/home/pages/HomePage.jsx` | 1143 |
| `lib/state/store.js` | 923 |
| `lib/api/httpApi.js` | 747 |
| `features/dashboard/pages/LodgingProfilePage.jsx` | 723 |
| `features/dashboard/pages/PaymentMethodsPage.jsx` | 719 |
| `components/ui/index.jsx` | 554 |

- `store.js` mezcla: datos semilla, persistencia/migración, reglas de negocio y
  acciones en un único módulo.
- `components/ui/index.jsx` agrupa ~30 componentes en un solo archivo, mientras
  `components/ui/Modal.jsx` queda fuera del barrel (se importa por ruta directa
  `@components/ui/Modal`). La API del sistema visual es inconsistente.
- No se extraen hooks ni servicios por feature; la lógica de páginas grandes queda
  dentro del componente.

---

## 3. Duplicación de lógica / múltiples fuentes de verdad (severidad: media)

- `getAdminStats` está implementado **3 veces**:
  - `src/lib/api/index.js:47`
  - `src/lib/api/demoApi.js:23`
  - `src/lib/api/httpApi.js:680`
- `src/lib/api/demoApi.js:5` hace `...actions` y además reimplementa getters que ya
  existen en el store.
- Reglas de `.gitignore` duplicadas y solapadas entre la raíz y `frontend/.gitignore`.
- La lógica de fallback a datos locales aparece replicada en `index.js`, `demoApi.js`
  y `httpApi.js`, lo que aumenta el riesgo de divergencia.

---

## 4. Convenciones de imports y estructura inconsistentes (severidad: media)

- Alias mezclados en el mismo archivo: `src/routes/index.jsx:3` usa `@components/...`
  y la línea 4 usa `@/components/...`.
- Contextos ubicados en dos lugares distintos según la feature: `features/auth/context/`
  vs `lib/theme/` (`ThemeContext`, `LodgingTheme`).
- Estructura de `features` no homogénea: `dashboard` tiene `components/` y `lib/`,
  pero `onboarding` y `admin` no tienen `lib/`; no hay barrels (`index`) por feature.
- Documentación vs realidad de imports: `contexto.md` indica imports relativos dentro
  de `features/auth`, pero el resto del proyecto mezcla alias y rutas directas.

---

## 5. Documentación de estructura desalineada con el código (severidad: baja)

- `frontend/contexto.md` lista `lib/format.js`, pero el archivo real es
  `src/utils/format.js`. Tampoco menciona `src/utils/color.js`,
  `src/lib/theme/patterns.js` ni `src/lib/theme/extractPalette.js`.
- `contexto.md` afirma que el estado demo se persiste en **`sessionStorage`**, pero
  `src/lib/state/store.js:363-387` usa **`localStorage`** (el `sessionStorage` solo se
  usa como migración legacy y en `tokenStorage.js`).
- La sección "Estructura" de la documentación no refleja la organización real de
  `features/` ni la ubicación de los documentos.

---

## 6. Ubicación de documentos y artefactos (severidad: baja)

- `frontend/contexto.md` y `frontend/docs/` conviven con configuración y código fuente.
- `revisiones.md` y `tests/` viven en la raíz; `frontend/docs/` en el subproyecto.
  No hay una convención única para documentación.
- `frontend/vercel.json` correcto (rewrites SPA + headers), pero la configuración de
  despliegue está dentro de `frontend/` sin documentarse en la estructura.

---

## Recomendaciones (orden sugerido)

1. **Definir la frontera de capas:** las páginas deben consumir hooks/servicios
   (`useBookings`, `useLodging`, etc.) que llamen a `api`; prohibir imports directos de
   `store`/`useStore` desde `features/` (excepto el provider de sesión).
2. **Desacoplar `httpApi` del store demo:** mover constantes de dominio fuera de
   `store.js` y resolver el fallback en un único punto (`api/index.js`).
3. **Dividir monolitos:** extraer hooks, servicios y subcomponentes de
   `OnboardingPage`, `HomePage`, `store.js` y `httpApi.js`.
4. **Unificar el sistema UI:** exportar `Modal` desde el barrel `components/ui` o
   migrar todo a archivos por componente.
5. **Homogeneizar estructura de features** (pages / components / hooks / lib / index)
   y una única convención de imports (alias).
6. **Actualizar `contexto.md`** para que la sección "Estructura" coincida con el árbol
   real y con `localStorage` como persistencia del demo.

---

## Verificación

- Comando ejecutado: `npm run build` (en `frontend/`).
- Resultado: build exitoso (1966 módulos transformados). Aviso no bloqueante de chunk
  > 500 kB (`dist/assets/index-*.js` ≈ 574 kB), relacionado con la falta de code
  splitting señalada en el punto 2.
