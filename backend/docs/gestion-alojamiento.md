# Módulo: Gestión del alojamiento

> Documentación del código del módulo, sus entidades JPA y las migraciones Flyway `V2`–`V7`. 

## 1. Alcance

El módulo permite al propietario autenticado:

- Crear, consultar y actualizar parcialmente el perfil de su alojamiento.
- Registrar imágenes de galería mediante URL HTTPS o subir archivos a Cloudinary.
- Subir o reemplazar el logotipo del alojamiento.
- Eliminar imágenes de galería, distinguiendo entre recursos gestionados por Cloudinary y URLs externas.
- Crear, consultar, actualizar y eliminar métodos de pago.
- Consultar y gestionar la desactivación temporal del alojamiento mediante un controlador separado.

El controlador de perfil y galería (`AccommodationProfileController`) y el controlador de métodos de pago (`AccommodationPaymentMethodController`) requieren el rol `OWNER`. La desactivación temporal se mantiene en `AccommodationController`; editar el perfil no activa ni desactiva el alojamiento.

## 2. Modelo de datos

Las columnas que se indican como obligatorias reflejan tanto las restricciones de la entidad actual como las migraciones relevantes. Las columnas `logo_public_id` y `cloudinary_public_id` se agregan en `V5` y `V4`, respectivamente.

### `accommodations`

| Columna | Tipo SQL | Nulo | Clave / descripción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. Identificador del alojamiento. |
| `owner_id` | `BIGINT` | No | FK `fk_accommodations_owner` a `owners(id)`; único. Un alojamiento por propietario. |
| `name` | `VARCHAR(200)` | No | Nombre comercial. |
| `room_count` | `INTEGER` | Sí | Cantidad de habitaciones. |
| `bathroom_count` | `INTEGER` | Sí | Cantidad de baños. |
| `guest_capacity` | `INTEGER` | Sí | Capacidad de huéspedes. |
| `base_price` | `NUMERIC(12,2)` | Sí | Precio base. |
| `logo_url` | `VARCHAR(2048)` | Sí | URL segura del logotipo. |
| `logo_public_id` | `VARCHAR(255)` | Sí | Identificador del recurso de Cloudinary; migración `V5`. |
| `primary_color` | `VARCHAR(20)` | Sí | Color principal hexadecimal; valor predeterminado de aplicación `#0fa3a3`. |
| `accent_color` | `VARCHAR(7)` | No | Color de acento hexadecimal; predeterminado `#d4f53c`. Añadido en `V7`. |
| `tertiary_color` | `VARCHAR(7)` | No | Color terciario hexadecimal; predeterminado `#f4b8c4`. Añadido en `V7`. |
| `background_pattern` | `VARCHAR(20)` | No | Identificador del patrón de fondo; predeterminado `none`. Añadido en `V7`. |
| `welcome_message` | `TEXT` | Sí | Mensaje de bienvenida. |
| `email` | `VARCHAR(150)` | Sí | Correo de contacto. |
| `phone` | `VARCHAR(40)` | Sí | Teléfono de contacto. |
| `address` | `TEXT` | Sí | Dirección. |
| `created_at` | `TIMESTAMP` | No | Fecha de creación; no se actualiza después. |
| `updated_at` | `TIMESTAMP` | No | Fecha de última actualización. |

`owner_id` tiene una restricción única, además de ser FK. Las relaciones de imágenes y métodos de pago se modelan como colecciones del alojamiento. La migración `V3` elimina de `accommodations` las columnas bancarias heredadas después de migrar los datos a `accommodation_payment_methods`.

### `accommodation_images`

| Columna | Tipo SQL | Nulo | Clave / descripción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. |
| `accommodation_id` | `BIGINT` | No | FK `fk_accommodation_images_accommodation` a `accommodations(id)`. |
| `image_url` | `VARCHAR(2048)` | No | URL de la imagen; no almacena el archivo binario. |
| `cloudinary_public_id` | `VARCHAR(255)` | Sí | ID de Cloudinary para imágenes subidas por HostFlow; `NULL` para URLs externas manuales. |
| `created_at` | `TIMESTAMP` | No | Fecha de registro. |

Índice: `idx_accommodation_images_accommodation` sobre `accommodation_id`. La FK tiene `ON DELETE CASCADE` en la migración.

### `accommodation_payment_methods`

