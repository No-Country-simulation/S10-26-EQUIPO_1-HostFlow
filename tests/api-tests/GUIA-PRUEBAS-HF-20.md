# Guía de pruebas — Merge HF-20 (Perfil y galería del alojamiento)

> Commit del merge: `5482c23` (`feat/HF-20-accommodation-profile` → `QA-review`)
> Alcance: perfil del alojamiento, galería, logotipo, branding y métodos de pago.

---

## 1. Qué se incorporó en este merge

El merge agrega el módulo de gestión del alojamiento en el backend:

- Perfil del alojamiento (`POST/GET/PATCH /owners/accommodation/profile`).
- Galería de imágenes (`GET /gallery`, `POST /gallery/upload`, `DELETE /gallery/{imageId}`).
- Logotipo (`POST /profile/logo`).
- Métodos de pago del alojamiento (`GET/POST/PATCH/DELETE /owners/accommodation/payment-methods`).
- Migraciones Flyway `V2` a `V7`.
- Integración con Cloudinary (solo para subida de archivos).
- Tests unitarios/de integración en `backend/src/test/java/...`.

El prefijo de todas las rutas es `/api/v1` (context path declarado en `application.properties`).
Todos los endpoints de este módulo requieren `Bearer JWT` y rol `OWNER`.

---

## 2. Endpoints a validar

### Perfil y galería — `/api/v1/owners/accommodation`

| Método | Ruta | Esperado | Requiere Cloudinary |
|---|---|---|---|
| `POST` | `/profile` | `201 Created` | No |
| `GET` | `/profile` | `200 OK` | No |
| `PATCH` | `/profile` | `200 OK` | No |
| `GET` | `/gallery` | `200 OK` | No |
| `POST` | `/gallery/upload` | `201 Created` | Sí |
| `POST` | `/profile/logo` | `200 OK` | Sí |
| `DELETE` | `/gallery/{imageId}` | `204 No Content` | Solo si la imagen está en Cloudinary |

### Métodos de pago — `/api/v1/owners/accommodation/payment-methods`

| Método | Ruta | Esperado |
|---|---|---|
| `GET` | `/` | `200 OK` |
| `POST` | `/` | `201 Created` |
| `PATCH` | `/{methodId}` | `200 OK` |
| `DELETE` | `/{methodId}` | `204 No Content` |

### Autenticación (público, para obtener el token)

| Método | Ruta | Esperado |
|---|---|---|
| `POST` | `/api/v1/auth/register` | `201 Created` + token |
| `POST` | `/api/v1/auth/login` | `200 OK` + token |

---

## 3. Preparación del entorno

### 3.1 Requisitos

- JDK 17.
- PostgreSQL 13+ corriendo en `localhost:5432`.
- Base de datos creada:

```sql
CREATE DATABASE hostflow_dev;
```

- Archivo `backend/.env` (copiar desde `backend/.env.example`).
  Variables mínimas:

```properties
DB_USERNAME=postgres
DB_PASSWORD=tu_clave
JWT_SECRET=<clave de al menos 64 caracteres para HS512>
ADMIN_PASSWORD=<clave del admin>
```

Variables necesarias solo para probar **subida de imágenes**:

```properties
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
CLOUDINARY_FOLDER=hostflow
```



> Sin credenciales de Cloudinary la aplicación arranca igual, pero `POST /gallery/upload` y `POST /profile/logo` responderán `502`. Los demás endpoints funcionan.

### 3.2 Arrancar el backend

Desde la carpeta `backend`:

```bash
# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Verificar que arranca en `http://localhost:8080/api/v1`.

> Nota: Flyway está deshabilitado y se usa `spring.jpa.hibernate.ddl-auto=update`. El esquema lo crea/actualiza Hibernate al iniciar; las migraciones `V2`–`V7` no se ejecutan automáticamente en este perfil. Si quieres probar las migraciones reales, actívalas en un entorno controlado.

---

## 4. Paso 1 — Revisar y probar en Swagger

1. Abrir Swagger UI:

```text
http://localhost:8080/api/v1/swagger-ui.html
```

2. OpenAPI JSON (útil para importar en Postman):

```text
http://localhost:8080/api/v1/api-docs
```

