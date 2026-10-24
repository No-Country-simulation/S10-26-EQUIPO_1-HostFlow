# Configuración de Cloudinary para el equipo HostFlow

Guía para que cada integrante del equipo pueda configurar Cloudinary en su entorno local y, si corresponde, preparar los permisos de carpeta necesarios para que el backend de HostFlow pueda **cargar y eliminar** imágenes.

---

## 1. Qué necesita cada desarrollador

1. Acceso a una cuenta de Cloudinary (propia o de desarrollo del equipo).
2. Los valores de **Cloud name**, **API Key** y **API Secret**.
3. El nombre de carpeta acordado: `hostflow`.

La aplicación lee las propiedades:

| Propiedad Spring | Variable de entorno | Valor por defecto |
|---|---|---|
| `cloudinary.cloud-name` | `CLOUDINARY_CLOUD_NAME` | — |
| `cloudinary.api-key` | `CLOUDINARY_API_KEY` | — |
| `cloudinary.api-secret` | `CLOUDINARY_API_SECRET` | — |
| `cloudinary.folder` | `CLOUDINARY_FOLDER` | `hostflow` |

`CloudinaryImageService` usa `cloudinary.folder` como destino de las cargas. Si no se define, el valor predeterminado es `hostflow`.

---

## 2. Obtener las credenciales