| Columna | Tipo SQL | Nulo | Clave / descripción |
|---|---|---:|---|
| `id` | `BIGSERIAL` | No | PK. |
| `accommodation_id` | `BIGINT` | No | FK `fk_payment_methods_accommodation` a `accommodations(id)`. |
| `payment_type` | `VARCHAR(30)` | No | Enum: `BANK_TRANSFER`, `DIGITAL_WALLET`, `CASH`, `CARD`, `OTHER`. |
| `display_name` | `VARCHAR(120)` | No | Nombre visible del método. |
| `provider_name` | `VARCHAR(150)` | Sí | Proveedor. |
| `bank_name` | `VARCHAR(150)` | Sí | Banco o entidad. |
| `account_holder` | `VARCHAR(200)` | Sí | Titular. |
| `account_number` | `VARCHAR(100)` | Sí | Número de cuenta. |
| `cbu_cvu` | `VARCHAR(100)` | Sí | CBU/CVU. |
| `alias` | `VARCHAR(100)` | Sí | Alias de cuenta. |
| `tax_id` | `VARCHAR(30)` | Sí | Identificador fiscal. |
| `instructions` | `TEXT` | Sí | Instrucciones para el pago. |
| `is_default` | `BOOLEAN` | No | Indica el método predeterminado; default SQL `FALSE`. |
| `is_active` | `BOOLEAN` | No | Indica si está activo; default SQL `TRUE`. |
| `created_at` | `TIMESTAMP` | No | Fecha de creación. |
| `updated_at` | `TIMESTAMP` | No | Fecha de última actualización. |

Índice: `idx_payment_methods_accommodation` sobre `accommodation_id`. La migración crea el índice único parcial `uk_payment_methods_one_default_per_accommodation` para limitar a un método predeterminado por alojamiento. La FK tiene `ON DELETE CASCADE` en la migración.

### Migraciones relacionadas

- `V2__create_accommodation_profile_and_gallery.sql`: crea el perfil y la galería.
- `V3__separate_accommodation_payment_methods.sql`: crea métodos de pago, migra los datos bancarios heredados y elimina las columnas bancarias antiguas del perfil.
- `V4__add_cloudinary_public_id_to_accommodation_images.sql`: añade el ID de Cloudinary a la galería.
- `V5__add_cloudinary_public_id_to_accommodations.sql`: añade el ID de Cloudinary del logotipo.
- `V7__add_accommodation_branding_fields.sql`: añade `accent_color`, `tertiary_color` y `background_pattern`; establece valores predeterminados para la configuración de marca y rellena `primary_color` cuando está vacío o es `NULL`, sin reemplazar colores principales existentes.

**Nota de entorno:** en los archivos de propiedades revisados, Flyway está deshabilitado en la configuración base y de desarrollo, y Hibernate usa `ddl-auto=update`. La existencia de estas migraciones en el repositorio no significa que se haya verificado su ejecución en una base PostgreSQL real.

## 3. API HTTP

La aplicación declara el context path `/api/v1`; por eso las rutas completas siguientes incluyen ese prefijo. Se necesita autenticación Bearer JWT y rol `OWNER`, salvo que la configuración global de seguridad establezca una restricción adicional.

### Perfil y galería — `/api/v1/owners/accommodation`

| Método | Ruta | Resultado esperado | Descripción |
|---|---|---|---|
| `POST` | `/profile` | `201 Created` | Crea el perfil del propietario autenticado. |
| `GET` | `/profile` | `200 OK` | Obtiene el perfil y las imágenes de galería. |
| `PATCH` | `/profile` | `200 OK` | Actualiza parcialmente el perfil. |
| `GET` | `/gallery` | `200 OK` | Lista las referencias de imagen. |
| `POST` | `/gallery` | `201 Created` | Registra una URL HTTPS externa. |
| `POST` | `/gallery/upload` | `201 Created` | Sube un archivo de imagen a Cloudinary y registra la URL. |
| `POST` | `/profile/logo` | `200 OK` | Sube o reemplaza el logotipo. |
| `DELETE` | `/gallery/{imageId}` | `204 No Content` | Elimina una imagen de la galería. |

Para crear/actualizar el perfil se acepta JSON con estos campos: `name`, `roomCount`, `bathroomCount`, `guestCapacity`, `basePrice`, `primaryColor`, `accentColor`, `tertiaryColor`, `backgroundPattern`, `welcomeMessage`, `email`, `phone` y `address`. `name` es obligatorio al crear. Entre las validaciones declaradas están: nombre hasta 200 caracteres; habitaciones de 1 a 10.000; baños de 0 a 10.000; capacidad de 1 a 100.000; precio no negativo con hasta dos decimales; colores en formato hexadecimal `#RGB` o `#RRGGBB`; correo válido; y patrón de fondo limitado a los identificadores enumerados más abajo.

