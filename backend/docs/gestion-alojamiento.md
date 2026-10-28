# Módulo: Gestión del alojamiento

> Documentación del código del módulo, sus entidades JPA y las migraciones Flyway `V2`–`V6`. 

## 1. Alcance

El módulo permite al propietario autenticado:

- Crear, consultar y actualizar parcialmente el perfil de su alojamiento.
- Subir hasta cuatro imágenes de galería a Cloudinary.
- Subir o reemplazar el logotipo del alojamiento.
- Eliminar imágenes de galería gestionadas por Cloudinary.
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
| `primary_color` | `VARCHAR(20)` | Sí | Color hexadecimal de la identidad visual. |
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
| `cloudinary_public_id` | `VARCHAR(255)` | Sí | ID del recurso de Cloudinary; nullable para conservar registros históricos. |
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
| `bank_name` | `VARCHAR(150)` | Sí | Banco o entidad; obligatorio para `BANK_TRANSFER`. |
| `account_holder` | `VARCHAR(200)` | Sí | Titular; obligatorio para `BANK_TRANSFER` y `DIGITAL_WALLET`. |
| `account_type` | `VARCHAR(80)` | Sí | Tipo de cuenta bancaria; obligatorio para `BANK_TRANSFER` (migración `V6`). |
| `account_number` | `VARCHAR(100)` | Sí | Número de cuenta; obligatorio para `BANK_TRANSFER`. |
| `cbu_cvu` | `VARCHAR(100)` | Sí | CBU/CVU; obligatorio para `BANK_TRANSFER`. |
| `alias` | `VARCHAR(100)` | Sí | Alias de cuenta. |
| `tax_id` | `VARCHAR(30)` | Sí | Identificador fiscal del titular o proveedor; opcional en la validación actual. |
| `user_identifier` | `VARCHAR(200)` | Sí | Nombre de usuario o identificador de la billetera digital; obligatorio para `DIGITAL_WALLET` (migración `V6`). |
| `wallet_phone` | `VARCHAR(50)` | Sí | Teléfono asociado a la billetera digital; obligatorio para `DIGITAL_WALLET` (migración `V6`). |
| `wallet_email` | `VARCHAR(254)` | Sí | Correo asociado a la billetera digital; obligatorio y validado por formato para `DIGITAL_WALLET` (migración `V6`). |
| `instructions` | `TEXT` | Sí | Instrucciones para el pago; hasta 5.000 caracteres en el DTO de solicitud. |
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
- `V6__add_payment_method_type_specific_fields.sql`: añade `account_type`, `user_identifier`, `wallet_phone` y `wallet_email` para completar los datos específicos de transferencias bancarias y billeteras digitales. Estos campos permanecen nullable en SQL porque no aplican a todos los tipos de pago; la obligatoriedad condicional se valida en el servicio.

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
| `POST` | `/gallery/upload` | `201 Created` | Sube un archivo de imagen a Cloudinary y registra la URL. |
| `POST` | `/profile/logo` | `200 OK` | Sube o reemplaza el logotipo. |
| `DELETE` | `/gallery/{imageId}` | `204 No Content` | Elimina una imagen de la galería. |

Para crear/actualizar el perfil se acepta JSON con estos campos: `name`, `roomCount`, `bathroomCount`, `guestCapacity`, `basePrice`, `primaryColor`, `welcomeMessage`, `email`, `phone`, `address`. `name` es obligatorio al crear. Entre las validaciones declaradas están: nombre hasta 200 caracteres; habitaciones de 1 a 10.000; baños de 0 a 10.000; capacidad de 1 a 100.000; precio no negativo con hasta dos decimales; color `#RGB` o `#RRGGBB`; correo válido.

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
  "primaryColor": "#1E293B",
  "welcomeMessage": "¡Te damos la bienvenida!",
  "email": "reservas@example.com",
  "phone": "+54 11 5555 5555",
  "address": "Delta del Tigre"
}
```

En `PATCH /profile`, los campos omitidos o enviados como `null` permanecen sin cambios; una solicitud sin campos modificables se rechaza. No se acepta un `ownerId` del cliente para decidir a qué propietario pertenece el registro: se obtiene a partir de la autenticación.

Para subir una imagen de galería o el logotipo, usar `multipart/form-data` con un único campo de archivo llamado `file`:

```bash
curl -X POST 'http://localhost:8080/api/v1/owners/accommodation/gallery/upload' \
  -H 'Authorization: Bearer <JWT>' \
  -F 'file=@./imagen.jpg'
