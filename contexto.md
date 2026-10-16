# HostFlow · Frontend — módulo de autenticación

Versión reducida del MVP con **solo** Login, Registro y Recuperación de contraseña.
React 19 · Vite · Tailwind CSS 4 · React Router.

## Cómo correrlo

```bash
pnpm install        # o npm install
pnpm dev            # http://localhost:5173
pnpm build          # genera dist/
```

Sin `VITE_API_URL` (ver `.env.example`) la app corre en **modo demo**: no valida credenciales,
no guarda nada y muestra un aviso visible. Cualquier correo/contraseña válidos permiten ingresar.

## Rutas

| Ruta | Pantalla |
|---|---|
| `/` | Home de presentación de HostFlow (enlaces a ingresar y crear cuenta) |
| `/auth/login` | Ingreso |
| `/auth/registro` | Crear cuenta |
| `/auth/recuperar` | Recuperar contraseña (solo solicitud del correo) |
| `/panel` | **Pantalla temporal** posterior al ingreso (protegida, con "Cerrar sesión"). Reemplazarla por el panel real. |

## Estructura

```
src/
  pages/auth/     Login · Register · Recover   (originales)
  pages/          Home (presentación) · SessionStarted (temporal)
  layouts/        AuthLayout                    (original)
  components/     ui.jsx (Brand, Button, Field, DemoBanner) · RequireAuth.jsx
  services/       index (api) · httpApi | demoApi · endpoints · httpClient · tokenStorage · errors
  config/env.js   VITE_API_URL → DEMO_MODE
  lib/format.js   cn, isEmail
  index.css       sistema visual original completo
```

## Conectar el backend Java Spring Boot

1. Definir `VITE_API_URL`.
2. Completar `path` de `login`, `register` y `recoverPassword` en `src/services/endpoints.js`
   (hoy son `null`: la interfaz avisa "conexión no configurada" y nunca finge una respuesta).
3. Datos de entrada/respuesta que espera el frontend y todo lo **pendiente de definir**
   (Bearer vs cookie, correo duplicado, flujo posterior de recuperación, etc.):
   ver el comentario al inicio de `src/services/httpApi.js`.
