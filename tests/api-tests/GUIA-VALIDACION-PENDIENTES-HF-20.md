# Guía de validación — Pendientes abiertos del merge HF-20

> Merge revisado: `5482c23` (`feat/HF-20-accommodation-profile` → `QA-review`)
> Entorno de referencia: Windows + Git Bash + PostgreSQL 18.
> Fecha de la última verificación: 2026-11-01.
>
> **Cómo usar esta guía:** ejecutar los pasos en orden, marcar `- [x]` solo cuando el
> resultado esté comprobado y guardar la evidencia (captura/log) en
> `tests/api-tests/evidencias/`.
>
> Algunos pasos de hoy **fallan a propósito**: son los pendientes que hay que
> demostrar. En esos casos, tachar el ítem de "verificado" cuando el fallo quede
> documentado, y tachar el ítem de "corregido" solo cuando el fix esté aplicado y
> el test vuelva a pasar.

---

## 0. Resumen de pendientes

| ID | Pendiente | Tipo | Bloquea `development` | Bloquea `main`/prod |
|---|---|---|---|---|
| P1 | Documentación desalineada con el código | Docs | No | Sí |
| P2 | Migraciones V2–V7 sin validar en PostgreSQL real | Datos/DB | No (pero avisar) | Sí |
| P3 | Arranque frágil sin `.env` | Config | No (si se usa perfil `dev`) | Sí (CI/clon limpio) |
| P4 | Sin tests HTTP de 401/403 y multipart | Tests | No | No (deuda) |
| P5 | `POST` multipart sin cuerpo responde `500` en vez de `415` | Bug menor API | No | No (deuda) |

Resultado de la verificación inicial (2026-11-01):

- P1: confirmado. `POST /gallery` no existe; docs dicen 5 MB y el código exige 500 KB–2 MB.
- P2: **confirmado y agravado**. Flyway falla tanto en base limpia (V2) como en base creada por Hibernate (V3). Ver sección 3.
- P3: confirmado por configuración (`${CLOUDINARY_*}` sin default en `application.properties`).
- P4: confirmado. Solo hay tests de servicio; no pasan por seguridad ni por multipart.
- P5: confirmado con `curl`: `POST /gallery/upload` sin body devuelve `500 INTERNAL_ERROR`.

---

## 1. Prerrequisitos comunes

### 1.1 Software

- [ ] JDK 17 (`java -version`).
- [ ] Backend compilando (`cd backend && ./mvnw -q -DskipTests compile`).
- [ ] PostgreSQL corriendo en `localhost:5432`.
- [ ] `curl` disponible (Git Bash).
- [ ] `psql` disponible. En este equipo está en:

```bash
export PATH="/c/Program Files/PostgreSQL/18/bin:$PATH"
psql --version
```

Si no se usa `psql`, hacer las mismas operaciones con pgAdmin.

### 1.2 Variables locales

El backend lee `backend/.env`. Para no exponer secretos en la terminal, cargarlo en
una subshell solo cuando se necesite:

```bash
( set -a; source backend/.env; set +a; echo "DB_USERNAME configurado: ${DB_USERNAME:+si}" )
```

> No pegar valores de `.env` en capturas ni en el `.md`.

### 1.3 Estado actual del backend de desarrollo

Debe estar corriendo para P1 y P4:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Verificar:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/swagger-ui.html
# Esperado: 302 (redirige) o 200 al seguir redirección con -L
```

Para P2 hay que detenerlo o usar otro puerto (`8081`), porque el perfil `prod`
levanta otra instancia.

---

## 2. P1 — Documentación desalineada con el código

### 2.1 Objetivo

Demostrar que `backend/docs/gestion-alojamiento.md` describe endpoints y límites que
no existen en el código, y corregir la documentación.

### 2.2 Verificar `POST /gallery` (URL externa) inexistente

- [ ] Listar los mappings reales del controlador:

```bash
grep -nE "@(Get|Post|Put|Patch|Delete)Mapping" \
  backend/src/main/java/com/hostflow/controller/AccommodationProfileController.java