```

Para el logotipo, cambiar la ruta por `/api/v1/owners/accommodation/profile/logo`. El archivo debe estar presente, tener entre 500 KB (500.000 bytes) y 2 MB (2.000.000 bytes), inclusive, y tener un `Content-Type` que comience por `image/`. La galería admite como máximo cuatro imágenes por alojamiento; el logotipo se gestiona por separado y no cuenta para ese límite. La comprobación de cantidad se realiza bajo bloqueo de la fila del alojamiento para evitar que cargas simultáneas excedan el límite. Los límites HTTP de multipart están configurados en 2 MB (2.000.000 bytes) por archivo y 3 MB (3.000.000 bytes) por solicitud para permitir el encabezado multipart.

### Métodos de pago — `/api/v1/owners/accommodation/payment-methods`

| Método | Ruta | Resultado esperado | Descripción |
|---|---|---|---|
| `GET` | `/` | `200 OK` | Lista los métodos del alojamiento propio. |
| `POST` | `/` | `201 Created` | Crea un método. |
| `PATCH` | `/{methodId}` | `200 OK` | Actualiza campos enviados. |
| `DELETE` | `/{methodId}` | `204 No Content` | Elimina un método. |

El cuerpo JSON admite `paymentType`, `displayName`, `providerName`, `bankName`, `accountHolder`, `accountType`, `accountNumber`, `cbuCvu`, `alias`, `taxId`, `userIdentifier`, `walletPhone`, `walletEmail`, `instructions`, `isDefault` e `isActive`. `paymentType` y `displayName` son obligatorios al crear. Los valores de `paymentType` son `BANK_TRANSFER`, `DIGITAL_WALLET`, `CASH`, `CARD` y `OTHER`.

Los campos obligatorios dependen de `paymentType` y se validan en `AccommodationPaymentMethodService`. En la creación, además de `paymentType` y `displayName`, se exigen los siguientes campos según el tipo:

- `BANK_TRANSFER` (transferencia bancaria): `bankName` (banco o entidad), `accountHolder` (titular), `accountType` (tipo de cuenta), `accountNumber` (número de cuenta), `cbuCvu` (CBU/CVU) y `alias`.
- `DIGITAL_WALLET` (billetera digital): `accountHolder` (titular), `userIdentifier` (nombre de usuario o identificador), `walletPhone` (teléfono) y `walletEmail` (correo electrónico). `walletEmail` también debe cumplir el formato de correo comprobado por el servicio. `providerName` puede utilizarse para indicar el proveedor de la billetera.
- `CASH`, `CARD` y `OTHER`: no requieren los campos específicos de transferencia bancaria ni de billetera digital; en esta implementación, solo son obligatorios `paymentType` y `displayName`.

Los demás campos disponibles son opcionales para la validación del servicio: `providerName`, `taxId` e `instructions`, además de los campos de otros tipos que no correspondan al método seleccionado. Los límites declarados en el DTO incluyen `displayName` 120 caracteres, `providerName` y `bankName` 150, `accountHolder` 200, `accountType` 80, `accountNumber`, `cbuCvu` y `alias` 100, `taxId` 30, `userIdentifier` 200, `walletPhone` 50, `walletEmail` 254 e `instructions` 5.000 caracteres.

En `PATCH`, los campos omitidos o enviados como `null` no cambian los valores guardados. El servicio combina los datos nuevos con el método existente y valida el estado final; por eso, cambiar `paymentType` o modificar datos de forma que falte un campo obligatorio provoca el rechazo de la actualización. Los campos específicos permanecen nullable en SQL porque no corresponden a todos los tipos de método de pago.

Reglas implementadas: el primer método se establece como predeterminado si todavía no existe uno; al marcar otro como predeterminado se desmarca el anterior; si se elimina el predeterminado y quedan métodos, se elige otro. Las consultas, actualizaciones y eliminaciones limitan el método al alojamiento del propietario autenticado.

## 4. Cloudinary y consistencia

La configuración usa las propiedades `cloudinary.cloud-name`, `cloudinary.api-key`, `cloudinary.api-secret` y `cloudinary.folder` (por defecto `hostflow`). Deben suministrarse mediante variables de entorno o configuración local; no se deben subir secretos al repositorio. Para crear una cuenta de desarrollo, configurar las variables y replicar la integración, consultar [`docs/cloudinary-setup.md`](cloudinary-setup.md). La carpeta configurada es el destino de las cargas, no una garantía de aislamiento de permisos.

- `POST /gallery/upload`: carga el archivo en Cloudinary y guarda en la base la URL segura y `cloudinary_public_id`.
- `POST /profile/logo`: carga primero el archivo nuevo, actualiza `logo_url` y `logo_public_id`, y coordina la limpieza del recurso anterior con la finalización de la transacción de base de datos.
- Si la transacción del cambio de logotipo confirma, se intenta borrar el logotipo anterior cuando hay un `logo_public_id` conocido. Si la transacción falla, se intenta borrar el recurso nuevo para evitar dejarlo huérfano.
- Si el logotipo anterior no tiene `logo_public_id`, se conserva por seguridad: no se intenta inferir el identificador desde una URL.
- Si la eliminación posterior del logotipo anterior falla, se registra una advertencia para limpieza posterior; el nuevo logotipo permanece guardado.
- `DELETE /gallery/{imageId}`: intenta confirmar primero la eliminación remota y después elimina el registro. Si Cloudinary no confirma, el registro se conserva.
- Una respuesta de Cloudinary con resultado `not found` al eliminar se considera éxito idempotente.

Las operaciones externas de Cloudinary y la transacción SQL no forman una transacción distribuida; la lógica descrita reduce el riesgo de pérdida de referencias, pero un fallo remoto durante la limpieza puede requerir intervención posterior.

## 5. Configuración local

Configuración relevante observada en el proyecto:

- Puerto por defecto: `8080`; prefijo de API: `/api/v1`.
- Perfil de desarrollo: `application-dev.properties` importa opcionalmente `./.env` y usa `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` con valores alternativos para desarrollo.
- Variables de Cloudinary: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`; `CLOUDINARY_FOLDER` es opcional y por defecto `hostflow`.
- También se requieren las variables de seguridad que la aplicación use en su entorno, como `JWT_SECRET` y `ADMIN_PASSWORD`. El correo SMTP se configura con `MAIL_USERNAME` y `MAIL_PASSWORD`.

