# Documentación de módulos de HostFlow (Autenticación, Dueños, Alojamiento, Monedas, Precios públicos y Servicios adicionales)

> Documentación del código de cada módulo, sus tablas, sus endpoints y una guía de pruebas paso a paso.
> Basada en el código fuente del backend `com.hostflow` (Spring Boot 3.2.5, Java 17, PostgreSQL, JWT).
> Complementa el documento **Módulo: Gestión del alojamiento** (perfil, galería, logotipo y métodos de pago).

## Índice

0. [Cómo usar esta guía de pruebas](#0-cómo-usar-esta-guía-de-pruebas)
1. [Módulo: Autenticación](#1-módulo-autenticación)
2. [Módulo: Dueños de Alojamiento](#2-módulo-dueños-de-alojamiento)
3. [Módulo: Gestión de Alojamiento (desactivación temporal)](#3-módulo-gestión-de-alojamiento-desactivación-temporal)
4. [Módulo: Monedas del alojamiento](#4-módulo-monedas-del-alojamiento)
5. [Módulo: Precios públicos](#5-módulo-precios-públicos)
6. [Módulo: Servicios adicionales](#6-módulo-servicios-adicionales)
7. [Resumen de hallazgos para revisar](#7-resumen-de-hallazgos-para-revisar)
8. [Ubicación de la implementación](#8-ubicación-de-la-implementación)

---

## 0. Cómo usar esta guía de pruebas

### 0.1 Datos generales de la API

| Dato | Valor |
|---|---|
| URL base local | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/api/v1/swagger-ui/index.html` |
| Formato de datos | JSON (`Content-Type: application/json`), salvo la subida de imágenes (`multipart/form-data`) |
| Autenticación | Token JWT en el header `Authorization: Bearer <token>` |
| Duración del token | 24 horas (`jwt.expiration=86400000`) |
| Roles | `ROLE_OWNER` (dueño de alojamiento) y `ROLE_ADMIN` (administrador) |
| Fechas | Formato ISO sin zona horaria, por ejemplo `2026-11-10T14:00:00` |

Todas las rutas de esta guía se escriben **sin** el prefijo `/api/v1` en las tablas, pero en Postman o Swagger se debe usar la URL completa, por ejemplo `http://localhost:8080/api/v1/auth/login`.

### 0.2 Herramientas para probar

Se puede usar cualquiera de estas dos:

- **Swagger UI** (más simple): abrir la URL de Swagger, desplegar el endpoint, pulsar **Try it out**, completar el cuerpo y pulsar **Execute**. Para endpoints protegidos, primero pulsar el botón **Authorize** (candado), pegar **solo el token** (Swagger agrega la palabra `Bearer` por sí mismo) y pulsar **Authorize**.
- **Postman**: crear una variable de entorno `baseUrl = http://localhost:8080/api/v1` y otra `token`. En la pestaña **Authorization** de cada petición elegir **Bearer Token** y escribir `{{token}}`.

### 0.3 Orden recomendado de pruebas (dependencias entre módulos)

Los módulos dependen unos de otros. Para no encontrar errores "falsos", probar en este orden:

1. **Autenticación**: registrar un dueño → verificar su correo → iniciar sesión → guardar el `token`.
2. **Gestión del alojamiento (perfil)**: crear el perfil con `POST /owners/accommodation/profile` (documentado en el documento *Módulo: Gestión del alojamiento*). **Sin este paso, los módulos 4, 5 y 6 responden 404** con el mensaje *"Registra primero los datos generales del alojamiento…"*.
3. **Monedas del alojamiento**: habilitar ARS/USD/EUR.
4. **Servicios adicionales**: crear servicios e imágenes.
5. **Precios públicos**: consultar sin token los precios convertidos.
6. **Dueños** (requiere un administrador) y **Desactivación temporal** se pueden probar en cualquier momento después del paso 1.

### 0.4 Cómo obtener los códigos y enlaces sin depender del correo

La aplicación envía correos reales por SMTP (Gmail con contraseña de aplicación). Si el correo no está configurado o no llega, se puede leer el código directamente de la base de datos PostgreSQL (`hostflow_dev`):

```sql
-- Token del enlace de verificación de correo (el más reciente de un usuario)
SELECT token, expires_at, used
FROM email_verification_tokens
WHERE email = 'dueno1@example.com'
ORDER BY created_at DESC LIMIT 1;

-- Código de 6 dígitos de recuperación de contraseña (el más reciente)
SELECT token AS codigo, expires_at, used
FROM password_reset_tokens
WHERE email = 'dueno1@example.com'
ORDER BY created_at DESC LIMIT 1;
```

### 0.5 Cómo probar con el rol ADMIN

El sistema crea automáticamente un administrador al arrancar (clase `DataInitializer`):

- Correo: `adminhostflow@gmail.com`
- Contraseña inicial: la definida en `DataInitializer.java` (cambiarla antes de producción).

**Importante:** ese administrador se crea con `email_verified = false`, y el login rechaza a los usuarios con correo no verificado. Para poder iniciar sesión como administrador hay que marcarlo como verificado una sola vez:

```sql
UPDATE owners SET email_verified = true WHERE email = 'adminhostflow@gmail.com';
```

### 0.6 Formato de errores de la API

Los errores controlados siguen este formato (clase `GlobalExceptionHandler`):

```json
{
  "timestamp": "2026-10-28T10:15:30.123",
  "status": 400,
  "error": "Bad Request",
  "message": "Mensaje explicativo",
  "errorCode": "CODIGO_OPCIONAL"
}
```

Los errores de validación de campos (`@Valid`) responden `400` con este formato:

```json
{
  "timestamp": "2026-10-28T10:15:30.123",
  "status": 400,
  "error": "Error de validación",
  "message": "Los datos enviados no son válidos",
  "fieldErrors": { "email": "El formato del correo electrónico no es válido" }
}
```

| Excepción | Código HTTP | Cuándo ocurre |
|---|---:|---|
| `BusinessException` | 400 | Regla de negocio incumplida (credenciales, tokens, fechas, tasas, límites). |
| `MethodArgumentNotValidException` | 400 | Un campo del JSON no cumple las validaciones. |
| `ResourceNotFoundException` | 404 | El recurso no existe o no pertenece al usuario. |
| `BadCredentialsException` / `UsernameNotFoundException` | 401 | Credenciales inválidas detectadas por Spring Security. |
| `CloudinaryUploadException` / `CloudinaryDeleteException` | 502 | Falla del servicio de imágenes. |
| Cualquier otra `Exception` | 500 | Error inesperado (`INTERNAL_ERROR`). |

> **Nota para quien prueba:** como el manejador global captura cualquier `Exception` y responde 500, algunos casos que normalmente serían 400 o 403 pueden aparecer como 500 en este proyecto (por ejemplo, un parámetro obligatorio ausente, un enumerado inválido, un JSON mal formado o un acceso denegado por rol). En cada módulo se indica el resultado **esperado según el diseño** y, cuando el código sugiere otra cosa, se marca con ⚠️ para que se anote lo que realmente responde.

### 0.7 Códigos de respuesta por falta de autenticación

Las rutas protegidas sin token (o con token inválido) no están cubiertas por un manejador propio: lo esperable es **403** sin token, y un token vencido o mal formado puede terminar en **500** porque el error ocurre en el filtro JWT, antes de llegar al manejador de errores. Es parte de lo que se debe verificar en las pruebas de seguridad (ver cada módulo).

---

## 1. Módulo: Autenticación

Swagger: `http://localhost:8080/api/v1/swagger-ui/index.html#/Autenticaci%C3%B3n`

### 1.1 Alcance

El módulo permite:

- Registrar un nuevo dueño de alojamiento y enviarle un correo de verificación.
- Verificar el correo electrónico mediante un enlace con token de un solo uso.
- Reenviar el enlace de verificación.
- Iniciar sesión y obtener un token JWT.
- Recuperar la contraseña mediante un código de 6 dígitos enviado por correo.
- Proteger las cuentas contra ataques de fuerza bruta (bloqueo temporal tras 5 intentos fallidos).

Todos los endpoints de este módulo son **públicos** (no requieren token): la ruta `/auth/**` está en la lista de accesos permitidos de `SecurityConfig`.

### 1.2 Modelo de datos

#### `owners`

| Columna | Tipo | Nulo | Descripción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. |
| `full_name` | `VARCHAR(150)` | No | Nombre y apellido. |
| `accommodation_name` | `VARCHAR(200)` | No | Nombre del alojamiento indicado al registrarse. |
| `email` | `VARCHAR(150)` | No | Correo; **único**. Se usa como nombre de usuario. |
| `password` | `VARCHAR` | No | Contraseña cifrada con BCrypt (nunca se devuelve). |
| `terms_accepted` | `BOOLEAN` | No | Aceptación de términos y política de privacidad. |
| `enabled` | `BOOLEAN` | No | Cuenta activa. Un administrador puede desactivarla. |
| `disabled_at` | `TIMESTAMP` | Sí | Fecha en que un administrador deshabilitó la cuenta. |
| `email_verified` | `BOOLEAN` | No | `false` al registrarse; pasa a `true` al verificar el correo. |
| `blocked_until` | `TIMESTAMP` | Sí | Si tiene valor futuro, la cuenta está bloqueada por seguridad hasta esa fecha. |
| `created_at` / `updated_at` | `TIMESTAMP` | No | Auditoría. |

Relación muchos-a-muchos con `roles` mediante la tabla `owner_roles` (`owner_id`, `role_id`). Los roles son `ROLE_OWNER` y `ROLE_ADMIN` (tabla `roles`, columna `role_name`).

#### `email_verification_tokens`

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | `BIGSERIAL` | PK. |
| `token` | `VARCHAR(100)` | UUID aleatorio, **único**. Va en el enlace del correo. |
| `email` | `VARCHAR(150)` | Correo al que pertenece. |
| `created_at` / `expires_at` | `TIMESTAMP` | Creación y vencimiento (60 minutos por defecto). |
| `used` | `BOOLEAN` | `true` cuando ya se usó o fue reemplazado por uno nuevo. |

#### `password_reset_tokens`

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | `BIGSERIAL` | PK. |
| `token` | `VARCHAR(10)` | Código numérico de 6 dígitos. |
| `email` | `VARCHAR(150)` | Correo asociado. |
| `created_at` / `expires_at` | `TIMESTAMP` | Creación y vencimiento (60 minutos). |
| `used` | `BOOLEAN` | `true` después de restablecer la contraseña. |

#### `login_attempts`

Registra cada intento de inicio de sesión (exitoso o fallido): `owner_id`, `email`, `full_name`, `role`, `success`, `blocked`, `blocked_until`, `attempt_at`, `ip_address`, `user_agent`. Es la base para contar intentos fallidos y bloquear cuentas.

### 1.3 Configuración que afecta al módulo

| Propiedad | Valor por defecto | Efecto |
|---|---|---|
| `app.security.max-failed-attempts` | `5` | Intentos fallidos permitidos antes de bloquear. |
| `app.security.failed-attempts-window-minutes` | `60` | Ventana de tiempo en la que se cuentan los intentos fallidos. |
| `app.security.block-duration-minutes` | `60` | Duración del bloqueo. |
| `app.email-verification.expiration-minutes` | `60` | Vigencia del enlace de verificación. |
| `app.email-verification.backend-url` | `http://localhost:8080/api/v1/auth/verify-email` | Base del enlace enviado por correo. |
| `app.password-reset.expiration-minutes` | `60` | Vigencia del código de recuperación. |
| `app.password-reset.code-length` | `6` | Longitud del código de recuperación. |
| `app.inactivity.owner-milliseconds` | `600000` (10 min) | Tiempo de inactividad sugerido al frontend para dueños. |
| `app.inactivity.admin-milliseconds` | `900000` (15 min) | Ídem para administradores. |
| `app.inactivity.warning-before-milliseconds` | `60000` (1 min) | Aviso previo al cierre por inactividad. |

### 1.4 API HTTP — `/api/v1/auth`

| Método | Ruta | Resultado esperado | Descripción |
|---|---|---|---|
| `POST` | `/auth/register` | `201 Created` | Registrar nuevo dueño de alojamiento. |
| `GET` | `/auth/verify-email?token=...` | `200 OK` | Verificar correo electrónico. |
| `POST` | `/auth/resend-verification?email=...` | `200 OK` | Reenviar enlace de verificación. |
| `POST` | `/auth/login` | `200 OK` | Iniciar sesión. |
| `POST` | `/auth/forgot-password` | `200 OK` | Solicitar código de recuperación. |
| `POST` | `/auth/reset-password` | `200 OK` | Restablecer contraseña con el código. |

> **Aclaración sobre la lista original:** `verify-email` está implementado como **`GET`** (no `POST`) porque es el enlace que el usuario pulsa desde su correo, y el token viaja como parámetro de URL (`?token=`). `resend-verification` es `POST` pero recibe el correo como **parámetro de URL** (`?email=`), no en el cuerpo JSON.

### 1.5 Detalle y pruebas por endpoint

#### 1.5.1 `POST /auth/register` — Registrar nuevo dueño de alojamiento

**Qué hace.** Crea la cuenta de un dueño. Guarda sus datos con la contraseña cifrada, le asigna el rol `ROLE_OWNER`, genera un token de verificación (UUID, válido 60 minutos), envía un correo con el enlace de verificación y devuelve un token JWT para que pueda empezar a usar la API. La cuenta queda con `email_verified = false`: **no podrá iniciar sesión (`/auth/login`) hasta verificar el correo**.

**Cuerpo (JSON):**

| Campo | Obligatorio | Reglas |
|---|:---:|---|
| `fullName` | Sí | 3 a 150 caracteres. |
| `accommodationName` | Sí | 3 a 200 caracteres. |
| `email` | Sí | Formato de correo válido; debe ser único en el sistema. |
| `password` | Sí | 8 a 100 caracteres. |
| `termsAccepted` | Sí | Debe ser `true`; si es `false` se rechaza. |

```json
{
  "fullName": "María Gómez",
  "accommodationName": "Cabañas del Río",
  "email": "dueno1@example.com",
  "password": "Clave12345",
  "termsAccepted": true
}
```

**Respuesta `201 Created`:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 2,
  "email": "dueno1@example.com",
  "fullName": "María Gómez",
  "roles": ["ROLE_OWNER"],
  "emailVerified": false,
  "verificationEmailSent": true,
  "inactivityTimeoutMs": 600000,
  "warningBeforeMs": 60000
}
```

**Prueba paso a paso:**

1. Abrir `POST /auth/register`, pegar el cuerpo de ejemplo (con un correo que **no** exista aún) y ejecutar.
2. Verificar que responde **201** y que `emailVerified` es `false` y `verificationEmailSent` es `true`.
3. Verificar que `roles` contiene `ROLE_OWNER` y que `inactivityTimeoutMs` es `600000`.
4. Copiar el valor de `token` (se usará en otras pruebas) y verificar en la base de datos que existe la fila en `owners` y una fila en `email_verification_tokens` con `used = false`.
5. Comprobar que la contraseña en la base de datos **no** está en texto plano (empieza con `$2a$` o `$2b$`).
6. Revisar la bandeja del correo registrado: debe llegar un mensaje con asunto *"HostFlow - Verifica tu correo electrónico"* y un enlace con `?token=`.

**Casos de error a probar:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| Correo repetido | Registrar dos veces el mismo `email` | `400` — *"Ya existe una cuenta registrada con el correo: …"* |
| Términos no aceptados | `"termsAccepted": false` | `400` — *"Debe aceptar los términos y la política de privacidad para registrarse"* |
| Términos ausentes | Omitir `termsAccepted` | `400` con `fieldErrors.termsAccepted` |
| Correo inválido | `"email": "sin-arroba"` | `400` con `fieldErrors.email` |
| Contraseña corta | `"password": "1234567"` | `400` con `fieldErrors.password` (mínimo 8) |
| Nombre muy corto | `"fullName": "Al"` | `400` con `fieldErrors.fullName` (mínimo 3) |
| Campos vacíos | Enviar `{}` | `400` con un error por cada campo obligatorio |

#### 1.5.2 `GET /auth/verify-email?token=...` — Verificar correo electrónico

**Qué hace.** Valida el token del enlace recibido por correo. Si es válido, marca al dueño con `email_verified = true`, marca el token como usado e invalida cualquier otro token pendiente del mismo correo. A partir de ese momento el usuario puede iniciar sesión.

**Parámetro:** `token` (en la URL, obligatorio).

**Respuesta `200 OK`:**

```json
{
  "message": "¡Correo electrónico verificado exitosamente! Ya puedes iniciar sesión con tus credenciales.",
  "success": "true"
}
```

**Reglas:** el token debe existir, no estar vencido (60 minutos) y no haber sido usado. Si el correo ya estaba verificado, responde 200 con el mensaje *"Tu correo electrónico ya fue verificado previamente…"* y marca el token como usado.

**Prueba paso a paso:**

1. Registrar un dueño nuevo (sección 1.5.1).
2. Obtener el token: abrir el enlace del correo, o consultarlo en la base con la consulta de la sección 0.4.
3. En el navegador (o en Postman con `GET`) abrir `http://localhost:8080/api/v1/auth/verify-email?token=<TOKEN>`.
4. Verificar que responde **200** con `success: "true"`.
5. Verificar en la base de datos: `owners.email_verified = true` y `email_verification_tokens.used = true`.
6. Repetir el mismo enlace: ahora debe responder **400** con el mensaje de que el enlace ya fue utilizado (o 200 con "ya fue verificado previamente", según el orden de las reglas; anotar lo observado).
7. Probar el inicio de sesión (sección 1.5.4): ahora debe funcionar.

**Casos de error a probar:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| Token inexistente | `?token=abc123` | `400` — *"El enlace de verificación es inválido o no existe."* |
| Token vencido | En la base, poner `expires_at` en el pasado y abrir el enlace | `400` — *"…ha expirado. Solicita un nuevo enlace…"* |
| Token ya usado | Abrir el enlace dos veces (con otro token que no haya verificado al usuario) | `400` — *"…ya fue utilizado…"* |
| Sin parámetro | Llamar sin `?token=` | ⚠️ Esperable `400`; por el manejador global puede aparecer `500`. Anotar. |

#### 1.5.3 `POST /auth/resend-verification?email=...` — Reenviar enlace de verificación

**Qué hace.** Genera un nuevo enlace de verificación y lo envía por correo. Invalida (marca como usados) los enlaces anteriores pendientes. Sirve cuando el usuario no recibió el correo o el enlace venció.

**Parámetro:** `email` (en la URL, **no** en el cuerpo). No lleva cuerpo JSON.

```http
POST /api/v1/auth/resend-verification?email=dueno1@example.com
```

**Respuesta `200 OK`:**

```json
{
  "message": "Si el correo está registrado, recibirás un nuevo enlace de verificación.",
  "success": "true"
}
```

**Reglas de seguridad:** si el correo no está registrado, responde exactamente lo mismo (200) para no revelar qué correos existen. Si el correo ya está verificado, el mensaje es *"Tu correo electrónico ya fue verificado. Ya puedes iniciar sesión."* y no se envía nada. El nuevo enlace vence en 60 minutos.

**Prueba paso a paso:**

1. Registrar un dueño nuevo y **no** verificar su correo.
2. Ejecutar `POST /auth/resend-verification?email=<correo del paso 1>`.
3. Verificar **200** con el mensaje *"Si el correo está registrado…"*.
4. En la base: el token anterior debe tener `used = true` y debe existir un token nuevo con `used = false`.
5. Verificar que llegó un correo con asunto *"HostFlow - Nuevo enlace de verificación"*.
6. Abrir el enlace **anterior**: debe fallar (token usado). Abrir el **nuevo**: debe verificar la cuenta.
7. Volver a ejecutar el reenvío con ese mismo correo (ya verificado): debe responder 200 con el mensaje de "ya fue verificado".

**Casos a probar:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| Correo no registrado | `?email=nadie@example.com` | `200` con el mensaje genérico (no se envía correo) |
| Correo ya verificado | Usar un dueño verificado | `200` — *"…ya fue verificado…"* |
| Sin parámetro `email` | Llamar sin query | ⚠️ Esperable `400`; puede aparecer `500`. Anotar. |

#### 1.5.4 `POST /auth/login` — Iniciar sesión

**Qué hace.** Autentica a un usuario con correo y contraseña y devuelve un token JWT. Aplica estas verificaciones **en este orden**:

1. ¿La cuenta fue deshabilitada por un administrador? → rechaza con la fecha de deshabilitación.
2. ¿La cuenta está bloqueada por intentos fallidos? → rechaza indicando los minutos restantes.
3. ¿El correo está verificado? → si no, rechaza e indica solicitar un nuevo enlace.
4. ¿La contraseña es correcta? → si no, cuenta un intento fallido y puede bloquear la cuenta.

Si el login es correcto y la cuenta tenía un bloqueo vencido, lo limpia. Cada intento queda guardado en `login_attempts`.

**Cuerpo (JSON):**

```json
{ "email": "dueno1@example.com", "password": "Clave12345" }
```

**Respuesta `200 OK`:** igual a la del registro, pero con `emailVerified: true` y `verificationEmailSent: false`. Este `token` es el que se usa en los demás módulos.

**Prueba paso a paso (flujo feliz):**

1. Tener un dueño con correo verificado.
2. Ejecutar `POST /auth/login` con su correo y contraseña.
3. Verificar **200**, `tokenType: "Bearer"` y `roles: ["ROLE_OWNER"]`.
4. Guardar el `token` en la variable `{{token}}` (o en **Authorize** de Swagger).
5. Probar el token: llamar a `GET /owners/accommodation/status` con ese token; debe responder 200.

**Prueba del bloqueo por intentos fallidos (paso a paso):**

1. Con un dueño verificado, ejecutar `POST /auth/login` con la contraseña **incorrecta**.
2. Intento 1: mensaje *"Credenciales incorrectas. Le quedan 4 intento(s) antes de que su cuenta sea bloqueada por 60 minutos."*
3. Repetir: intento 2 → quedan 3; intento 3 → quedan 2; intento 4 → queda 1.
4. Intento 5: *"Credenciales incorrectas. Su cuenta ha sido bloqueada por seguridad durante 60 minutos tras 5 intentos fallidos…"*
5. Ahora iniciar sesión con la contraseña **correcta**: debe rechazar con *"Cuenta bloqueada por seguridad. Intenta nuevamente en N minutos…"*
6. Verificar en la base: `owners.blocked_until` tiene una fecha futura y `login_attempts` tiene los 5 intentos con `success = false`.
7. Para continuar probando sin esperar una hora: `UPDATE owners SET blocked_until = NULL WHERE email = 'dueno1@example.com';`

**Casos de error a probar:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| Contraseña incorrecta | Login con clave errónea | `400` con mensaje de intentos restantes (ver ⚠️ abajo) |
| Correo sin verificar | Login con un dueño recién registrado | `400` — *"Tu correo electrónico no ha sido verificado…"* |
| Cuenta bloqueada | 5 fallos seguidos | `400` — *"Cuenta bloqueada por seguridad…"* |
| Cuenta deshabilitada | Un admin la desactiva (módulo 2) | `400` — *"Tu cuenta ha sido deshabilitada por el administrador. Fecha de deshabilitación: …"* |
| Campos vacíos / correo inválido | `{}` o `"email": "x"` | `400` con `fieldErrors` |

⚠️ **A anotar:** Swagger declara `401` para credenciales incorrectas, pero el servicio convierte ese error en una `BusinessException`, por lo que el código real es **400**. La documentación y el comportamiento no coinciden; conviene decidir cuál se mantiene.

#### 1.5.5 `POST /auth/forgot-password` — Solicitar recuperación de contraseña

**Qué hace.** Genera un código numérico de 6 dígitos (vigente 60 minutos, de un solo uso), lo guarda y lo envía por correo. Si el usuario ya tenía códigos pendientes, los elimina antes de crear el nuevo. Responde siempre lo mismo, exista o no el correo, para evitar que alguien descubra qué correos están registrados.

**Cuerpo (JSON):**

```json
{ "email": "dueno1@example.com" }
```

**Respuesta `200 OK`** (el correo se muestra ofuscado):

```json
{
  "message": "Si el correo está registrado, recibirás un código de recuperación.",
  "email": "d***@e***.com",
  "expirationMinutes": 60
}
```

**Prueba paso a paso:**

1. Ejecutar `POST /auth/forgot-password` con el correo de un dueño existente.
2. Verificar **200**, el mensaje genérico, `expirationMinutes: 60` y el correo ofuscado (`d***@e***.com`).
3. Verificar que llegó un correo con asunto *"HostFlow - Código de recuperación de contraseña"* con un código de 6 dígitos (o leerlo en la base, sección 0.4).
4. Ejecutar `forgot-password` **otra vez**: el código anterior debe desaparecer de la tabla y existir uno nuevo. Probar el código viejo en `reset-password` más adelante: debe fallar.
5. Ejecutar con un correo **no registrado**: debe responder el mismo **200** y no enviar correo (así se verifica que no se filtra información).

**Casos de error a probar:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| Correo inválido | `{"email": "abc"}` | `400` con `fieldErrors.email` |
| Correo vacío | `{}` | `400` con `fieldErrors.email` |
| Dominio sin punto | `{"email": "usuario@localhost"}` | ⚠️ El método que ofusca el correo asume un dominio con punto; puede responder `500`. Anotar. |

#### 1.5.6 `POST /auth/reset-password` — Restablecer contraseña con código de recuperación

**Qué hace.** Valida el par correo + código de 6 dígitos y, si es correcto, cambia la contraseña (cifrada con BCrypt), marca el código como usado y envía un correo de confirmación del cambio.

**Cuerpo (JSON):**

| Campo | Obligatorio | Reglas |
|---|:---:|---|
| `email` | Sí | Correo del usuario. |
| `code` | Sí | Exactamente 6 caracteres. |
| `newPassword` | Sí | 8 a 100 caracteres. |

```json
{ "email": "dueno1@example.com", "code": "483920", "newPassword": "NuevaClave456" }
```

**Respuesta `200 OK`:**

```json
{
  "message": "Tu contraseña ha sido actualizada exitosamente. Ya puedes iniciar sesión.",
  "email": "d***@e***.com",
  "expirationMinutes": 60
}
```

**Prueba paso a paso:**

1. Ejecutar `forgot-password` (sección 1.5.5) y obtener el código del correo o de la base.
2. Ejecutar `POST /auth/reset-password` con el correo, el código y una nueva contraseña.
3. Verificar **200** y el mensaje de contraseña actualizada.
4. Verificar que llegó el correo *"HostFlow - Contraseña actualizada exitosamente"*.
5. Iniciar sesión con la contraseña **anterior**: debe fallar. Iniciar sesión con la **nueva**: debe funcionar.
6. Repetir `reset-password` con el mismo código: debe rechazar por código ya utilizado.

**Casos de error a probar:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| Código incorrecto | Cambiar un dígito | `400` — *"El código de recuperación es inválido o no corresponde a este correo."* |
| Código de otro correo | Usar el código de un usuario con el correo de otro | `400` (mismo mensaje) |
| Código vencido | En la base, `expires_at` en el pasado | `400` — *"…ha expirado. Solicita un nuevo código."* |
| Código ya usado | Reutilizar un código | `400` — *"…ya fue utilizado…"* |
| Código de longitud incorrecta | `"code": "123"` | `400` con `fieldErrors.code` |
| Contraseña corta | `"newPassword": "123"` | `400` con `fieldErrors.newPassword` |

### 1.6 Seguridad del módulo — qué verificar

- Los endpoints de autenticación funcionan **sin** token (probar sin `Authorization`).
- El token devuelto sirve para llamar a endpoints protegidos y deja de servir pasadas 24 horas.
- Las contraseñas nunca aparecen en ninguna respuesta.
- `forgot-password` y `resend-verification` no revelan si un correo existe.

### 1.7 Observaciones del código (para revisar)

- **El registro devuelve un JWT aunque el correo no esté verificado.** El filtro JWT no consulta `email_verified`, por lo que un usuario sin verificar podría usar ese token en endpoints protegidos. Si no es lo deseado, habría que validarlo en el filtro o no devolver token al registrarse. Se puede comprobar llamando a un endpoint protegido con el token del registro.
- **Restablecer la contraseña no quita el bloqueo.** El mensaje de bloqueo invita a usar *"Recuperar contraseña"*, pero `reset-password` solo cambia la clave; `blocked_until` se mantiene hasta que venza.
- **Códigos de 6 dígitos sin límite de intentos en `reset-password`** y generados con `java.util.Random` (no `SecureRandom`). Para producción conviene limitar intentos y usar un generador seguro.
- **El administrador semilla se crea con `email_verified = false`**, por lo que no puede iniciar sesión hasta verificarlo manualmente (ver sección 0.5).
- `app.email-verification.frontend-url` existe en `application-dev.properties`, pero el código solo usa `backend-url` para armar el enlace.

---

## 2. Módulo: Dueños de Alojamiento

Swagger: sección **Dueños de Alojamiento** (todos los endpoints exigen token).

### 2.1 Alcance

Consulta de los dueños registrados y activación o desactivación de sus cuentas:

- Listar todos los dueños (solo administrador).
- Obtener un dueño por ID o por correo (dueño o administrador).
- Activar o desactivar la cuenta de un dueño (solo administrador).

### 2.2 Modelo de datos

Usa la tabla `owners` descrita en la sección 1.2. La respuesta de la API (`OwnerResponse`) **no** incluye la contraseña ni los tokens:

| Campo | Descripción |
|---|---|
| `id` | Identificador del dueño. |
| `fullName` | Nombre y apellido. |
| `accommodationName` | Nombre del alojamiento indicado al registrarse. |
| `email` | Correo. |
| `enabled` | `true` si la cuenta está activa. |
| `roles` | Lista de roles, por ejemplo `["ROLE_OWNER"]`. |
| `createdAt` / `updatedAt` | Fechas de auditoría. |

Los campos con valor `null` se omiten del JSON (`default-property-inclusion=non_null`).

### 2.3 API HTTP — `/api/v1/owners`

| Método | Ruta | Rol requerido | Resultado esperado | Descripción |
|---|---|---|---|---|
| `GET` | `/owners` | `ADMIN` | `200 OK` | Listar todos los dueños. |
| `GET` | `/owners/{id}` | `OWNER` o `ADMIN` | `200 OK` | Obtener dueño por ID. |
| `GET` | `/owners/email/{email}` | `OWNER` o `ADMIN` | `200 OK` | Obtener dueño por correo. |
| `PATCH` | `/owners/{id}/status?enabled=true\|false` | `ADMIN` | `200 OK` | Activar o desactivar la cuenta. |

### 2.4 Preparación de las pruebas

1. Tener **dos dueños verificados**: por ejemplo `dueno1@example.com` y `dueno2@example.com` (registro + verificación del módulo 1).
2. Tener el **administrador** habilitado para iniciar sesión (sección 0.5).
3. Iniciar sesión con cada uno y guardar tres tokens: `tokenAdmin`, `tokenDueno1`, `tokenDueno2`.

### 2.5 Detalle y pruebas por endpoint

#### 2.5.1 `GET /owners` — Listar todos los dueños

**Qué hace.** Devuelve la lista completa de usuarios registrados (incluido el administrador, que también es una fila de `owners`). Restringido al rol `ADMIN`.

**Prueba paso a paso:**

1. Ejecutar `GET /owners` con `tokenAdmin`.
2. Verificar **200** y que la respuesta es un arreglo con todos los usuarios (al menos el administrador y los dueños creados).
3. Verificar que cada elemento trae `id`, `fullName`, `accommodationName`, `email`, `enabled`, `roles`, `createdAt`, `updatedAt` y **no** trae `password`.
4. Comprobar que el administrador aparece con `roles: ["ROLE_ADMIN", "ROLE_OWNER"]`.
5. Repetir con `tokenDueno1`: **no** debe permitir el acceso.

**Casos a probar:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| Sin token | Llamar sin `Authorization` | `403` (no autenticado) |
| Con rol `OWNER` | Usar `tokenDueno1` | ⚠️ Esperado `403`. Como el manejador global captura toda `Exception`, un acceso denegado por `@PreAuthorize` puede aparecer como `500`. Anotar lo observado. |
| Token vencido o alterado | Modificar un carácter del token | ⚠️ Puede responder `500` (el error ocurre en el filtro JWT). Anotar. |

#### 2.5.2 `GET /owners/{id}` — Obtener dueño por ID

**Qué hace.** Devuelve los datos de un dueño según su identificador numérico. Permitido a `OWNER` y `ADMIN`.

**Prueba paso a paso:**

1. Obtener el `id` de un dueño (por ejemplo desde la respuesta del registro, campo `userId`).
2. Ejecutar `GET /owners/2` con `tokenDueno1`.
3. Verificar **200** y que los datos coinciden con el dueño consultado.
4. Ejecutar con un ID inexistente, por ejemplo `GET /owners/99999`: debe responder **404** — *"No se encontró el dueño con el ID: 99999"*.
5. Ejecutar con un ID no numérico (`/owners/abc`): ⚠️ esperado `400`; puede aparecer `500`.

**⚠️ Verificación de seguridad importante:** el código **no comprueba que el dueño consulte solo su propia información**. Con `tokenDueno1`, ejecutar `GET /owners/<id del dueno2>`: si responde **200** con los datos del otro dueño, un dueño puede ver el nombre y correo de otros. Anotar el resultado y decidir si se restringe a "solo el propio usuario o un administrador".

#### 2.5.3 `GET /owners/email/{email}` — Obtener dueño por correo

**Qué hace.** Igual que el anterior, pero buscando por correo electrónico. Permitido a `OWNER` y `ADMIN`.

**Prueba paso a paso:**

1. Ejecutar `GET /owners/email/dueno1@example.com` con `tokenDueno1`.
2. Verificar **200** y que `email` coincide.
3. Ejecutar con un correo inexistente: debe responder **404** — *"No se encontró el dueño con el correo: …"*.
4. Repetir la verificación de seguridad del punto anterior: consultar el correo de **otro** dueño con `tokenDueno1`.

> Si el cliente tiene problemas con el símbolo `@` en la URL, codificarlo como `%40` (`dueno1%40example.com`).

#### 2.5.4 `PATCH /owners/{id}/status?enabled=...` — Activar o desactivar cuenta de dueño

**Qué hace.** Cambia el estado `enabled` de la cuenta. No lleva cuerpo JSON: el valor viaja como parámetro de URL. Al desactivar (`enabled=false`) guarda la fecha en `disabled_at`; al reactivar (`enabled=true`) la borra. Una cuenta desactivada **no puede iniciar sesión**. Restringido a `ADMIN`.

```http
PATCH /api/v1/owners/2/status?enabled=false
Authorization: Bearer <tokenAdmin>
```

**Prueba paso a paso:**

1. Con `tokenAdmin`, ejecutar `PATCH /owners/<id dueno1>/status?enabled=false`.
2. Verificar **200** y que la respuesta trae `enabled: false`.
3. Verificar en la base que `owners.disabled_at` tiene la fecha actual.
4. Intentar iniciar sesión como `dueno1`: debe rechazar con *"Tu cuenta ha sido deshabilitada por el administrador. Fecha de deshabilitación: dd/MM/yyyy 'a las' HH:mm…"* (código 400).
5. Reactivar con `PATCH /owners/<id>/status?enabled=true`: verificar `enabled: true` y `disabled_at` vacío.
6. Iniciar sesión nuevamente: debe funcionar.

**Casos a probar:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| ID inexistente | `/owners/99999/status?enabled=false` | `404` — *"No se encontró el dueño con el ID: 99999"* |
| Con rol `OWNER` | Usar `tokenDueno1` | ⚠️ Esperado `403` (puede aparecer `500`). Anotar. |
| Falta `enabled` | Omitir el parámetro | ⚠️ Esperado `400`; puede aparecer `500`. Anotar. |
| Valor no booleano | `?enabled=abc` | ⚠️ Esperado `400`; puede aparecer `500`. Anotar. |

**⚠️ Verificación importante:** desactivar una cuenta solo bloquea **nuevos** inicios de sesión. El filtro JWT valida únicamente que el token coincida con el usuario y no esté vencido, **no consulta `enabled`**. Pruebe: con `dueno1` ya logueado (token guardado), el administrador lo desactiva; luego llame con ese token anterior a `GET /owners/<id>`. Si sigue respondiendo 200, el dueño desactivado conserva acceso hasta que su token venza (24 horas).

### 2.6 Observaciones del código (para revisar)

- Falta control de propiedad en `GET /owners/{id}` y `GET /owners/email/{email}` (un dueño podría consultar a otros).
- Un dueño desactivado mantiene acceso con un token ya emitido.
- Nada impide que un administrador se desactive a sí mismo.

---

## 3. Módulo: Gestión de Alojamiento (desactivación temporal)

Swagger: sección **Gestión de Alojamiento**. Controlador: `AccommodationController`.

### 3.1 Alcance

Permite al dueño autenticado:

- Desactivar temporalmente su alojamiento (remodelación, mantenimiento, vacaciones, cierre).
- Reactivarlo.
- Consultar si tiene una desactivación vigente.
- Ver el historial de desactivaciones.
- Consultar si el alojamiento estará disponible en una fecha concreta.

Todos los endpoints exigen rol `OWNER`. Se identifica al dueño por el token (no se envía ningún identificador). **No exigen** que el perfil del alojamiento esté creado: trabajan con el identificador del dueño.

> Complementa al documento *Módulo: Gestión del alojamiento*: allí se indica que editar el perfil no activa ni desactiva el alojamiento; la desactivación se gestiona solo con estos endpoints.

### 3.2 Modelo de datos

#### `accommodation_temporary_disables`

| Columna | Tipo SQL | Nulo | Descripción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. |
| `owner_id` | `BIGINT` | No | Dueño al que pertenece la desactivación. |
| `reason` | `VARCHAR(500)` | No | Motivo. |
| `disabled_from` | `TIMESTAMP` | No | Inicio de la desactivación. |
| `disabled_until` | `TIMESTAMP` | Sí | Fin previsto. Si es `NULL`, la desactivación es indefinida. |
| `is_active` | `BOOLEAN` | No | `true` mientras la desactivación esté vigente; `false` al reactivar. |
| `created_at` | `TIMESTAMP` | No | Fecha de creación. |
| `updated_at` | `TIMESTAMP` | Sí | Última modificación. |

Índices: `idx_owner_active (owner_id, is_active)`, `idx_disable_dates (disabled_from, disabled_until)` e `idx_owner_created (owner_id, created_at)`.

### 3.3 API HTTP — `/api/v1/owners/accommodation`

| Método | Ruta | Resultado esperado | Descripción |
|---|---|---|---|
| `POST` | `/disable` | `201 Created` | Desactivar temporalmente mi alojamiento. |
| `POST` | `/enable` | `200 OK` | Reactivar mi alojamiento. |
| `GET` | `/status` | `200 OK` | Consultar estado (desactivación vigente). |
| `GET` | `/history` | `200 OK` | Historial de desactivaciones. |
| `GET` | `/availability?date=...` | `200 OK` | Verificar disponibilidad en una fecha. |

### 3.4 Preparación de las pruebas

1. Iniciar sesión con un dueño verificado y guardar su `token`.
2. Asegurarse de que **no tiene** una desactivación vigente (verificar con `GET /status`).
3. Usar fechas **futuras** respecto al día de la prueba. Ejemplos de esta guía (hoy es 2026-10-28): `2026-11-10T08:00:00` y `2026-11-20T18:00:00`.

### 3.5 Detalle y pruebas por endpoint

#### 3.5.1 `POST /disable` — Desactivar temporalmente mi alojamiento

**Qué hace.** Registra una desactivación para el dueño autenticado. Mientras esté vigente, el alojamiento figura como no disponible en las fechas indicadas.

**Cuerpo (JSON):**

| Campo | Obligatorio | Reglas |
|---|:---:|---|
| `reason` | Sí | No vacío (máximo 500 caracteres en base de datos). |
| `disabledFrom` | Sí | Fecha y hora de inicio; **no puede estar en el pasado**. |
| `disabledUntil` | No | Si se envía, debe ser **posterior** a `disabledFrom`. Si se omite, es indefinida. |

```json
{
  "reason": "Remodelación de la cocina",
  "disabledFrom": "2026-11-10T08:00:00",
  "disabledUntil": "2026-11-20T18:00:00"
}
```

**Respuesta `201 Created`:**

```json
{
  "id": 1,
  "ownerId": 2,
  "reason": "Remodelación de la cocina",
  "disabledFrom": "2026-11-10T08:00:00",
  "disabledUntil": "2026-11-20T18:00:00",
  "isActive": true,
  "createdAt": "2026-10-28T10:30:00"
}
```

**Prueba paso a paso:**

1. Ejecutar `POST /owners/accommodation/disable` con el cuerpo de ejemplo.
2. Verificar **201**, `isActive: true` y `ownerId` igual al del usuario del token.
3. Verificar la fila en `accommodation_temporary_disables`.
4. Ejecutar `GET /status`: debe devolver ese mismo registro.

**Casos de error a probar:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| Ya existe una desactivación vigente | Repetir el `POST /disable` sin reactivar | `400` — *"Tu alojamiento ya tiene una desactivación temporal activa. Debes reactivarlo antes de crear una nueva desactivación."* |
| Fecha de inicio en el pasado | `"disabledFrom": "2026-01-01T08:00:00"` | `400` — *"La fecha de inicio de la desactivación no puede ser en el pasado."* |
| Fin anterior al inicio | `disabledUntil` menor que `disabledFrom` | `400` — *"La fecha de reactivación debe ser posterior a la fecha de desactivación."* |
| Motivo vacío | `"reason": ""` | `400` con `fieldErrors.reason` |
| Falta fecha de inicio | Omitir `disabledFrom` | `400` con `fieldErrors.disabledFrom` |
| Fecha con formato incorrecto | `"disabledFrom": "10/11/2026"` | ⚠️ Esperado `400`; puede aparecer `500`. Anotar. |
| Motivo de más de 500 caracteres | Texto largo | ⚠️ No hay validación de tamaño en el DTO; puede fallar en base de datos con `500`. Anotar. |
| Sin token / rol incorrecto | Llamar sin token | `403` |

#### 3.5.2 `GET /status` — Consultar estado de mi alojamiento

**Qué hace.** Devuelve la desactivación **vigente** (`is_active = true`) del dueño. Si no hay ninguna, responde `200` con cuerpo vacío.

**Prueba paso a paso:**

1. Sin desactivación vigente: `GET /owners/accommodation/status` → **200** con cuerpo vacío.
2. Crear una desactivación (sección 3.5.1) y repetir: **200** con los datos de la desactivación.
3. Reactivar (sección 3.5.3) y repetir: de nuevo cuerpo vacío.

> `status` devuelve el registro mientras `is_active` sea `true`, aunque la fecha de inicio esté en el futuro o la de fin ya haya pasado. Ver observaciones en 3.6.

#### 3.5.3 `POST /enable` — Reactivar mi alojamiento

**Qué hace.** Marca como inactiva (`is_active = false`) la desactivación vigente. No lleva cuerpo. Sirve tanto para terminar una desactivación en curso como para **cancelar** una programada a futuro.

**Prueba paso a paso:**

1. Con una desactivación vigente, ejecutar `POST /owners/accommodation/enable`.
2. Verificar **200** y que la respuesta trae `isActive: false`.
3. Ejecutar `GET /status`: debe volver vacío.
4. Ejecutar `GET /history`: el registro debe seguir apareciendo, con `isActive: false`.

**Caso de error:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| No hay desactivación vigente | Ejecutar `/enable` dos veces seguidas | `400` — *"No hay una desactivación temporal activa para tu alojamiento."* |

#### 3.5.4 `GET /history` — Historial de desactivaciones

**Qué hace.** Lista **todas** las desactivaciones del dueño (vigentes y terminadas), de la más reciente a la más antigua.

**Prueba paso a paso:**

1. Crear una desactivación, reactivarla, crear otra (con otras fechas).
2. Ejecutar `GET /owners/accommodation/history`.
3. Verificar **200**, que aparecen las dos, que la más reciente está primero y que la primera tiene `isActive: false` y la segunda `isActive: true`.
4. Con un dueño sin desactivaciones, debe devolver `[]`.
5. Con **otro** dueño, comprobar que no aparece el historial del primero (aislamiento por dueño).

#### 3.5.5 `GET /availability?date=...` — Verificar disponibilidad en fecha específica

**Qué hace.** Indica si el alojamiento está disponible en una fecha y hora. Devuelve `true` (disponible) o `false` (hay una desactivación vigente que cubre esa fecha: `disabledFrom <= date` y (`disabledUntil` vacío o `disabledUntil >= date`)).

```http
GET /api/v1/owners/accommodation/availability?date=2026-11-15T12:00:00
```

**Prueba paso a paso** (con la desactivación del 10/11 al 20/11 de 2026 vigente):

| Fecha consultada | Resultado esperado | Motivo |
|---|---|---|
| `2026-11-05T12:00:00` | `true` | Antes del inicio. |
| `2026-11-10T08:00:00` | `false` | Justo en el inicio (límite incluido). |
| `2026-11-15T12:00:00` | `false` | Dentro del período. |
| `2026-11-20T18:00:00` | `false` | Justo en el fin (límite incluido). |
| `2026-11-25T12:00:00` | `true` | Después del fin. |

Luego reactivar el alojamiento y repetir con `2026-11-15T12:00:00`: debe dar `true`. Probar también una desactivación **sin** `disabledUntil`: toda fecha posterior al inicio debe dar `false`.

**Caso de error:** sin el parámetro `date` o con formato inválido (`date=15-11-2026`) ⚠️ esperado `400`; puede aparecer `500`. Anotar.

### 3.6 Observaciones del código (para revisar)

- **Las desactivaciones no vencen automáticamente.** Existe el método `isExpired()` en la entidad, pero no hay una tarea programada que ponga `is_active = false` al pasar `disabledUntil`. Prueba sugerida: crear una desactivación con fin cercano, esperar a que pase y comprobar si `POST /disable` sigue rechazando con "ya tiene una desactivación activa". Si es así, el dueño debe llamar a `/enable` manualmente.
- `GET /status` devuelve cuerpo vacío (no un objeto con un campo de estado) cuando no hay desactivación; puede ser incómodo para el frontend.
- Otros módulos no consultan esta tabla todavía, por lo que la desactivación hoy solo se refleja en `/availability`.

---

## 4. Módulo: Monedas del alojamiento

Swagger: sección **Monedas del alojamiento**. Controlador: `AccommodationCurrencyController`.

### 4.1 Alcance

Permite al dueño decidir en qué monedas puede mostrar y cobrar sus precios y cómo se calcula la tasa de conversión:

- Monedas soportadas: **ARS** (peso argentino, moneda base), **USD** y **EUR**.
- Cada moneda puede usar una **tasa automática** (tomada de APIs externas y guardada en caché) o una **tasa manual** definida por el dueño.
- Para USD se elige el tipo de cotización (oficial, blue, bolsa/MEP, etc.).

Requiere rol `OWNER` y que el **perfil del alojamiento ya esté creado**.

### 4.2 Modelo de datos

#### `accepted_currencies`

| Columna | Tipo SQL | Nulo | Descripción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. |
| `accommodation_id` | `BIGINT` | No | FK `fk_accepted_currencies_accommodation` a `accommodations(id)`. |
| `currency` | `VARCHAR(10)` | No | `ARS`, `USD` o `EUR`. |
| `is_enabled` | `BOOLEAN` | No | Moneda habilitada para mostrar/cobrar. |
| `use_automatic_rate` | `BOOLEAN` | No | `true` = tasa de API externa; `false` = tasa manual. |
| `manual_rate` | `NUMERIC(20,10)` | Sí | Tasa manual (solo si no es automática). |
| `usd_rate_type` | `VARCHAR(20)` | Sí | Tipo de dólar (solo USD con tasa automática). |
| `created_at` / `updated_at` | `TIMESTAMP` | No | Auditoría. |

Restricción única `uk_accepted_currencies_accommodation_currency (accommodation_id, currency)`: una fila por moneda y alojamiento. Índice `idx_accepted_currencies_accommodation`.

#### `exchange_rates` (caché de tasas automáticas)

| Columna | Descripción |
|---|---|
| `from_currency` / `to_currency` | Par de monedas (siempre `ARS` → `USD` o `EUR`). |
| `usd_rate_type` | Tipo de dólar (solo para USD; `NULL` para EUR). |
| `rate` | `NUMERIC(20,10)`. Unidades de la moneda destino por **1 ARS**. |
| `fetched_at` | Momento en que se obtuvo la tasa. |

#### `accommodations.base_currency`

La tabla `accommodations` incluye la columna `base_currency` (`VARCHAR(10)`, no nula, por defecto `ARS`). Es la moneda en la que están expresados el precio base del alojamiento y el de los servicios adicionales. **No se puede cambiar desde la API del perfil** (el DTO del perfil no la incluye): en la práctica es siempre `ARS`.

### 4.3 Cómo se obtienen y usan las tasas

- **Qué significa la tasa:** cuántas unidades de la moneda destino equivalen a **1 ARS**. Ejemplo: `0.00085` significa 1 ARS = 0.00085 USD. El precio convertido es `monto × tasa`, redondeado a 2 decimales (`HALF_UP`).
- **USD automático:** se consulta `https://dolarapi.com/v1/dolares` (valor de venta). El sistema guarda `1 / venta`. Tipos soportados (`UsdRateType`): `OFICIAL`, `BLUE`, `BOLSA`, `CONTADOCONLIQUI`, `MAYORISTA`, `CRIPTO`, `TARJETA`. Si no se indica tipo, se usa `BLUE`.
- **EUR automático:** se consulta `https://open.er-api.com/v6/latest/ARS`.
- **Actualización:** una tarea programada corre **cada hora** (la primera 30 segundos después de arrancar). Una tasa con más de 4 horas se sigue usando, pero se registra una advertencia en el log.
- **Moneda base (ARS):** siempre queda habilitada, con tasa manual `1`, sin importar lo que se envíe.

### 4.4 API HTTP — `/api/v1/owners/accommodation/currencies`

| Método | Ruta | Resultado esperado | Descripción |
|---|---|---|---|
| `GET` | `/owners/accommodation/currencies` | `200 OK` | Listar monedas configuradas con sus tasas efectivas. |
| `PUT` | `/owners/accommodation/currencies` | `200 OK` | Crear o actualizar la configuración de una moneda. |

### 4.5 Preparación de las pruebas

1. Dueño verificado con `token`.
2. **Perfil del alojamiento creado** (`POST /owners/accommodation/profile`, con `basePrice`, por ejemplo `85000.00`).
3. Esperar al menos 30 segundos desde el arranque del backend (para que se carguen las tasas automáticas) y tener acceso a internet. Revisar en el log mensajes como *"USD (Dólar Blue) actualizado"*.

### 4.6 Detalle y pruebas por endpoint

#### 4.6.1 `GET /owners/accommodation/currencies` — Listar monedas configuradas

**Qué hace.** Devuelve las monedas que el dueño ya configuró, ordenadas por código, junto con la **tasa efectiva** actual de cada una (la que realmente se aplicaría hoy). Solo aparecen las monedas que ya tienen una fila: un alojamiento nuevo devuelve `[]` hasta que se configure alguna con `PUT`.

**Respuesta `200 OK` (ejemplo):**

```json
[
  {
    "id": 1, "currency": "ARS", "symbol": "AR$", "displayName": "Peso Argentino",
    "enabled": true, "useAutomaticRate": false, "manualRate": 1,
    "effectiveRate": 1, "updatedAt": "2026-10-28T11:00:00"
  },
  {
    "id": 2, "currency": "USD", "symbol": "US$", "displayName": "Dólar Estadounidense",
    "enabled": true, "useAutomaticRate": true, "usdRateType": "BLUE",
    "effectiveRate": 0.0008547009, "updatedAt": "2026-10-28T11:01:00"
  }
]
```

**Prueba paso a paso:**

1. Con el perfil recién creado, ejecutar `GET /owners/accommodation/currencies`: debe responder **200** con `[]`.
2. Configurar monedas con `PUT` (sección 4.6.2).
3. Volver a ejecutar el `GET` y verificar que aparecen ordenadas (ARS, EUR, USD) con `symbol`, `displayName`, `enabled`, `useAutomaticRate` y `effectiveRate`.
4. En monedas con tasa automática, comparar `effectiveRate` con la fila correspondiente de `exchange_rates`.
5. En monedas con tasa manual, verificar que `effectiveRate` es igual a `manualRate`.
6. Probar con un dueño **sin perfil de alojamiento**: debe responder **404** — *"Registra primero los datos generales del alojamiento antes de configurar monedas."*

⚠️ **A verificar (posible error de código):** el controlador calcula `effectiveRate` llamando a `resolveEffectiveRate(ownerId, moneda)`, pero ese método espera el **ID del alojamiento**, no el del dueño. Solo funcionará bien si ambos IDs coinciden. Pruebe con un dueño cuyo `owners.id` sea distinto de su `accommodations.id` (por ejemplo, el administrador ocupa el ID 1 de `owners`): si `effectiveRate` aparece vacío (`null`) o distinto de la tasa real aunque la moneda esté bien configurada, es este defecto. Los precios públicos (módulo 5) usan el ID correcto, así que sirven de comparación.

#### 4.6.2 `PUT /owners/accommodation/currencies` — Crear o actualizar la configuración de una moneda

**Qué hace.** Habilita o deshabilita una moneda y define cómo se obtiene su tasa. Si ya existe la fila de esa moneda, la actualiza; si no, la crea (por eso es `PUT`).

**Cuerpo (JSON):**

| Campo | Obligatorio | Descripción |
|---|:---:|---|
| `currency` | Sí | `ARS`, `USD` o `EUR`. |
| `enabled` | Sí | `true`/`false`. |
| `useAutomaticRate` | Sí | `true` = tasa automática; `false` = manual. |
| `manualRate` | Solo si `useAutomaticRate = false` | Mayor a 0. Unidades de la moneda por 1 ARS. |
| `usdRateType` | No (solo USD automático) | `OFICIAL`, `BLUE`, `BOLSA`, `CONTADOCONLIQUI`, `MAYORISTA`, `CRIPTO`, `TARJETA`. Por defecto `BLUE`. |

**Reglas que aplica el servicio:**

- Para `ARS`: se fuerza `enabled = true`, tasa manual `1` y sin tasa automática, sea lo que sea que se envíe.
- Si `useAutomaticRate = false` y la moneda no es ARS: `manualRate` debe ser mayor a 0.
- Con tasa automática se **borra** la tasa manual; con tasa manual se **borra** el tipo de dólar.
- `usdRateType` solo se conserva para USD con tasa automática.

**Ejemplos de cuerpo:**

```json
{ "currency": "USD", "enabled": true, "useAutomaticRate": true, "usdRateType": "BLUE" }
```

```json
{ "currency": "EUR", "enabled": true, "useAutomaticRate": false, "manualRate": 0.00078 }
```

```json
{ "currency": "ARS", "enabled": true, "useAutomaticRate": false }
```

> ⚠️ El texto de ayuda del DTO en Swagger menciona valores como `MEP` y `CRYPTO`, pero el enumerado real usa `BOLSA` y `CRIPTO`. Usar los nombres del enumerado; los otros probablemente fallen al leer el JSON.

**Prueba paso a paso:**

1. **Moneda base:** enviar el ejemplo de `ARS`. Verificar **200**, `enabled: true`, `manualRate: 1` y `effectiveRate: 1`.
2. **USD automático:** enviar el ejemplo de USD. Verificar **200**, `usdRateType: "BLUE"`, `manualRate` ausente y `effectiveRate` con un valor pequeño (por ejemplo `0.0008…`).
3. **Cambiar tipo de dólar:** reenviar con `"usdRateType": "OFICIAL"`. Verificar que ahora devuelve `OFICIAL`, que **no** se creó una fila nueva (misma `id`) y que `effectiveRate` cambió.
4. **USD sin tipo:** enviar `useAutomaticRate: true` sin `usdRateType`: debe asignar `BLUE`.
5. **EUR manual:** enviar el ejemplo manual. Verificar `useAutomaticRate: false`, `manualRate: 0.00078`, `usdRateType` ausente y `effectiveRate: 0.00078`.
6. **Deshabilitar:** enviar `{"currency":"EUR","enabled":false,"useAutomaticRate":false,"manualRate":0.00078}`. Verificar `enabled: false`.
7. Verificar en la base: una sola fila por moneda en `accepted_currencies` (restricción única).

**Casos de error a probar:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| Manual sin tasa | `useAutomaticRate:false` sin `manualRate` (en USD o EUR) | `400` — *"Debe indicar una tasa manual válida mayor a 0."* |
| Tasa manual cero o negativa | `"manualRate": 0` o `-1` | `400` (validación del campo o del servicio) |
| Falta `currency` | Omitirlo | `400` con `fieldErrors.currency` |
| Falta `enabled` o `useAutomaticRate` | Omitirlos | `400` con el campo faltante |
| Moneda no soportada | `"currency": "BRL"` | ⚠️ Esperado `400`; puede aparecer `500`. Anotar. |
| Tipo de dólar inexistente | `"usdRateType": "MEP"` | ⚠️ Esperado `400`; puede aparecer `500`. Anotar. |
| Sin perfil de alojamiento | Usar un dueño sin perfil | `404` — *"Registra primero los datos generales del alojamiento antes de configurar monedas."* |
| Sin token / rol administrador sin alojamiento | Llamar sin token | `403` |

**Comprobación de efecto cruzado:** después de configurar monedas, consultar los precios públicos (módulo 5) y los servicios adicionales (módulo 6): deben aparecer los montos convertidos con las tasas configuradas.

### 4.7 Observaciones del código (para revisar)

- Posible uso del ID equivocado en `effectiveRate` (ver 4.6.1).
- Un alojamiento nuevo **no** tiene ARS configurada por defecto: la moneda base solo aparece como fila tras un `PUT` de ARS (aunque los precios públicos siempre incluyen la moneda base).
- Si las APIs externas están caídas al arrancar, las monedas automáticas quedan sin tasa: `effectiveRate` será `null` y la moneda se omite de los precios públicos. Conviene probar también con tasa manual.
- La ayuda de Swagger del request indica `BLUE, OFICIAL, MEP, CRYPTO…`, que no coincide con el enumerado real.

---

## 5. Módulo: Precios públicos

Swagger: sección **Precios públicos**. Controlador: `PublicPricingController`.

> **Aclaración sobre la lista original:** en la solicitud, este módulo aparecía con los mismos endpoints de "Monedas del alojamiento" (`/owners/accommodation/currencies`). Esos pertenecen al módulo 4. Los endpoints reales del módulo **Precios públicos** son los dos de abajo, bajo la ruta `/public/accommodations/{accommodationId}`.

### 5.1 Alcance

Endpoints **sin autenticación** pensados para el sitio público o el motor de reservas: muestran a cualquier visitante el precio base del alojamiento y sus servicios adicionales activos convertidos a todas las monedas habilitadas por el dueño. La ruta `/public/**` está permitida sin token en `SecurityConfig`.

### 5.2 Datos que utiliza

No tiene tablas propias. Lee:

- `accommodations` (`base_price`, `base_currency`).
- `additional_services` (solo los activos) y `additional_service_images`.
- `accepted_currencies` (monedas habilitadas y tipo de tasa) y `exchange_rates` (tasas en caché).

### 5.3 API HTTP — `/api/v1/public/accommodations/{accommodationId}`

| Método | Ruta | Resultado esperado | Descripción |
|---|---|---|---|
| `GET` | `/public/accommodations/{accommodationId}/base-price` | `200 OK` | Precio base en todas las monedas habilitadas. |
| `GET` | `/public/accommodations/{accommodationId}/additional-services` | `200 OK` | Servicios adicionales **activos** con precios convertidos e imágenes. |

`accommodationId` es el ID del **alojamiento** (tabla `accommodations`), no el del dueño. Se obtiene de la respuesta de `POST /owners/accommodation/profile` o de la base de datos.

### 5.4 Cómo se arma la respuesta

1. La primera entrada es siempre la **moneda base** (ARS) con el precio tal cual está guardado.
2. Luego se agregan USD y EUR **solo si** están configuradas, habilitadas y con una tasa resoluble.
3. Si una moneda está deshabilitada, no configurada o sin tasa disponible (API caída, sin tasa manual válida), se **omite** silenciosamente para no mostrar precios incorrectos.

Cada precio incluye `currency`, `symbol`, `displayName`, `amount` y `formatted` (por ejemplo `"US$ 72.56"`).

### 5.5 Preparación de las pruebas

1. Perfil de alojamiento creado con `basePrice` (por ejemplo `85000.00`). Anotar el `accommodationId` (supongamos `1`).
2. Monedas configuradas (módulo 4): ARS, USD automático (`BLUE`) y EUR manual (`0.00078`).
3. Al menos **dos** servicios adicionales creados (módulo 6): uno activo y uno con `active: false`.
4. Para estas pruebas **no** se envía token: probar desde una ventana de navegador sin sesión o Postman sin autorización.

### 5.6 Detalle y pruebas por endpoint

#### 5.6.1 `GET /public/accommodations/{accommodationId}/base-price`

**Qué hace.** Devuelve el `basePrice` del alojamiento en la moneda base y en cada moneda extra disponible.

**Respuesta `200 OK` (ejemplo con base 85000 ARS):**

```json
[
  { "currency": "ARS", "symbol": "AR$", "displayName": "Peso Argentino", "amount": 85000.00, "formatted": "AR$ 85000.00" },
  { "currency": "USD", "symbol": "US$", "displayName": "Dólar Estadounidense", "amount": 72.56, "formatted": "US$ 72.56" },
  { "currency": "EUR", "symbol": "€", "displayName": "Euro", "amount": 66.30, "formatted": "€ 66.30" }
]
```

**Prueba paso a paso:**

1. Abrir en el navegador `http://localhost:8080/api/v1/public/accommodations/1/base-price` (sin iniciar sesión).
2. Verificar **200** y que el primer elemento es ARS con el precio exacto del perfil.
3. Verificar que USD y EUR aparecen y que el monto es `precio × tasa` redondeado a 2 decimales. Ejemplo manual: `85000 × 0.00078 = 66.30`.
4. En el módulo 4, **deshabilitar EUR** y repetir: EUR debe desaparecer de la respuesta.
5. Cambiar USD a tasa manual `0.001`: el monto USD debe pasar a `85.00`.
6. Borrar la configuración (o probar con un alojamiento sin monedas configuradas): debe devolver solo ARS.
7. Probar un ID inexistente (`/public/accommodations/99999/base-price`): **404** — *"No se encontró el alojamiento con ID: 99999"*.
8. Probar un ID no numérico (`/public/accommodations/abc/base-price`): ⚠️ esperado `400`; puede aparecer `500`.

⚠️ **Caso límite a verificar:** si el alojamiento fue creado **sin** `basePrice` (campo opcional en el perfil), el código intenta formatear un monto nulo. Pruebe esta combinación: es probable que responda `500` en lugar de una lista vacía o solo con la moneda base.

#### 5.6.2 `GET /public/accommodations/{accommodationId}/additional-services`

**Qué hace.** Devuelve los servicios adicionales **activos** (`active = true`) del alojamiento, ordenados por fecha de creación, con sus precios convertidos y sus imágenes. Los servicios inactivos no se muestran al público.

**Prueba paso a paso:**

1. Abrir `http://localhost:8080/api/v1/public/accommodations/1/additional-services` sin sesión.
2. Verificar **200** y que aparecen solo los servicios activos.
3. Verificar en cada elemento: `id`, `name`, `description`, `baseCurrency`, `basePrice`, `priceType` (`PER_DAY` o `TOTAL`), `convertedPrices` (con la misma lógica que el precio base) e `images` (lista de hasta 3 imágenes).
4. Desactivar un servicio (`PATCH` con `active: false`, módulo 6) y repetir: debe desaparecer de la lista pública pero seguir apareciendo en el listado del dueño.
5. Probar con un alojamiento sin servicios: debe devolver `[]`.
6. Probar un ID inexistente: **404**.

⚠️ **Riesgo a verificar:** el controlador arma la respuesta fuera de una transacción y con `open-in-view=false`. Si al probar con servicios existentes aparece un error `500` (típicamente `LazyInitializationException` en los logs al leer el alojamiento del servicio), es por este motivo; habría que cargar esa relación en la consulta o envolver la lógica en una transacción.

### 5.7 Qué verificar en seguridad

- Ambos endpoints responden **sin** token.
- No se exponen datos sensibles: no deben aparecer correo, teléfono, métodos de pago ni datos del dueño.
- No se muestran servicios inactivos.

---

## 6. Módulo: Servicios adicionales

Swagger: sección **Servicios adicionales**. Controlador: `AdditionalServiceController`.

### 6.1 Alcance

Permite al dueño gestionar los servicios extra que ofrece junto con el alojamiento (desayuno, traslado, limpieza adicional, etc.):

- Crear, listar, actualizar y eliminar servicios.
- Ver el precio de cada servicio convertido a las monedas habilitadas.
- Subir hasta 3 imágenes por servicio (almacenadas en Cloudinary), listarlas y eliminarlas.

Todos los endpoints exigen rol `OWNER` y que el **perfil del alojamiento esté creado**. Un dueño solo puede ver y modificar los servicios de **su propio** alojamiento.

### 6.2 Modelo de datos

#### `additional_services`

| Columna | Tipo SQL | Nulo | Descripción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. |
| `accommodation_id` | `BIGINT` | No | FK `fk_additional_services_accommodation` a `accommodations(id)`. |
| `name` | `VARCHAR(150)` | No | Nombre del servicio. |
| `description` | `VARCHAR(1000)` | Sí | Descripción, horarios o condiciones. |
| `base_price` | `NUMERIC(12,2)` | No | Precio en la moneda base (ARS). |
| `price_type` | `VARCHAR(20)` | No | `PER_DAY` (por cada día de estadía) o `TOTAL` (monto único). Por defecto `TOTAL`. |
| `is_active` | `BOOLEAN` | No | Si está activo y visible para los huéspedes. |
| `created_at` / `updated_at` | `TIMESTAMP` | No | Auditoría. |

Índice `idx_additional_services_accommodation`. Eliminar un servicio elimina también sus imágenes (cascada).

#### `additional_service_images`

| Columna | Tipo SQL | Nulo | Descripción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. |
| `additional_service_id` | `BIGINT` | No | FK al servicio. |
| `image_url` | `VARCHAR(2048)` | No | URL segura de la imagen en Cloudinary. |
| `cloudinary_public_id` | `VARCHAR(500)` | No | Identificador del recurso en Cloudinary. |
| `cloudinary_folder` | `VARCHAR(255)` | Sí | Carpeta de destino: `hostflow/additional-services/{serviceId}`. |
| `file_size_bytes` | `BIGINT` | No | Tamaño del archivo. |
| `mime_type` | `VARCHAR(50)` | Sí | Tipo de contenido (por ejemplo `image/jpeg`). |
| `order_index` | `INTEGER` | No | Orden de visualización (empieza en 0). |
| `created_at` | `TIMESTAMP` | No | Fecha de registro. |

### 6.3 API HTTP — `/api/v1/owners/accommodation/additional-services`

| Método | Ruta | Resultado esperado | Descripción |
|---|---|---|---|
| `GET` | `/` | `200 OK` | Listar servicios con precios convertidos e imágenes. |
| `POST` | `/` | `201 Created` | Crear un servicio adicional. |
| `PATCH` | `/{serviceId}` | `200 OK` | Actualizar parcialmente un servicio. |
| `DELETE` | `/{serviceId}` | `204 No Content` | Eliminar un servicio (y sus imágenes). |
| `GET` | `/{serviceId}/images` | `200 OK` | Listar imágenes de un servicio. |
| `POST` | `/{serviceId}/images` | `201 Created` | Subir una imagen (`multipart/form-data`, campo `file`). |
| `DELETE` | `/{serviceId}/images/{imageId}` | `204 No Content` | Eliminar una imagen. |

> Las dos rutas finales que figuraban sueltas en la lista original (`/{serviceId}/images` y `/{serviceId}/images/{imageId}`) corresponden a estos mismos endpoints de imágenes; no son endpoints adicionales.

### 6.4 Reglas de negocio

| Regla | Detalle |
|---|---|
| Nombre | Obligatorio, máximo 150 caracteres. |
| Descripción | Opcional, máximo 1000 caracteres. |
| Precio base | Obligatorio, mínimo `0.01`, en la moneda base (ARS). |
| Tipo de cobro | `PER_DAY` o `TOTAL`; si se omite al crear, se asume `TOTAL`. |
| Estado | `active`; si se omite al crear, el servicio queda **activo**. |
| Imágenes por servicio | Máximo **3**. |
| Tamaño de imagen | Mínimo **500 KB**, máximo **2 MB**. |
| Tipo de archivo | El `Content-Type` debe comenzar con `image/`. |
| Orden de imágenes | Se asigna `order_index` consecutivo; al eliminar una, las restantes se reordenan (0, 1, 2…). |

El servidor también limita las cargas a 5 MB por archivo y 10 MB por solicitud (`spring.servlet.multipart`); la regla de 2 MB del módulo es más estricta y se aplica antes.

### 6.5 Preparación de las pruebas

1. Dueño verificado con `token` y perfil del alojamiento creado.
2. Monedas configuradas (módulo 4) para ver los precios convertidos.
3. Cloudinary configurado (`CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`) para las pruebas de imágenes.
4. Preparar **imágenes de prueba**: una de unos 800 KB (válida), una de unos 100 KB (muy pequeña), una de más de 2 MB (muy grande) y un archivo que no sea imagen (por ejemplo un `.txt`).
5. Para probar el aislamiento entre dueños, tener un **segundo dueño** con su propio perfil y token.

### 6.6 Detalle y pruebas por endpoint

#### 6.6.1 `POST /owners/accommodation/additional-services` — Crear un servicio adicional

**Qué hace.** Registra un servicio nuevo en el alojamiento del dueño autenticado. Las imágenes se agregan después con el endpoint de imágenes.

**Cuerpo (JSON):**

```json
{
  "name": "Desayuno buffet continental",
  "description": "Servido de 8:00 a 10:00 hs en el comedor principal.",
  "basePrice": 5000.00,
  "priceType": "PER_DAY",
  "active": true
}
```

**Respuesta `201 Created` (ejemplo):**

```json
{
  "id": 1,
  "name": "Desayuno buffet continental",
  "description": "Servido de 8:00 a 10:00 hs en el comedor principal.",
  "baseCurrency": "ARS",
  "basePrice": 5000.00,
  "priceType": "PER_DAY",
  "convertedPrices": [
    { "currency": "ARS", "symbol": "AR$", "displayName": "Peso Argentino", "amount": 5000.00, "formatted": "AR$ 5000.00" },
    { "currency": "USD", "symbol": "US$", "displayName": "Dólar Estadounidense", "amount": 4.27, "formatted": "US$ 4.27" }
  ],
  "images": [],
  "active": true
}
```

**Prueba paso a paso:**

1. Ejecutar `POST` con el cuerpo de ejemplo.
2. Verificar **201**, `id` asignado, `images: []`, `priceType: "PER_DAY"` y `active: true`.
3. Verificar que `convertedPrices` empieza por ARS con el precio exacto y contiene las demás monedas habilitadas con su monto convertido.
4. Crear otro servicio **sin** `priceType` ni `active`: debe quedar `TOTAL` y `active: true`.
5. Crear un tercero con `"active": false` y verificar `active: false`.
6. Verificar en la base de datos las tres filas en `additional_services`.

**Casos de error a probar:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| Nombre vacío | `"name": ""` | `400` con `fieldErrors.name` |
| Nombre de más de 150 caracteres | Texto largo | `400` con `fieldErrors.name` |
| Sin precio | Omitir `basePrice` | `400` con `fieldErrors.basePrice` |
| Precio cero o negativo | `"basePrice": 0` o `-5` | `400` — *"El precio base debe ser mayor a 0"* |
| Descripción de más de 1000 caracteres | Texto largo | `400` con `fieldErrors.description` |
| `priceType` inválido | `"priceType": "MENSUAL"` | ⚠️ Esperado `400`; puede aparecer `500`. Anotar. |
| Sin perfil de alojamiento | Usar un dueño sin perfil | `404` — *"Registra primero los datos generales del alojamiento."* |
| Sin token | Llamar sin `Authorization` | `403` |

#### 6.6.2 `GET /owners/accommodation/additional-services` — Listar servicios

**Qué hace.** Devuelve **todos** los servicios del alojamiento (activos e inactivos), ordenados por fecha de creación, cada uno con sus precios convertidos y sus imágenes.

**Prueba paso a paso:**

1. Con los tres servicios creados, ejecutar `GET`.
2. Verificar **200** y que aparecen los tres (incluido el inactivo) en orden de creación.
3. Verificar `convertedPrices` en cada uno y `images` (vacío o con las imágenes ya subidas).
4. Comparar con el endpoint público (módulo 5): el público debe mostrar solo los activos.
5. Con un dueño sin servicios: `[]`.
6. Con el **segundo dueño**: no deben aparecer los servicios del primero.

#### 6.6.3 `PATCH /owners/accommodation/additional-services/{serviceId}` — Actualizar parcialmente

**Qué hace.** Modifica los campos enviados de un servicio. En teoría, los campos omitidos o `null` quedan sin cambios. Solo funciona con servicios del alojamiento propio.

```http
PATCH /api/v1/owners/accommodation/additional-services/1
```

```json
{ "name": "Desayuno buffet premium", "basePrice": 6500.00 }
```

**Prueba paso a paso:**

1. Ejecutar el `PATCH` del ejemplo sobre un servicio existente.
2. Verificar **200** y que `name` y `basePrice` cambiaron, mientras `description`, `priceType` y `active` se mantienen.
3. Verificar que `convertedPrices` se recalculó con el nuevo precio.
4. Ejecutar un `PATCH` con `{"name":"Desayuno","basePrice":6500,"active":false}` y comprobar que pasa a inactivo.
5. Verificar que desaparece del listado público (módulo 5).

**Casos de error a probar:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| Servicio inexistente | `/additional-services/99999` | `404` — *"No se encontró ese servicio en tu alojamiento."* |
| Servicio de otro dueño | Usar el ID de un servicio del segundo dueño | `404` (mismo mensaje) |
| Precio inválido | `"basePrice": 0` | `400` |
| Nombre vacío | `"name": ""` | `400` |

⚠️ **Hallazgo importante para verificar:** el endpoint se llama "actualización parcial", pero usa el **mismo objeto de entrada que la creación**, con `name` y `basePrice` marcados como obligatorios (`@NotBlank` / `@NotNull`) y validados con `@Valid`. Por eso enviar **solo** `{"active": false}` (o solo `{"description": "…"}`) **probablemente responda `400`** con errores en `name` y `basePrice`. Pruebe exactamente eso: si falla, para que el `PATCH` sea realmente parcial hay que usar un DTO de actualización sin esas validaciones obligatorias.

⚠️ **Segundo riesgo:** el método de actualización arma la respuesta leyendo las imágenes y el alojamiento del servicio fuera de una transacción (`open-in-view=false`). Si el `PATCH` responde `500` con un `LazyInitializationException` en el log, es por esto.

#### 6.6.4 `DELETE /owners/accommodation/additional-services/{serviceId}` — Eliminar un servicio

**Qué hace.** Elimina permanentemente el servicio y, por cascada, sus registros de imágenes.

**Prueba paso a paso:**

1. Crear un servicio de prueba y subirle una imagen (sección 6.6.6).
2. Ejecutar `DELETE /owners/accommodation/additional-services/{serviceId}`.
3. Verificar **204** sin cuerpo.
4. Ejecutar `GET` del listado: el servicio ya no debe aparecer.
5. Verificar en la base que no quedan filas del servicio ni de `additional_service_images`.
6. ⚠️ Revisar en el panel de Cloudinary si el archivo de la imagen sigue existiendo: el servicio borra los registros de la base, pero **no se ve en este código que llame a Cloudinary para eliminar los archivos** de las imágenes del servicio (aunque la descripción de Swagger dice que sí). Anotar el resultado; si quedan archivos huérfanos, conviene eliminar las imágenes una por una antes, o completar la lógica.
7. Repetir el `DELETE` con el mismo ID: **404** — *"No se encontró ese servicio en tu alojamiento."*

**Otros casos:** servicio de otro dueño → `404`; sin token → `403`.

#### 6.6.5 `GET /{serviceId}/images` — Listar imágenes de un servicio

**Qué hace.** Devuelve las imágenes del servicio ordenadas por `orderIndex` (máximo 3).

**Respuesta `200 OK` (ejemplo):**

```json
[
  {
    "id": 10,
    "imageUrl": "https://res.cloudinary.com/.../hostflow/additional-services/1/abc.jpg",
    "cloudinaryPublicId": "hostflow/additional-services/1/abc",
    "fileSizeBytes": 812345,
    "mimeType": "image/jpeg",
    "orderIndex": 0,
    "createdAt": "2026-10-28T12:00:00"
  }
]
```

**Prueba paso a paso:**

1. Sobre un servicio sin imágenes: `[]`.
2. Subir 2 imágenes (sección 6.6.6) y repetir: deben aparecer con `orderIndex` 0 y 1.
3. Abrir la `imageUrl` en el navegador: debe mostrar la imagen.
4. Servicio inexistente o de otro dueño: `404` — *"No se encontró ese servicio adicional en tu alojamiento."*

#### 6.6.6 `POST /{serviceId}/images` — Subir una imagen a un servicio

**Qué hace.** Valida el archivo, lo sube a Cloudinary en la carpeta `hostflow/additional-services/{serviceId}` y registra la imagen con su siguiente `orderIndex`.

**Cómo enviar la petición:** `multipart/form-data` con **un único campo llamado `file`**.

- En Swagger: elegir el archivo con **Choose File** en el campo `file`.
- En Postman: pestaña **Body → form-data**, clave `file` de tipo **File**.
- Con `curl`:

```bash
curl -X POST 'http://localhost:8080/api/v1/owners/accommodation/additional-services/1/images' \
  -H 'Authorization: Bearer <JWT>' \
  -F 'file=@./desayuno.jpg'
```

**Prueba paso a paso:**

1. Subir la imagen válida (~800 KB, JPG o PNG). Verificar **201** con `id`, `imageUrl`, `cloudinaryPublicId`, `fileSizeBytes`, `mimeType` y `orderIndex: 0`.
2. Abrir `imageUrl` y comprobar que la imagen se ve.
3. Subir una segunda y una tercera imagen: `orderIndex` 1 y 2.
4. Intentar subir una **cuarta**: debe rechazar.
5. Verificar en Cloudinary que los archivos están en la carpeta `hostflow/additional-services/{serviceId}` y en la base, la tabla `additional_service_images`.
6. Consultar `GET /additional-services`: el servicio debe traer las 3 imágenes dentro de `images`.

**Casos de error a probar:**

| Caso | Cómo provocarlo | Resultado esperado |
|---|---|---|
| Cuarta imagen | Subir una imagen cuando ya hay 3 | `400` — *"El servicio ya tiene el máximo de 3 imágenes permitidas."* |
| Archivo vacío | Enviar un archivo de 0 bytes | `400` — *"Debes seleccionar una imagen que no esté vacía."* |
| No es imagen | Subir un `.txt` o `.pdf` | `400` — *"El archivo debe ser una imagen (JPEG, PNG, WEBP, etc.)."* |
| Muy pequeña | Imagen de ~100 KB | `400` — *"La imagen es demasiado pequeña… El tamaño mínimo es 500 KB."* |
| Muy grande | Imagen de ~3 MB | `400` — *"La imagen supera el tamaño máximo permitido… El tamaño máximo es 2 MB."* |
| Excede el límite del servidor | Imagen de más de 5 MB | `400` — *"El archivo supera el tamaño máximo permitido de 5 MB."* (código `FILE_TOO_LARGE`) |
| Sin archivo / campo con otro nombre | No enviar `file` o llamarlo distinto | `400` — *"Debes seleccionar una imagen para subir."* |
| Servicio inexistente / de otro dueño | ID inexistente | `404` |
| Cloudinary no disponible | Credenciales vacías o incorrectas | `502` — *"No fue posible procesar la imagen con el servicio de almacenamiento."* |

**Orden de validaciones:** primero se comprueba el límite de 3 imágenes y luego el archivo; por eso, con 3 imágenes ya subidas, cualquier archivo (incluso inválido) recibe el error de "máximo de imágenes".

#### 6.6.7 `DELETE /{serviceId}/images/{imageId}` — Eliminar una imagen

**Qué hace.** Intenta borrar el archivo en Cloudinary, elimina el registro de la base y **reordena** las imágenes restantes para que los `orderIndex` queden consecutivos (0, 1, 2…).

**Prueba paso a paso:**

1. Con un servicio con 3 imágenes (`orderIndex` 0, 1 y 2), eliminar la **del medio** (índice 1) con `DELETE /additional-services/{serviceId}/images/{imageId}`.
2. Verificar **204** sin cuerpo.
3. Ejecutar `GET /{serviceId}/images`: deben quedar 2 imágenes con `orderIndex` **0 y 1** (la antigua 2 pasó a 1).
4. Verificar en Cloudinary que el archivo eliminado ya no existe.
5. Subir una nueva imagen: ahora sí se permite (ya hay solo 2) y recibe `orderIndex: 2`.
6. Repetir el `DELETE` con el mismo `imageId`: **404** — *"No se encontró esa imagen en el servicio adicional."*

**Otros casos:** `imageId` de otro servicio o `serviceId` ajeno → `404`; sin token → `403`.

⚠️ **A anotar:** Swagger declara `502` si falla Cloudinary al eliminar, pero el servicio de imágenes **captura el error, lo registra como advertencia y elimina igualmente el registro de la base**. El resultado es `204` aunque Cloudinary falle (puede quedar el archivo huérfano). Esto difiere del comportamiento de la galería del alojamiento, donde, si Cloudinary no confirma, el registro se conserva.

### 6.7 Observaciones del código (para revisar)

- El `PATCH` exige `name` y `basePrice` por reutilizar el DTO de creación (ver 6.6.3).
- Los métodos de crear y actualizar del servicio están marcados como privados con `@Transactional`; esa anotación no se aplica a métodos privados, y el armado de la respuesta ocurre fuera de una transacción. Si aparecen errores `LazyInitializationException`, revisar esto.
- El borrado de un servicio no elimina los archivos de sus imágenes en Cloudinary.
- Borrar una imagen ignora los errores de Cloudinary (posibles archivos huérfanos).
- La descripción de Swagger del `POST` dice que el precio se almacena en ARS: coincide con `base_currency` por defecto, que hoy no se puede cambiar desde la API.

---

## 7. Resumen de hallazgos para revisar

Lista consolidada de lo detectado al leer el código. Los marcados como **Verificar** son hipótesis que deben confirmarse en la prueba.

| # | Módulo | Hallazgo | Tipo |
|---:|---|---|---|
| 1 | Autenticación | Credenciales incorrectas responden `400` aunque Swagger declara `401`. | Inconsistencia |
| 2 | Autenticación | El registro devuelve JWT utilizable sin haber verificado el correo. | Verificar / seguridad |
| 3 | Autenticación | `reset-password` no desbloquea la cuenta bloqueada por intentos fallidos. | Comportamiento |
| 4 | Autenticación | Códigos de 6 dígitos sin límite de intentos y con `Random` (no seguro). | Seguridad |
| 5 | Autenticación | El administrador semilla no puede iniciar sesión hasta verificar su correo. | Configuración |
| 6 | Autenticación | El ofuscado del correo falla con dominios sin punto (`@localhost`). | Verificar |
| 7 | Dueños | Un dueño puede consultar a otro por ID o correo (sin control de propiedad). | Verificar / seguridad |
| 8 | Dueños | Un dueño desactivado conserva acceso con un token ya emitido. | Verificar / seguridad |
| 9 | Global | Parámetros ausentes, enumerados inválidos, JSON mal formado y accesos denegados pueden responder `500` por el manejador genérico. | Verificar |
| 10 | Global | Token vencido o alterado puede terminar en `500` (error en el filtro JWT). | Verificar |
| 11 | Desactivación | Las desactivaciones no vencen solas (`is_active` solo cambia con `/enable`). | Verificar |
| 12 | Monedas | `effectiveRate` puede calcularse con el ID del dueño en lugar del ID del alojamiento. | Verificar / posible error |
| 13 | Monedas | La ayuda de Swagger lista tipos de dólar (`MEP`, `CRYPTO`) que no existen en el enumerado. | Documentación |
| 14 | Precios públicos | Alojamiento sin `basePrice` puede provocar `500` en `base-price`. | Verificar |
| 15 | Precios públicos | Posible `LazyInitializationException` al listar servicios públicos. | Verificar |
| 16 | Servicios adicionales | `PATCH` "parcial" exige `name` y `basePrice`. | Verificar |
| 17 | Servicios adicionales | Borrar un servicio no elimina los archivos en Cloudinary. | Verificar |
| 18 | Servicios adicionales | Borrar una imagen ignora fallos de Cloudinary y responde `204`. | Comportamiento |

---

## 8. Ubicación de la implementación

- **Controladores** (`src/main/java/com/hostflow/controller/`): `AuthController`, `OwnerController`, `AccommodationController`, `AccommodationCurrencyController`, `PublicPricingController`, `AdditionalServiceController`.
- **Servicios** (`src/main/java/com/hostflow/service/`): `AuthService`, `PasswordResetService`, `LoginAttemptService`, `EmailService`, `JwtService`, `OwnerService`, `AccommodationTemporaryDisableService`, `AcceptedCurrencyService`, `CurrencyConversionService`, `ExchangeRateProviderService`, `AdditionalServiceService`, `AdditionalServiceImageService`, `CloudinaryImageService`.
- **Modelos** (`src/main/java/com/hostflow/model/`): `Owner`, `Role`, `EmailVerificationToken`, `PasswordResetToken`, `LoginAttempt`, `AccommodationTemporaryDisable`, `Accommodation`, `AcceptedCurrency`, `ExchangeRateCache`, `AdditionalService`, `AdditionalServiceImage`.
- **DTO** (`src/main/java/com/hostflow/dto/request/` y `dto/response/`): `OwnerRegisterRequest`, `LoginRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `AuthResponse`, `PasswordResetResponse`, `OwnerResponse`, `AccommodationDisableRequest/Response`, `AcceptedCurrencyRequest/Response`, `ConvertedPriceResponse`, `AdditionalServiceRequest/Response`, `AdditionalServiceImageResponse`.
- **Seguridad y configuración** (`security/` y `config/`): `SecurityConfig`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, `InactivityConfig`, `DataInitializer`, `SwaggerConfig`.
- **Manejo de errores**: `src/main/java/com/hostflow/exception/GlobalExceptionHandler.java`.
- **Propiedades**: `application.properties`, `application-dev.properties` y `application-prod.properties` (en producción Swagger queda deshabilitado).