3. Obtener un token:
   - Ejecutar `POST /auth/register` (con un email único) o `POST /auth/login`.
   - Copiar el campo `token` de la respuesta.

4. Autorizar en Swagger:
   - Clic en **Authorize**.
   - Pegar el token JWT. En el esquema `HTTP bearer` normalmente se pega **solo el token** (Swagger agrega `Bearer`). Si recibes `401`, prueba con `Bearer <token>`.

5. Revisar las etiquetas:
   - **Autenticación**
   - **Perfil y galería del alojamiento**
   - **Métodos de pago del alojamiento**

6. Probar primero los endpoints sin efectos externos: `GET /profile`, `GET /gallery`, `GET /payment-methods`.

### Hallazgos detectados antes de probar

Antes de armar la colección, ten en cuenta estas diferencias entre la documentación y el código de este merge:

1. `backend/docs/gestion-alojamiento.md` describe `POST /owners/accommodation/gallery` para registrar una URL HTTPS externa, pero **ese endpoint no existe** en `AccommodationProfileController`. El DTO `AccommodationImageRequest` está creado pero sin uso. En Swagger no aparecerá.
2. La misma documentación indica un límite de subida de 5 MB en una sección, pero el código (`CloudinaryImageService`) valida **entre 500 KB y 2 MB**. Usa 500 KB–2 MB.
3. La galería admite un **máximo de 4 imágenes** (`MAX_GALLERY_IMAGES = 4`).
4. El registro (`/auth/register`) crea el `Owner`, **no** crea el perfil de alojamiento. Hay que llamar `POST /profile` después.
5. El admin sembrado por `DataInitializer` (`admin@hostflow.com`) no tiene rol `OWNER`; con ese token los endpoints responderán `403`.

---

## 5. Paso 2 — Crear la colección de Postman

### Opción A: importar desde OpenAPI (recomendada para arrancar)

1. En Postman: **Import** → **Link**.
2. Pegar `http://localhost:8080/api/v1/api-docs`.
3. Revisar que se hayan importado los tags y endpoints.
4. Crear un **Environment** y una variable de colección para el token.

### Opción B: crear la colección manualmente

Estructura sugerida:

```text
HostFlow - HF-20
├── 00 Auth
│   ├── Register owner
│   └── Login owner
├── 01 Perfil
│   ├── Crear perfil
│   ├── Obtener perfil
│   ├── Actualizar perfil (PATCH)
│   └── Subir logo
├── 02 Galería
│   ├── Listar galería
│   ├── Subir imagen
│   └── Eliminar imagen
└── 03 Métodos de pago
    ├── Listar métodos
    ├── Crear transferencia bancaria
    ├── Crear billetera digital
    ├── Actualizar método
    └── Eliminar método
```

### Variables de entorno

| Variable | Valor inicial | Uso |
|---|---|---|
| `baseUrl` | `http://localhost:8080/api/v1` | Base de todas las requests |
| `token` | (vacío) | Se llena con el login/registro |
| `email` | `owner.qa+<fecha>@example.com` | Email único de prueba |
| `password` | `Password123` | Contraseña de prueba |
| `imageId` | (vacío) | ID devuelto al subir imagen |
| `methodId` | (vacío) | ID devuelto al crear método de pago |

### Autenticación de la colección

En la colección → **Authorization** → tipo **Bearer Token** → valor `{{token}}`.

### Scripts útiles

En `Login owner` y `Register owner` → pestaña **Tests**:

```javascript
const body = pm.response.json();
pm.environment.set("token", body.token);
```

En `Subir imagen` → **Tests**:

```javascript
const body = pm.response.json();
pm.environment.set("imageId", body.id);
```

En `Crear transferencia bancaria` → **Tests**:

```javascript
const body = pm.response.json();
pm.environment.set("methodId", body.id);
```

---

## 6. Paso 3 — Ejecutar el flujo end-to-end

Ejecutar en este orden. Cada paso depende del anterior.

### 6.1 Registro / login

`POST {{baseUrl}}/auth/register`

```json
{
  "fullName": "Owner QA",
  "accommodationName": "Cabañas QA",
  "email": "{{email}}",
  "password": "{{password}}",
  "termsAccepted": true
}
```