## 6. Pruebas revisadas

Los reportes Surefire revisados registran las siguientes pruebas, con 1 prueba ejecutada, 0 fallos y 0 errores en cada reporte:

- `AccommodationProfileServiceTest`
- `AccommodationProfileRollbackIntegrationTest`
- `AccommodationProfileReplacementIntegrationTest`

Las pruebas de integración del reemplazo y rollback usan una configuración de pruebas con H2 y Cloudinary simulado. Son evidencia de los escenarios cubiertos en esas pruebas; no demuestran por sí solas que las migraciones se hayan aplicado sobre PostgreSQL real ni que Cloudinary remoto esté operativo.

## 7. Ubicación de la implementación

- Controladores: `src/main/java/com/hostflow/controller/AccommodationProfileController.java`, `AccommodationPaymentMethodController.java` y `AccommodationController.java`.
- Servicios: `src/main/java/com/hostflow/service/AccommodationProfileService.java`, `AccommodationPaymentMethodService.java` y `CloudinaryImageService.java`.
- Modelos: `src/main/java/com/hostflow/model/Accommodation.java`, `AccommodationImage.java` y `AccommodationPaymentMethod.java`.
- Migraciones: `src/main/resources/db/migration/V2__create_accommodation_profile_and_gallery.sql` hasta `V5__add_cloudinary_public_id_to_accommodations.sql`.