```

Resultado esperado: aparecen `/profile`, `/gallery`, `/gallery/upload`,
`/profile/logo` y `/gallery/{imageId}`, pero **no** `POST /gallery`.

- [ ] Confirmar que el DTO quedó sin uso:

```bash
grep -rn "AccommodationImageRequest" backend/src/main/java
```

Resultado esperado: solo la declaración del record en
`backend/src/main/java/com/hostflow/dto/request/AccommodationImageRequest.java`.

- [ ] Abrir Swagger y comprobar que no está el endpoint:

```text
http://localhost:8080/api/v1/swagger-ui.html
```

Buscar `POST /owners/accommodation/gallery` (sin `/upload`). No debe existir.

- [ ] Confirmar que la doc sí lo promete:

```bash
grep -n "POST.*gallery\|/gallery\`\|URL externa\|URL HTTPS" backend/docs/gestion-alojamiento.md
```

Resultado esperado: fila `POST /gallery` y bloque "Para añadir una URL externa"
(~líneas 106 y 155-165).

### 2.3 Verificar el límite de archivo (5 MB vs 2 MB)

- [ ] Leer el límite real del servicio:

```bash
grep -n "MIN_IMAGE_SIZE_BYTES\|MAX_IMAGE_SIZE_BYTES" \
  backend/src/main/java/com/hostflow/service/CloudinaryImageService.java
```

Resultado esperado: `500_000L` (500 KB) y `2_000_000L` (2 MB).

- [ ] Leer el límite real de multipart:

```bash
grep -n "max-file-size\|max-request-size" \
  backend/src/main/resources/application.properties
```

Resultado esperado: `2000000B` por archivo y `3000000B` por request.

- [ ] Confirmar que la doc dice otra cosa:

```bash
grep -n "MB\|500 KB" backend/docs/gestion-alojamiento.md
```

Resultado esperado: la línea ~173 indica "no superar 5 MB" y "límites ... 5 MB".

### 2.4 Verificar referencia a `AccommodationController` inexistente

- [ ] Buscar el controlador de desactivación temporal mencionado en la doc:

```bash
ls backend/src/main/java/com/hostflow/controller
grep -rn "AccommodationController\|temporary\|desactivaci" \
  backend/src/main/java/com/hostflow/controller backend/src/main/java/com/hostflow/model
```

Resultado esperado: **no existe** `AccommodationController`; la doc lo menciona en
las líneas 14, 16 y 223.

### 2.5 Corregir la documentación (opción recomendada)

- [ ] En `backend/docs/gestion-alojamiento.md`:
  - Quitar la fila `POST /gallery` de la tabla de endpoints (~línea 106).
  - Quitar el bloque "Para añadir una URL externa" (~líneas 155-165).
  - Cambiar "5 MB" por "2 MB" y aclarar el mínimo de 500 KB (~línea 173).
  - Quitar o marcar como "fuera de este merge" las referencias a
    `AccommodationController` / desactivación temporal (líneas 14, 16 y 223).
- [ ] Decidir con el equipo qué hacer con `AccommodationImageRequest`:
  - Implementar `POST /gallery` (URL externa), o
  - Eliminar el DTO sin uso.
- [ ] Revisar que el Swagger y la doc coincidan después del cambio.

### 2.6 Criterio de cierre P1

- [ ] Doc corregida o endpoint implementado.
- [ ] No quedan referencias a endpoints inexistentes.
- [ ] Límite documentado = 500 KB–2 MB (o el que se acuerde y se implemente).

---

## 3. P2 — Validar migraciones V2–V7 en PostgreSQL real

### 3.1 Objetivo

Comprobar si las migraciones Flyway pueden crear/actualizar una base real y si
Hibernate (`ddl-auto=validate`) acepta el esquema resultante.

> **Hallazgo verificado:** hoy las migraciones **no** funcionan ni en base limpia ni
> sobre una base creada por la versión actual de Hibernate. Los pasos siguientes
> reproducen ambos fallos. Son evidencia para bloquear el paso a `main`/prod.

### 3.2 Preparar conexión y bases de prueba

```bash
export PATH="/c/Program Files/PostgreSQL/18/bin:$PATH"