Esperado: `201 Created`, respuesta con `token`, `tokenType`, `userId`, `roles`.

Luego, `POST {{baseUrl}}/auth/login`:

```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

Esperado: `200 OK` con `token`.

### 6.2 Crear perfil

`POST {{baseUrl}}/owners/accommodation/profile`

```json
{
  "name": "Cabañas del Río",
  "roomCount": 8,
  "bathroomCount": 4,
  "guestCapacity": 20,
  "basePrice": 85000.00,
  "primaryColor": "#1E293B",
  "accentColor": "#d4f53c",
  "tertiaryColor": "#f4b8c4",
  "backgroundPattern": "waves",
  "welcomeMessage": "¡Te damos la bienvenida!",
  "email": "reservas@example.com",
  "phone": "+54 11 5555 5555",
  "address": "Delta del Tigre"
}
```

Esperado: `201 Created`. La respuesta debe incluir `id`, `ownerId`, colores, `backgroundPattern`, `images: []`, `createdAt`, `updatedAt`.

Comportamientos a verificar:

- Si se omiten los campos de branding, deben quedar los defaults:
  - `primaryColor` = `#0fa3a3`
  - `accentColor` = `#d4f53c`
  - `tertiaryColor` = `#f4b8c4`
  - `backgroundPattern` = `none`
- Segundo `POST /profile` con el mismo owner: `400` con `errorCode = ACCOMMODATION_ALREADY_EXISTS`.

### 6.3 Consultar y actualizar perfil

`GET {{baseUrl}}/owners/accommodation/profile` → `200 OK`.

`PATCH {{baseUrl}}/owners/accommodation/profile`

```json
{
  "basePrice": 90000.00,
  "backgroundPattern": "leaves"
}
```

Esperado: `200 OK`. Solo cambian los campos enviados; los omitidos permanecen iguales.

`PATCH` con `{}` → `400` (debe enviar al menos un campo).

### 6.4 Galería

`GET {{baseUrl}}/owners/accommodation/gallery` → `200 OK` (lista, puede estar vacía).

`POST {{baseUrl}}/owners/accommodation/gallery/upload`

- Body: `form-data`
- Key: `file` (tipo **File**)
- Archivo: imagen real de **entre 500 KB y 2 MB** (`.jpg`/`.png`).

Esperado: `201 Created` con `{ id, url, createdAt }`.

> Requiere credenciales de Cloudinary. Si no las tienes, este paso responderá `502` con `errorCode = CLOUDINARY_UPLOAD_FAILED`; documéntalo como bloqueo de entorno, no como fallo funcional.

`DELETE {{baseUrl}}/owners/accommodation/gallery/{{imageId}}` → `204 No Content`.

Repetir la subida más de 4 veces → la quinta debe responder `400` con `errorCode = GALLERY_IMAGE_LIMIT_EXCEEDED`.

### 6.5 Logotipo

`POST {{baseUrl}}/owners/accommodation/profile/logo`

- Body: `form-data`
- Key: `file` (tipo **File**)
- Imagen de 500 KB a 2 MB.

Esperado: `200 OK` con el perfil actualizado y `logoUrl` con la URL de Cloudinary.

### 6.6 Métodos de pago

`POST {{baseUrl}}/owners/accommodation/payment-methods`

Transferencia bancaria:

```json
{
  "paymentType": "BANK_TRANSFER",
  "displayName": "Transferencia Banco Nación",
  "bankName": "Banco Nación",
  "accountHolder": "Owner QA",
  "accountType": "Cuenta corriente",
  "accountNumber": "123456789",
  "cbuCvu": "0110599520000012345678",
  "alias": "cabanas.qa",
  "instructions": "Enviar comprobante por correo"
}
```

Esperado: `201 Created`. El primer método debe quedar con `isDefault: true` e `isActive: true`.

Billetera digital:

```json
{
  "paymentType": "DIGITAL_WALLET",
  "displayName": "Mercado Pago",
  "providerName": "Mercado Pago",
  "accountHolder": "Owner QA",
  "userIdentifier": "owner.qa",
  "walletPhone": "+5491123456789",
  "walletEmail": "pagos@example.com"
}
```