Ejemplo de creación:

```http
POST /api/v1/owners/accommodation/profile
Authorization: Bearer <JWT>
Content-Type: application/json
```

```json
{
  "name": "Cabañas del Río",
  "roomCount": 8,
  "bathroomCount": 4,
  "guestCapacity": 20,
  "basePrice": 85000.00,
  "primaryColor": "#0fa3a3",
  "accentColor": "#d4f53c",
  "tertiaryColor": "#f4b8c4",
  "backgroundPattern": "none",
  "welcomeMessage": "¡Te damos la bienvenida!",
  "email": "reservas@example.com",
  "phone": "+54 11 5555 5555",
  "address": "Delta del Tigre"
}
```

La configuración visual del alojamiento se compone de tres colores independientes y un identificador de patrón:

| Campo JSON | Valor predeterminado al crear | Regla |
|---|---|---|
| `primaryColor` | `#0fa3a3` | Color principal hexadecimal. |
| `accentColor` | `#d4f53c` | Color de acento hexadecimal. |
| `tertiaryColor` | `#f4b8c4` | Color terciario hexadecimal. |
| `backgroundPattern` | `none` | Debe ser uno de `none`, `leaves`, `waves`, `petals`, `arches`, `mountains`, `scales` o `sun`. |

Los colores se guardan por separado; el backend no deriva un color a partir de otro. `backgroundPattern` almacena un identificador, no una URL ni un archivo de imagen. La aplicación frontend decide cómo utilizar los tres colores y cómo representar el patrón. Esta API no persiste colores derivados para modo oscuro.

Al crear un perfil, los valores de marca omitidos o vacíos reciben los valores predeterminados anteriores. En `PATCH /profile`, los campos omitidos o enviados como `null` permanecen sin cambios; un campo de marca enviado con un valor no nulo actualiza únicamente ese campo. Una solicitud sin campos modificables se rechaza. No se acepta un `ownerId` del cliente para decidir a qué propietario pertenece el registro: se obtiene a partir de la autenticación.

Para añadir una URL externa:

```http
POST /api/v1/owners/accommodation/gallery
Authorization: Bearer <JWT>
Content-Type: application/json
```

```json
{ "url": "https://example.com/images/cabin-exterior.jpg" }
```

La URL debe ser HTTPS válida y tener como máximo 2048 caracteres. Esta operación no carga el archivo al backend ni lo administra en Cloudinary.

Para subir una imagen de galería o el logotipo, usar `multipart/form-data` con un único campo de archivo llamado `file`:

```bash
curl -X POST 'http://localhost:8080/api/v1/owners/accommodation/gallery/upload' \
  -H 'Authorization: Bearer <JWT>' \
  -F 'file=@./imagen.jpg'
```

Para el logotipo, cambiar la ruta por `/api/v1/owners/accommodation/profile/logo`. El archivo debe estar presente, no vacío, tener un `Content-Type` que comience por `image/` y no superar 5 MB. Los límites HTTP de multipart también están configurados en 5 MB para archivo y solicitud.

### Métodos de pago — `/api/v1/owners/accommodation/payment-methods`

| Método | Ruta | Resultado esperado | Descripción |
|---|---|---|---|
| `GET` | `/` | `200 OK` | Lista los métodos del alojamiento propio. |
| `POST` | `/` | `201 Created` | Crea un método. |
| `PATCH` | `/{methodId}` | `200 OK` | Actualiza campos enviados. |
| `DELETE` | `/{methodId}` | `204 No Content` | Elimina un método. |

El cuerpo JSON admite `paymentType`, `displayName`, `providerName`, `bankName`, `accountHolder`, `accountNumber`, `cbuCvu`, `alias`, `taxId`, `instructions`, `isDefault` e `isActive`. `paymentType` y `displayName` son obligatorios al crear. Los valores de `paymentType` son `BANK_TRANSFER`, `DIGITAL_WALLET`, `CASH`, `CARD` y `OTHER`.

Reglas implementadas: el primer método se establece como predeterminado si todavía no existe uno; al marcar otro como predeterminado se desmarca el anterior; si se elimina el predeterminado y quedan métodos, se elige otro. Las consultas, actualizaciones y eliminaciones limitan el método al alojamiento del propietario autenticado.