# Cargar credenciales locales sin mostrarlas
( set -a; source backend/.env; set +a
  export PGPASSWORD="$DB_PASSWORD"
  PSQL=psql

  # Base A: vacía (simula despliegue nuevo)
  "$PSQL" -U "$DB_USERNAME" -h localhost -d postgres -c "DROP DATABASE IF EXISTS hostflow_migration_check;"
  "$PSQL" -U "$DB_USERNAME" -h localhost -d postgres -c "CREATE DATABASE hostflow_migration_check;"
)
```

- [ ] Base `hostflow_migration_check` creada.
- [ ] Sin tablas de negocio (solo estará vacía).

### 3.3 Test A — Base limpia (despliegue nuevo)

Detener el backend de `dev` o usar el puerto `8081`:

```bash
cd backend

( set -a; source .env; set +a
  export DB_URL="jdbc:postgresql://localhost:5432/hostflow_migration_check"
  timeout 180 ./mvnw -q spring-boot:run \
    -Dspring-boot.run.profiles=prod \
    -Dspring-boot.run.arguments=--server.port=8081 \
    > /tmp/mig-fresh.log 2>&1
  echo "exit=$?"
)

grep -iE "Flyway|Migration of schema|SQL State|does not exist|no existe|Schema-validation|Started Hostflow|APPLICATION FAILED" /tmp/mig-fresh.log | head -40
```

Resultado esperado **hoy** (fallo):

```text
Migration of schema "public" to version "2 - create accommodation profile and gallery" failed!
SQL State  : 42P01          (undefined_table)
```

- [ ] Fallo reproducido y log guardado.
- [ ] Causa identificada: las migraciones **no crean** `owners`, `roles` ni
      `owner_roles`. `V1` solo crea `password_reset_tokens` y `V2` referencia
      `owners(id)` con FK.
- [ ] Confirmar tablas creadas tras el fallo:

```bash
( set -a; source backend/.env; set +a; export PGPASSWORD="$DB_PASSWORD"
  psql -U "$DB_USERNAME" -h localhost -d hostflow_migration_check \
    -tAc "select tablename from pg_tables where schemaname='public' order by 1;"
)
```

Resultado esperado: solo `flyway_schema_history` y `password_reset_tokens`.

### 3.4 Test B — Base creada por Hibernate (upgrade realista)

Simula una base de desarrollo existente que se quiere "pasar a Flyway".

```bash
( set -a; source backend/.env; set +a
  export PGPASSWORD="$DB_PASSWORD"
  psql -U "$DB_USERNAME" -h localhost -d postgres -c "DROP DATABASE IF EXISTS hostflow_migration_upgrade;"
  psql -U "$DB_USERNAME" -h localhost -d postgres -c "CREATE DATABASE hostflow_migration_upgrade;"
  pg_dump -U "$DB_USERNAME" -h localhost -d hostflow_dev --no-owner --no-privileges \
    | psql -U "$DB_USERNAME" -h localhost -d hostflow_migration_upgrade
)
```

> Si `hostflow_dev` no existe o está vacía, crear primero el esquema corriendo el
> backend con perfil `dev` al menos una vez.

Luego ejecutar `prod` contra esa base:

```bash
cd backend

( set -a; source .env; set +a
  export DB_URL="jdbc:postgresql://localhost:5432/hostflow_migration_upgrade"
  timeout 180 ./mvnw -q spring-boot:run \
    -Dspring-boot.run.profiles=prod \
    -Dspring-boot.run.arguments=--server.port=8081 \
    > /tmp/mig-upgrade.log 2>&1
  echo "exit=$?"
)