1. Inicia sesión en el [panel de Cloudinary](https://console.cloudinary.com/).
2. Crea una Api Key, con el rol de Media Library User, si quieres limitar acceso especifico a folders.
3. En el dashboard localiza **Cloud name**, **API Key** y **API Secret**.
4. Copia esos valores únicamente en tu archivo de entorno local (o en el gestor de secretos autorizado).

Para desarrollo local cada persona puede usar sus propias credenciales. Para entornos compartidos (QA, staging) conviene que el equipo administre una cuenta y sus credenciales de forma controlada.

---

## 3. Configurar el entorno local

En la raíz del backend crea el archivo `.env` y define:

```dotenv
CLOUDINARY_CLOUD_NAME=tu_cloud_name
CLOUDINARY_API_KEY=tu_api_key
CLOUDINARY_API_SECRET=tu_api_secret
CLOUDINARY_FOLDER=hostflow
```

En la configuración de Spring Boot del proyecto estas variables se mapean así:

```properties
cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME}
cloudinary.api-key=${CLOUDINARY_API_KEY}
cloudinary.api-secret=${CLOUDINARY_API_SECRET}
cloudinary.folder=${CLOUDINARY_FOLDER:hostflow}
```

---

## 4. Carpeta y permisos: una diferencia importante

- `CLOUDINARY_FOLDER=hostflow` solo indica **dónde** la aplicación pide guardar las imágenes.
- El nombre de la carpeta **no limita por sí solo** lo que una API Key/API Secret puede hacer.
- Si se necesita aislamiento real, revisa las opciones de seguridad y claves restringidas del plan de Cloudinary que uses. Si no se puede garantizar el alcance requerido, usa una cuenta separada de desarrollo.

### 4.1 Por qué hace falta el rol de carpeta adecuado

En las pruebas de integración se observó este error al acceder a funciones como:

```text
Request forbidden due to missing permissions (actions=["delete"])
```

- El rol de carpeta **Contributor** permitía cargar, pero **no** eliminar.
- Al asignar el rol de carpeta **Manager** (`cld::role::folder::manager`), la carga y la eliminación funcionaron correctamente.

---

## 5. Preparar la carpeta y los permisos (una sola vez por cuenta/clave)

Si el equipo comparte la misma cuenta de desarrollo y la misma API key de ejecución, **esta sección se hace una sola vez**. Cada desarrollador solo necesita configurar su `.env` local con las credenciales de ejecución ya preparadas.

Si cada persona usa su propia cuenta o su propia API key, cada uno debe completar estos pasos en su entorno.

### 5.1 Crear o identificar la carpeta

1. En la consola de Cloudinary, crea (o localiza) la carpeta `hostflow` dentro de *Home*.
2. Anota el **Cloud name** y la **API Key** que usará HostFlow en tiempo de ejecución.

### 5.2 Obtener el `external_id` de la carpeta

El nombre visible `hostflow` no es suficiente para las operaciones de permisos. Necesitas el `external_id`.

Con credenciales que tengan permiso para consultar la Admin API (rol Master Admin):

```bash
export CLOUDINARY_CLOUD_NAME="<cloud_name>"
export CLOUDINARY_API_KEY="<api_key_con_permisos_admin>"
export CLOUDINARY_API_SECRET="<api_secret_correspondiente>"

curl --user "$CLOUDINARY_API_KEY:$CLOUDINARY_API_SECRET" \
  "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/folders"
```

Busca la entrada cuyo nombre sea `hostflow` y copia su `external_id`.

> El valor encontrado en una prueba concreta no sirve para otras cuentas. Vuelve a consultar la API en tu entorno.

Documentación: [List folders](https://cloudinary.com/documentation/admin_api#folders).

### 5.3 Asignar el rol Manager a la API key de ejecución

La operación se hace con credenciales **administrativas** (no con las de ejecución de HostFlow, a menos que esas tengan permisos de administración).

La siguiente llamada es una plantilla basada en la documentación de Cloudinary. Comprueba siempre el esquema vigente antes de ejecutarla:

```bash
export ADMIN_API_KEY="<api_key_administrativa>"
export ADMIN_API_SECRET="<api_secret_administrativo>"
export TARGET_API_KEY="<api_key_de_ejecucion_de_hostflow>"
export FOLDER_EXTERNAL_ID="<external_id_de_hostflow>"
export CLOUDINARY_CLOUD_NAME="<cloud_name>"

curl --request POST \
  --user "$ADMIN_API_KEY:$ADMIN_API_SECRET" \
  --header "Content-Type: application/json" \
  --data '{
    "role_id": "cld::role::folder::manager",
    "operations": [{
      "operation": "add",
      "principal": {
        "type": "apiKey",
        "id": "'"$TARGET_API_KEY"'"
      }
    }]
  }' \
  "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/folder_operations/invite/$FOLDER_EXTERNAL_ID"
```

Para verificar la asignación:

```bash
curl --user "$ADMIN_API_KEY:$ADMIN_API_SECRET" \
  "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/folder_operations/invite/$FOLDER_EXTERNAL_ID"
```

Documentación: [Folder operations](https://cloudinary.com/documentation/admin_api#folder_operations) y [Roles y permisos](https://cloudinary.com/documentation/dam_admin_permissions).

---

## 6. Comprobar la integración

1. Arranca el backend con el perfil de desarrollo y confirma que la configuración de Cloudinary se carga sin errores.
2. Con un usuario `OWNER` autenticado, prueba la carga de una imagen de galería:
   ```http
   POST /api/v1/owners/accommodation/gallery/upload
   ```
3. Confirma que la respuesta contiene la URL HTTPS y que el registro de galería guarda el `cloudinary_public_id`.
4. Prueba el reemplazo del logotipo:
   ```http
   POST /api/v1/owners/accommodation/profile/logo
   ```
5. Verifica en el panel de Cloudinary que los recursos aparecen en la carpeta configurada.
6. Prueba la eliminación de una imagen cargada por HostFlow y confirma que el recurso se elimina en Cloudinary.

Usa siempre recursos de prueba, nunca imágenes importantes de producción.

---

## 7. Si algo falla

| Síntoma | Qué revisar |
|---|---|
| Error genérico de carga (`CloudinaryUploadException`) | Cloud name, API key/secret, rol global y permisos de carpeta. Revisa la causa original de Cloudinary en el log. |
| Carga funciona, eliminación falla con `missing permissions (actions=["delete"])` | El rol de carpeta es insuficiente (p. ej. Contributor). Asigna Manager o el rol que cubra `delete`. |
| No se encuentra la carpeta | Estás usando el nombre visible en lugar del `external_id`. Consulta `/folders`. |
| Las imágenes aparecen en otra carpeta | `CLOUDINARY_FOLDER` o la propiedad `cloudinary.folder` no coinciden con el destino esperado. |
| Credenciales no se cargan | El archivo `.env` no está en la ruta desde la que se ejecuta la app, o el perfil activo no lo importa. Revisa espacios o comillas en los valores. |

---

## 8. Seguridad – recordatorios

- Separa las credenciales administrativas (usadas solo para asignar permisos) de las credenciales de ejecución de HostFlow.
- Antes de producción, revisa la matriz de roles de Cloudinary y los límites del plan (almacenamiento, transformaciones, tráfico).
- La interfaz y el modelo de permisos de Cloudinary pueden cambiar; contrasta siempre este procedimiento con la documentación oficial actual.

### Referencias oficiales

- [Encontrar credenciales](https://cloudinary.com/documentation/developer_onboarding_faq_find_credentials)
- [Roles y permisos granulares](https://cloudinary.com/documentation/dam_admin_permissions)
- [Admin API – carpetas](https://cloudinary.com/documentation/admin_api#folders)
- [Admin API – operaciones de carpeta](https://cloudinary.com/documentation/admin_api#folder_operations)