## 4. Cloudinary y consistencia

La configuración usa las propiedades `cloudinary.cloud-name`, `cloudinary.api-key`, `cloudinary.api-secret` y `cloudinary.folder` (por defecto `hostflow`). Deben suministrarse mediante variables de entorno o configuración local; no se deben subir secretos al repositorio. Para crear una cuenta de desarrollo, configurar las variables y replicar la integración, consultar [`docs/cloudinary-setup.md`](cloudinary-setup.md). La carpeta configurada es el destino de las cargas, no una garantía de aislamiento de permisos.

- `POST /gallery/upload`: carga el archivo en Cloudinary y guarda en la base la URL segura y `cloudinary_public_id`.
- `POST /profile/logo`: carga primero el archivo nuevo, actualiza `logo_url` y `logo_public_id`, y coordina la limpieza del recurso anterior con la finalización de la transacción de base de datos.
- Si la transacción del cambio de logotipo confirma, se intenta borrar el logotipo anterior cuando hay un `logo_public_id` conocido. Si la transacción falla, se intenta borrar el recurso nuevo para evitar dejarlo huérfano.
- Si el logotipo anterior no tiene `logo_public_id`, se conserva por seguridad: no se intenta inferir el identificador desde una URL.
- Si la eliminación posterior del logotipo anterior falla, se registra una advertencia para limpieza posterior; el nuevo logotipo permanece guardado.
- `DELETE /gallery/{imageId}`: para recursos con `cloudinary_public_id`, intenta confirmar primero la eliminación remota y después elimina el registro. Si Cloudinary no confirma, el registro se conserva. Las URLs externas agregadas manualmente solo se quitan de la galería.
- Una respuesta de Cloudinary con resultado `not found` al eliminar se considera éxito idempotente.

Las operaciones externas de Cloudinary y la transacción SQL no forman una transacción distribuida; la lógica descrita reduce el riesgo de pérdida de referencias, pero un fallo remoto durante la limpieza puede requerir intervención posterior.

## 5. Configuración local

Configuración relevante observada en el proyecto:

- Puerto por defecto: `8080`; prefijo de API: `/api/v1`.
- Perfil de desarrollo: `application-dev.properties` importa opcionalmente `./.env` y usa `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` con valores alternativos para desarrollo.
- Variables de Cloudinary: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`; `CLOUDINARY_FOLDER` es opcional y por defecto `hostflow`.
- También se requieren las variables de seguridad que la aplicación use en su entorno, como `JWT_SECRET` y `ADMIN_PASSWORD`. El correo SMTP se configura con `MAIL_USERNAME` y `MAIL_PASSWORD`.

## 6. Pruebas revisadas

Los tests de integración y unitarios fueron ejecutados después de incorporar los campos de marca; el usuario confirmó que todos los tests pasaron. Los escenarios relevantes de perfil incluyen valores predeterminados y actualización parcial de la configuración visual. Los reportes Surefire revisados anteriormente registraban las siguientes pruebas, con 1 prueba ejecutada, 0 fallos y 0 errores en cada reporte:

- `AccommodationProfileServiceTest`
- `AccommodationProfileRollbackIntegrationTest`
- `AccommodationProfileReplacementIntegrationTest`

Las pruebas de integración del reemplazo y rollback usan una configuración de pruebas con H2 y Cloudinary simulado. Son evidencia de los escenarios cubiertos en esas pruebas; no demuestran por sí solas que las migraciones se hayan aplicado sobre PostgreSQL real ni que Cloudinary remoto esté operativo.

## 7. Ubicación de la implementación

- Controladores: `src/main/java/com/hostflow/controller/AccommodationProfileController.java`, `AccommodationPaymentMethodController.java` y `AccommodationController.java`.
- Servicios: `src/main/java/com/hostflow/service/AccommodationProfileService.java`, `AccommodationPaymentMethodService.java` y `CloudinaryImageService.java`.
- Modelos: `src/main/java/com/hostflow/model/Accommodation.java`, `AccommodationImage.java` y `AccommodationPaymentMethod.java`.
- Migraciones: `src/main/resources/db/migration/V2__create_accommodation_profile_and_gallery.sql` hasta `V7__add_accommodation_branding_fields.sql`, incluidas las migraciones intermedias `V3`–`V6`.