grep -iE "Baselining|baselined|Successfully applied|Migration of schema|failed!|SQL State|no existe|Schema-validation|Started Hostflow|APPLICATION FAILED" /tmp/mig-upgrade.log | head -40
```

Resultado esperado **hoy** (fallo):

```text
Successfully baselined schema with version: 1
Migration of schema "public" to version "3 - separate accommodation payment methods" failed!
SQL State  : 42703          (undefined_column)
Message    : ERROR: no existe la columna a.bank_name
```

- [ ] Fallo reproducido y log guardado.
- [ ] Causa identificada: `V3` hace `INSERT ... SELECT a.bank_name, ...` asumiendo
      columnas `bank_*` que la entidad actual ya no crea. `V2` solo las incluye
      dentro del `CREATE TABLE IF NOT EXISTS`, que no se ejecuta si la tabla ya
      existe.

Comprobar que la base de desarrollo actual **no** tiene esas columnas:

```bash
( set -a; source backend/.env; set +a; export PGPASSWORD="$DB_PASSWORD"
  psql -U "$DB_USERNAME" -h localhost -d hostflow_dev \
    -tAc "select column_name from information_schema.columns where table_name='accommodations' order by ordinal_position;"
)
```

Resultado esperado: no aparece `bank_name` ni `bank_account_holder`, etc.

### 3.5 Qué corregir antes de reintentar

- [ ] **Base limpia:** agregar las tablas base que faltan (`owners`, `roles`,
      `owner_roles`) en una migración previa/anterior a `V2`, o documentar que
      Flyway no soporta bootstrap de base nueva y usar otra estrategia.
- [ ] **Upgrade:** hacer `V3` tolerante a la ausencia de `bank_*`. Opciones:
  - Añadir en `V2` los `ALTER TABLE accommodations ADD COLUMN IF NOT EXISTS bank_*`
    antes de que `V3` los consulte (aunque luego `V3` los elimine), o
  - Envolver el `INSERT ... SELECT` de `V3` en un bloque que verifique
    `information_schema.columns` antes de ejecutarse.
- [ ] Volver a ejecutar Test A y Test B hasta que:
  - [ ] Flyway aplique las 7 migraciones sin errores.
  - [ ] Hibernate `validate` arranque sin `Schema-validation: missing ...`.
  - [ ] `flyway_schema_history` tenga `success = true` en todas las filas.
- [ ] Verificar columnas finales:

```bash
( set -a; source backend/.env; set +a; export PGPASSWORD="$DB_PASSWORD"
  psql -U "$DB_USERNAME" -h localhost -d hostflow_migration_check -c "\d accommodations"
  psql -U "$DB_USERNAME" -h localhost -d hostflow_migration_check -c "\d accommodation_images"
  psql -U "$DB_USERNAME" -h localhost -d hostflow_migration_check -c "\d accommodation_payment_methods"
)
```

Comprobar que existan, entre otras:

- `accommodations`: `logo_public_id`, `primary_color`, `accent_color`,
  `tertiary_color`, `background_pattern`.
- `accommodation_images`: `cloudinary_public_id`.
- `accommodation_payment_methods`: `account_type`, `user_identifier`,
  `wallet_phone`, `wallet_email`, índice único parcial de `is_default`.
- [ ] Historial Flyway:

```bash
( set -a; source backend/.env; set +a; export PGPASSWORD="$DB_PASSWORD"
  psql -U "$DB_USERNAME" -h localhost -d hostflow_migration_check \
    -c "select installed_rank, version, description, success from flyway_schema_history order by installed_rank;"
)
```

### 3.6 Limpieza

```bash
( set -a; source backend/.env; set +a; export PGPASSWORD="$DB_PASSWORD"
  psql -U "$DB_USERNAME" -h localhost -d postgres -c "DROP DATABASE IF EXISTS hostflow_migration_check;"
  psql -U "$DB_USERNAME" -h localhost -d postgres -c "DROP DATABASE IF EXISTS hostflow_migration_upgrade;"
)
```

- [ ] Bases de prueba eliminadas.

### 3.7 Criterio de cierre P2

- [ ] Test A (base limpia) pasa y arranca la app.
- [ ] Test B (base Hibernate existente) pasa y arranca la app.
- [ ] Evidencia de ambos logs guardada.

---

## 4. P3 — Arranque sin `.env`

### 4.1 Objetivo

Demostrar que un clon limpio (sin `backend/.env` y sin variables de entorno) no
arranca, y decidir/corregir la estrategia.

### 4.2 Reproducir el fallo

Detener el backend que esté usando `backend/.env` y hacer un respaldo temporal:

```bash
mv backend/.env backend/.env.bak
```

Abrir una terminal **sin** las variables de Cloudinary/Admin exportadas y ejecutar
sin perfil (usa `application.properties` base, donde no hay valores por defecto):

```bash
cd backend
timeout 120 ./mvnw -q spring-boot:run > /tmp/boot-noenv.log 2>&1
echo "exit=$?"
grep -iE "Could not resolve placeholder|placeholder|APPLICATION FAILED|BUILD FAILURE" \
  /tmp/boot-noenv.log | head -30