Esperado: `201 Created` con `isDefault: false` (ya existe un default).

Marcar la billetera como predeterminada:

`PATCH {{baseUrl}}/owners/accommodation/payment-methods/{{methodId}}`

```json
{
  "isDefault": true
}
```

Esperado: `200 OK` y el método anterior debe quedar con `isDefault: false`.

`GET {{baseUrl}}/owners/accommodation/payment-methods` → `200 OK`, ordenado por defecto primero.

`DELETE {{baseUrl}}/owners/accommodation/payment-methods/{{methodId}}` → `204 No Content`. Si se elimina el predeterminado y quedan métodos, otro debe pasar a `isDefault: true`.

---

## 7. Casos negativos mínimos

| Caso | Request | Esperado |
|---|---|---|
| Sin token | Cualquier endpoint del módulo | `401` o `403` (según entry point de Spring Security) |
| Token de admin | Cualquier endpoint del módulo | `403` (requiere `OWNER`) |
| Perfil inexistente | `GET /profile` antes de crearlo | `404` |
| Crear perfil sin `name` | `POST /profile` con `{}` | `400` |
| Crear perfil duplicado | Segundo `POST /profile` | `400` `ACCOMMODATION_ALREADY_EXISTS` |
| PATCH vacío | `PATCH /profile` con `{}` | `400` |
| Color inválido | `PATCH /profile` con `primaryColor: "azul"` | `400` con `fieldErrors` |
| Precio negativo | `PATCH /profile` con `basePrice: -1` | `400` |
| Patrón inválido | `backgroundPattern: "estrellas"` | `400` |
| Pago antes de perfil | `GET /payment-methods` sin perfil | `404` |
| Pago sin campos base | `POST /payment-methods` con `{}` | `400` |
| Transferencia incompleta | `BANK_TRANSFER` sin `bankName`, `cbuCvu`, etc. | `400` |
| Billetera con email inválido | `DIGITAL_WALLET` con `walletEmail: "abc"` | `400` |
| PATCH pago vacío | `PATCH /payment-methods/{id}` con `{}` | `400` |
| Pago ajeno/inexistente | `DELETE /payment-methods/999999` | `404` |
| Imagen muy chica | Upload de archivo < 500 KB | `400` `INVALID_IMAGE` |
| Imagen muy grande | Upload de archivo > 2 MB | `400` `FILE_TOO_LARGE` |
| Archivo no imagen | Upload de `.txt` | `400` `INVALID_IMAGE` |
| Sin archivo | Upload sin `file` | `400` `INVALID_IMAGE` |
| Imagen ajena/inexistente | `DELETE /gallery/999999` | `404` |

---

## 8. Formato de errores

Errores de negocio:

```json
{
  "timestamp": "2026-10-31T16:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "paymentType y displayName son obligatorios.",
  "errorCode": "BUSINESS_ERROR"
}
```

Errores de validación:

```json
{
  "timestamp": "2026-10-31T16:30:00",
  "status": 400,
  "error": "Error de validación",
  "message": "Los datos enviados no son válidos",
  "fieldErrors": {
    "primaryColor": "primaryColor debe ser un color hexadecimal, por ejemplo #AABBCC"
  }
}
```

---

## 9. Checklist de cierre

- [ ] Backend arranca con perfil `dev` y Swagger es accesible.
- [ ] Registro/login devuelve JWT.
- [ ] `POST /profile` responde `201` y aplica defaults de branding.
- [ ] `GET /profile` y `PATCH /profile` respetan el comportamiento parcial.
- [ ] `GET /gallery` responde `200`.
- [ ] Subida de imagen responde `201` (si hay Cloudinary) o `502` documentado.
- [ ] Límite de 4 imágenes verificado.
- [ ] `DELETE /gallery/{id}` responde `204`.
- [ ] CRUD de métodos de pago validado, incluida la regla de un único default.
- [ ] Casos negativos verificados.
- [ ] Hallazgos de documentación (endpoint `POST /gallery` ausente y límite 5 MB vs 2 MB) reportados al equipo.
- [ ] Evidencia adjunta (capturas de Postman o export de la colección) en la carpeta `tests/api-tests`.