```

Resultado esperado: falla al resolver algún placeholder, por ejemplo:

```text
Could not resolve placeholder 'CLOUDINARY_CLOUD_NAME'
```

o `ADMIN_PASSWORD` / `JWT_SECRET`.

- [ ] Fallo reproducido.
- [ ] Anotado cuál fue el primer placeholder faltante.

Repetir también a nivel de tests:

```bash
cd backend
timeout 300 ./mvnw -q test > /tmp/test-noenv.log 2>&1
echo "exit=$?"
grep -iE "Could not resolve placeholder|APPLICATION FAILED|BUILD FAILURE|Tests run" \
  /tmp/test-noenv.log | head -30
```

- [ ] Comportamiento de los tests sin `.env` documentado.

### 4.3 Decidir la corrección

Opción A (recomendada para no romper CI ni clones limpios): dar defaults vacíos en
`application.properties`, igual que ya hace el perfil `dev`.

```properties
cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:}
cloudinary.api-key=${CLOUDINARY_API_KEY:}
cloudinary.api-secret=${CLOUDINARY_API_SECRET:}
```

Opción B (si se quiere obligar la configuración): mantener los placeholders sin
default, documentar el requisito y validar al arrancar con un mensaje claro; además,
configurar las variables en CI.

- [ ] Decisión tomada y registrada.
- [ ] Si es Opción A, cambio aplicado en `backend/src/main/resources/application.properties`.

### 4.4 Verificar la corrección

Con `.env` aún renombrado y sin variables exportadas:

```bash
cd backend
timeout 120 ./mvnw -q spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/boot-fixed.log 2>&1
echo "exit=$?"
grep -iE "Started Hostflow|APPLICATION FAILED|Could not resolve placeholder" \
  /tmp/boot-fixed.log | head -20
```

Resultado esperado (si Opción A): arranca y, al subir imágenes, responde `502`
por falta de credenciales, sin impedir el resto de endpoints.

- [ ] Arranca sin `.env`.
- [ ] `GET /owners/accommodation/profile` con token funciona.
- [ ] `POST /gallery/upload` responde `502 CLOUDINARY_UPLOAD_FAILED` (no `500`).

Restaurar el archivo:

```bash
mv backend/.env.bak backend/.env
```

- [ ] `.env` restaurado.

### 4.5 Criterio de cierre P3

- [ ] Un entorno sin `.env` arranca o falla con un mensaje claro y documentado.
- [ ] Los tests no dependen de un `.env` local no versionado.

---

## 5. P4 — Tests HTTP de 401/403 y validación multipart

### 5.1 Objetivo

Cubrir a nivel HTTP lo que hoy solo está probado a nivel de servicio.

### 5.2 Comportamiento actual verificado (importante)

- [ ] Sin token o con token inválido, los endpoints protegidos responden **403**,
      no 401, porque `SecurityConfig` no define un `AuthenticationEntryPoint`.

```bash
curl -s -o /tmp/noauth.txt -w "sin token HTTP %{http_code}\n" \
  http://localhost:8080/api/v1/owners/accommodation/profile
cat /tmp/noauth.txt; echo

curl -s -o /tmp/badtoken.txt -w "token inválido HTTP %{http_code}\n" \
  -H "Authorization: Bearer abc.def.ghi" \
  http://localhost:8080/api/v1/owners/accommodation/profile
cat /tmp/badtoken.txt; echo
```

Resultado esperado: `403` en ambos casos.

- [ ] Confirmado: no hay `401` real.
- [ ] Decidir si se deja así (guardar test como 403) o se configura un entry point
      para devolver `401`. Si se cambia, actualizar el test.

### 5.3 Sobre el 403 con rol distinto de OWNER

`DataInitializer` asigna al admin **ADMIN + OWNER** (línea ~147), por lo que usar el
token de admin **no** produce 403. Para probar 403 hay dos opciones:

Opción recomendada: test automatizado con `@WithMockUser(roles = "ADMIN")`. Ver 5.5.

Opción manual (base de datos): crear un usuario solo con `ROLE_ADMIN` copiando el
hash del admin y luego hacer login. Solo en entorno local:

```sql
INSERT INTO owners (full_name, accommodation_name, email, password, enabled, terms_accepted, created_at, updated_at)
SELECT full_name, 'QA Admin', 'qa.admin@example.com', password, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM owners WHERE email = 'admin@hostflow.com';

INSERT INTO owner_roles (owner_id, role_id)
SELECT (SELECT id FROM owners WHERE email = 'qa.admin@example.com'),
       (SELECT id FROM roles WHERE role_name = 'ROLE_ADMIN');
```

Luego `POST /auth/login` con `qa.admin@example.com` y `ADMIN_PASSWORD` del `.env`.
Con ese token, cualquier endpoint del módulo debe dar `403`.

- [ ] 403 con rol distinto de OWNER verificado.
- [ ] Usuario de prueba eliminado después.

### 5.4 Validación multipart — casos manuales

Preparar archivos (Git Bash). Usar rutas relativas: `curl` de Windows puede fallar
con `;type=` y rutas `/tmp` en `-F`.

```bash
# Archivo < 500 KB (falla por tamaño)
head -c 100000 /dev/urandom > ./qa-small.jpg

# Archivo > 2 MB (lo rechaza el límite de Spring)
head -c 2500000 /dev/urandom > ./qa-big.jpg

# Archivo > 500 KB pero NO imagen (falla por Content-Type)
head -c 600000 /dev/urandom > ./qa-600kb.txt

# Archivo pequeño .txt (falla primero por tamaño, no por tipo)
echo "no soy imagen" > ./qa.txt
```

Obtener token OWNER:

```bash
EMAIL="qa.multipart.$(date +%s)@example.com"
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"fullName\":\"QA Multipart\",\"accommodationName\":\"QA Multipart\",\"email\":\"$EMAIL\",\"password\":\"Password123\",\"termsAccepted\":true}" \
  -o /tmp/qa-reg.json -w "register HTTP %{http_code}\n"
TOKEN=$(grep -oE '"token":"[^"]+"' /tmp/qa-reg.json | head -1 | sed 's/"token":"//;s/"//')
echo "token_len=${#TOKEN}"

# Crear perfil (necesario para llegar a la validación de imagen)
curl -s -X POST http://localhost:8080/api/v1/owners/accommodation/profile \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"QA Multipart"}' -o /dev/null -w "profile HTTP %{http_code}\n"
```

Ejecutar cada caso:

| Caso | Comando | Esperado |
|---|---|---|
| Sin token | `curl -X POST .../gallery/upload` sin header | `403` |
| Sin parte `file` | `curl -X POST .../gallery/upload -H "Authorization: Bearer $TOKEN" -F "other=@./qa.txt"` | `400 INVALID_IMAGE` |
| Sin cuerpo/JSON | `curl -X POST .../gallery/upload -H "Authorization: Bearer $TOKEN"` | **Hoy `500 INTERNAL_ERROR`** (debería ser `415`) |
| Archivo < 500 KB | `-F "file=@./qa-small.jpg"` | `400 INVALID_IMAGE` |
| Archivo > 2 MB | `-F "file=@./qa-big.jpg"` | `400 FILE_TOO_LARGE` |
| `.txt` > 500 KB | `-F "file=@./qa-600kb.txt"` | `400 INVALID_IMAGE` ("debe ser una imagen") |
| `.txt` pequeño | `-F "file=@./qa.txt"` | `400 INVALID_IMAGE` ("al menos 500 KB") |
| Imagen válida 500 KB–2 MB | usar una foto real | `201` si Cloudinary está configurado; `502` si no |

Comandos de ejemplo:

```bash
BASE=http://localhost:8080/api/v1/owners/accommodation

echo "--- sin parte file ---"
curl -s -X POST "$BASE/gallery/upload" -H "Authorization: Bearer $TOKEN" \
  -F "other=@./qa.txt" -o /tmp/m1.txt -w "HTTP %{http_code}\n"; cat /tmp/m1.txt; echo

echo "--- sin cuerpo ---"
curl -s -X POST "$BASE/gallery/upload" -H "Authorization: Bearer $TOKEN" \
  -o /tmp/m2.txt -w "HTTP %{http_code}\n"; cat /tmp/m2.txt; echo

echo "--- < 500 KB ---"
curl -s -X POST "$BASE/gallery/upload" -H "Authorization: Bearer $TOKEN" \
  -F "file=@./qa-small.jpg" -o /tmp/m3.txt -w "HTTP %{http_code}\n"; cat /tmp/m3.txt; echo

echo "--- > 2 MB ---"
curl -s -X POST "$BASE/gallery/upload" -H "Authorization: Bearer $TOKEN" \
  -F "file=@./qa-big.jpg" -o /tmp/m4.txt -w "HTTP %{http_code}\n"; cat /tmp/m4.txt; echo

echo "--- .txt > 500 KB ---"
curl -s -X POST "$BASE/gallery/upload" -H "Authorization: Bearer $TOKEN" \
  -F "file=@./qa-600kb.txt" -o /tmp/m5.txt -w "HTTP %{http_code}\n"; cat /tmp/m5.txt; echo
```

Repetir para `POST /profile/logo` (mismos límites y tipo).

- [ ] Todos los casos ejecutados y con respuesta/estado anotado.
- [ ] Caso "sin cuerpo" registrado como hallazgo P5 (`500` en vez de `415`).

Limpiar archivos y datos de prueba:

```bash
rm -f qa-small.jpg qa-big.jpg qa-600kb.txt qa.txt
```

- [ ] Archivos temporales eliminados.
- [ ] Owner de prueba eliminado (si no se quiere conservar).

### 5.5 Tests automatizados sugeridos (MockMvc)

Agregar una clase, por ejemplo
`backend/src/test/java/com/hostflow/controller/AccommodationProfileControllerSecurityTest.java`
(la dependencia `spring-security-test` ya está en `pom.xml`):

```java
package com.hostflow.controller;

import com.hostflow.service.CloudinaryImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccommodationProfileControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CloudinaryImageService cloudinaryImageService;

    @Test
    void sinToken_devuelveProhibido() throws Exception {
        mockMvc.perform(get("/owners/accommodation/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rolDistintoDeOwner_devuelveProhibido() throws Exception {
        mockMvc.perform(get("/owners/accommodation/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void multipartSinParteFile_devuelveBadRequest() throws Exception {
        mockMvc.perform(multipart("/owners/accommodation/gallery/upload"))
                .andExpect(status().isBadRequest());
    }
}
```

Notas:

- Los tests de 401/403 no llegan al controlador, por eso no necesitan datos.
- Para probar la validación de tamaño/tipo a nivel HTTP con un OWNER real, extender
  `AccommodationProfileServiceTest` o crear un test de integración que persista un
  `Owner` y su `Accommodation`, y usar `MockMultipartFile`.
- Si se decide devolver `401` en vez de `403`, cambiar `isForbidden()` por
  `isUnauthorized()` y configurar el `AuthenticationEntryPoint`.

Ejecutar:

```bash
cd backend
./mvnw -Dtest=AccommodationProfileControllerSecurityTest test
```

- [ ] Tests agregados.
- [ ] Pasan en local.
- [ ] `mvnw test` completo sigue en verde.

### 5.6 Criterio de cierre P4

- [ ] Existe al menos un test que cubre 401/403 de los endpoints del módulo.
- [ ] Existe al menos un test que cubre archivo ausente / tamaño / tipo en multipart.
- [ ] Se documenta si el contrato usa 401 o 403 para no autenticado.

---

## 6. P5 — `POST` multipart sin cuerpo devuelve `500`

### 6.1 Reproducir

Con el backend en `dev`:

```bash
curl -s -X POST http://localhost:8080/api/v1/owners/accommodation/gallery/upload \
  -H "Authorization: Bearer $TOKEN" \
  -o /tmp/p5.txt -w "HTTP %{http_code}\n"
cat /tmp/p5.txt; echo
```

Resultado actual: `500` con `errorCode: INTERNAL_ERROR`.

- [ ] Reproducido.

### 6.2 Corregir

El `GlobalExceptionHandler` atrapa `HttpMediaTypeNotSupportedException` con el
handler genérico `Exception`. Agregar un handler específico que devuelva
`415 Unsupported Media Type` (o `400`) con un `errorCode` claro.

- [ ] Handler agregado.
- [ ] Re-test: `POST` sin cuerpo responde `415`/`400`, no `500`.
- [ ] Test automatizado agregado.

---

## 7. Orden sugerido de ejecución

1. P1 (docs): no requiere reiniciar nada.
2. P4/P5 (HTTP/multipart): con backend `dev`.
3. P3 (sin `.env`): requiere renombrar `.env` y reiniciar.
4. P2 (migraciones): al final, porque usa otro puerto/base y conviene hacerlo con el
   backend detenido.

---

## 8. Checklist maestro

### Documentación (P1)

- [ ] `POST /gallery` confirmado como inexistente.
- [ ] `AccommodationImageRequest` sin uso confirmado.
- [ ] Límite 5 MB vs 2 MB confirmado.
- [ ] Referencias a `AccommodationController` inexistente confirmadas.
- [ ] Documentación corregida o endpoint implementado.

### Migraciones (P2)

- [ ] Test A (base limpia) ejecutado; fallo V2 `42P01` documentado.
- [ ] Test B (base Hibernate) ejecutado; fallo V3 `42703` documentado.
- [ ] Causa raíz identificada (`owners`/`roles`/`owner_roles` ausentes; `bank_*` ausentes).
- [ ] Correcciones aplicadas.
- [ ] Test A y Test B en verde tras el fix.
- [ ] `ddl-auto=validate` arranca con el esquema migrado.

### Arranque sin `.env` (P3)

- [ ] Fallo reproducido sin `.env`.
- [ ] Decisión de configuración tomada.
- [ ] Fix aplicado (si corresponde).
- [ ] Arranque sin `.env` verificado.
- [ ] Tests sin `.env` verificados.

### Tests HTTP (P4)

- [ ] Sin token / token inválido verificado (`403` actual).
- [ ] 403 con rol distinto de OWNER verificado.
- [ ] Casos multipart manuales verificados.
- [ ] Tests MockMvc agregados y pasando.
- [ ] Contrato 401 vs 403 documentado.

### Bug multipart sin cuerpo (P5)

- [ ] `500` reproducido.
- [ ] Handler `415` agregado.
- [ ] Re-test en verde.

---

## 9. Evidencias

Guardar en `tests/api-tests/evidencias/`:

- [ ] `p1-docs-grep.txt` — salida de los greps de P1.
- [ ] `p2-fresh.log` — log del Test A.
- [ ] `p2-upgrade.log` — log del Test B.
- [ ] `p3-boot-sin-env.log` — log del fallo y del fix.
- [ ] `p4-multipart.md` — tabla de casos con estados HTTP obtenidos.
- [ ] `p5-sin-cuerpo.txt` — respuesta actual y posterior al fix.
- [ ] Capturas de Swagger/Postman si aplica.
