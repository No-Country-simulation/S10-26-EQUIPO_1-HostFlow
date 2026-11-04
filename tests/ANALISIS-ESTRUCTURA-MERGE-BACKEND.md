# Análisis de estructura — Merge BACKEND `d18dd48`

**Rama:** `review-rama-back-1`
**Merge analizado:** `d18dd48` — *Merge branch 'development' ... into feat/backend-sistema-de-reservas-hostflow*
**Padres:** `5a06cb8` (rama de reservas) + `093e86d` (`development` / HF-20)
**Alcance backend:** controllers, services, DTOs, models, repositorios, migraciones Flyway,
configuración y tests.

---

## Resumen ejecutivo

A diferencia del merge de frontend, **este merge rompe el backend a nivel estructural y de
compilación**. La verificación con `./mvnw -DskipTests compile` falla:

- **4 archivos fuente no compilan** por resolución de merge defectuosa.
- **La app no puede arrancar** por versiones duplicadas de migraciones Flyway.
- Además hay violaciones de capas, clases monolito y configuración de perfiles
  inconsistente que ya venían arrastrándose y que el merge expone.

| # | Tipo de error | ¿Introducido por el merge? | Severidad |
|---|---|---|---|
| 1 | Conflictos de merge sin resolver / código truncado y duplicado | Sí | **Bloqueante** |
| 2 | Migraciones Flyway con versión duplicada (`V6`, `V7`) | Sí | **Bloqueante** |
| 3 | Violación de capas (controller → repository, `@Transactional` en controller, entidades expuestas) | Mixto | Alta |
| 4 | Clases monolito / falta de separación de responsabilidades | No (pre-existente) | Alta |
| 5 | Estructura de paquetes por capa plana (sin módulos de dominio) | No (pre-existente) | Media |
| 6 | Tests mal ubicados / cobertura insuficiente | No (pre-existente) | Media |
| 7 | Configuración duplicada y desalineada entre perfiles | Mixto | Media |
| 8 | Controladores solapados, código muerto y nombrado ambiguo | No (pre-existente) | Baja |

---

## 1. Conflictos de merge sin resolver / código corrupto (BLOQUEANTE)

`./mvnw -DskipTests compile` falla en **4 archivos**:

```
backend/src/main/java/com/hostflow/controller/AccommodationProfileController.java
backend/src/main/java/com/hostflow/service/AccommodationPaymentMethodService.java
backend/src/main/java/com/hostflow/service/AccommodationProfileService.java
backend/src/main/java/com/hostflow/service/CloudinaryImageService.java
```

### 1.1 Marcadores de conflicto commiteados

`AccommodationProfileController.java:3-6`:

```
<<<<<<< HEAD
import com.hostflow.dto.request.AccommodationImageRequest;
=======
>>>>>>> 093e86dea5265b3004d369880a4959870dec2e5c
```

Error de compilación: `class, interface, enum, or record expected` (líneas 3 y 5).

### 1.2 Métodos truncados (llaves sin cerrar)

- `AccommodationProfileService.java:166-172` — el método `addImage(...)` no cierra su
  bloque; `ensureGalleryHasSpace(...)` aparece dentro de otro cuerpo:
  `illegal start of expression` en 172.
- `CloudinaryImageService.java:123-131` — `uploadImageToFolder(...)` queda con un `try`
  sin `catch`/`finally`; además `this.folder = properties.getFolder();` aparece en medio
  del cuerpo: `'try' without 'catch', 'finally' or resource declarations` en 127.

### 1.3 Duplicación de bloques por merge mal resuelto

- `AccommodationPaymentMethodService.java` tiene **dos declaraciones de clase**
  (líneas `39` y `120`), imports insertados en medio del archivo (línea `110`) y
  **dos métodos `list(...)`** (líneas `58` y `125`). Errores en cascada desde 110.
- `CloudinaryImageService.java` mezcla dos implementaciones de subida (`uploadImage`
  vs `uploadImageToFolder`) y campos (`defaultFolder` en 83 vs `folder` en 131).

**Acción:** rehacer la resolución de estos 4 archivos comparando ambas ramas; no se puede
mergear ni desplegar en este estado.

---

## 2. Migraciones Flyway con versión duplicada (BLOQUEANTE)

El merge combinó ramas que numeraron migraciones de forma independiente y dejó
**dos archivos con la misma versión**:

| Versión | Archivo |
|---|---|
| V6 | `V6__add_payment_method_type_specific_fields.sql` |
| V6 | `V6__multicurrency_and_additional_services.sql` |
| V7 | `V7__add_accommodation_branding_fields.sql` |
| V7 | `V7__create_reservations_tables.sql` |

Con `spring.flyway.enabled=true`, Flyway aborta al iniciar con
*"Found more than one migration with version 6/7"*. El backend **no arranca** en ningún
perfil.

Problemas adicionales de la misma raíz:

- `V7__create_reservations_tables.sql:2` dice `-- Migración V6: ...` (encabezado
  desactualizado).
- El orden de dependencias importa: `V7__create_reservations_tables` crea `guests` y
  `reservations`, y `V8`/`V9` las alteran. Renumerar exige respetar esa secuencia
  (p. ej. reservas pasaría a `V10+`, después de las migraciones de `development`).
- `V6__multicurrency...` y `V6__add_payment_method...` alteran las mismas tablas
  (`accommodations`, `accommodation_payment_methods`): hay que consolidar el orden.

**Acción:** renumerar por orden de llegada (V6/V7 de `development` primero, luego las de
reservas como V10, V11, V12), corregir encabezados y validar con una base limpia.

---

## 3. Violación de capas (Alta)

### 3.1 Controladores que acceden a repositorios

7 controladores importan repositorios directamente, saltándose la capa de servicio:

- `AccommodationController.java:7`
- `AccommodationCurrencyController.java:9`
- `AccommodationPaymentMethodController.java:8`
- `AccommodationProfileController.java:13`
- `AdditionalServiceController.java:9-10`
- `OwnerReservationController.java:9-10`
- `PublicPricingController.java:9-10`

Esto rompe el flujo `Controller → Service → Repository` y duplica reglas de negocio en
la capa web.

### 3.2 Transacciones en el controlador

- `PublicPricingController.java:62` y `:106` usan `@Transactional(readOnly = true)` dentro
  del controller. La gestión transaccional debe vivir en el servicio.

### 3.3 Entidades JPA expuestas como respuesta

Varios servicios devuelven entidades de persistencia en lugar de DTOs:

- `AccommodationPaymentMethodService.java:58,125,178` → `List<AccommodationPaymentMethod>` /
  `AccommodationPaymentMethod`.
- `AccommodationTemporaryDisableService.java:41,91` → `AccommodationTemporaryDisable`.

Esto acopla el contrato HTTP al modelo de base de datos (lazy loading, campos internos,
cambios de esquema que rompen la API).

---

## 4. Clases monolito / falta de separación de responsabilidades (Alta)

| Archivo | Líneas |
|---|---|
| `service/AuthService.java` | 533 |
| `service/ReservationService.java` | 522 |
| `service/EmailService.java` | 492 |
| `service/AdditionalServiceService.java` | 439 |
| `model/AccommodationTemporaryDisable.java` | 439 |
| `service/AccommodationPaymentMethodService.java` | 434 |
| `controller/AuthController.java` | 428 |
| `controller/AdditionalServiceController.java` | 332 |

- `AccommodationTemporaryDisable` (entidad) con 439 líneas mezcla estado y lógica de
  negocio que debería estar en `AccommodationTemporaryDisableService`.
- Servicios como `AuthService`, `ReservationService` y `EmailService` concentran
  demasiadas responsabilidades (autenticación + tokens + correo; reservas + links +
  disponibilidad; plantillas + envío + reintentos).

---

## 5. Estructura de paquetes por capa plana (Media)

Todo el backend usa **paquetes por capa técnica** (`controller/`, `service/`,
`repository/`, `model/`, `dto/`), sin módulos de dominio (`accommodation/`,
`reservation/`, `auth/`, `payment/`). Con ~20 controladores y ~20 servicios en carpetas
planas, la navegación y el aislamiento por feature se vuelven costosos.

Además, `dto/` mezcla todos los request/response de todos los módulos en dos carpetas, sin
subpaquetes por dominio.

No es un error funcional, pero sí una limitación estructural para escalar y para ubicar
código.

---

## 6. Tests mal ubicados / cobertura insuficiente (Media)

- `src/test/java/com/hostflow/hostflow/HostflowApplicationTests.java` está en el paquete
  **`com.hostflow.hostflow`** (duplicado), distinto de `com.hostflow`. Debe ser
  `com.hostflow`.
- Todos los tests viven en `service/` aunque dos son de integración
  (`AccommodationProfileReplacementIntegrationTest`, `AccommodationProfileRollbackIntegrationTest`);
  no hay separación `unit/` vs `integration/`.
- No hay tests de controllers (`MockMvc`), seguridad (401/403) ni de migraciones, que son
  justamente donde aparecieron los defectos.
- `AccommodationPaymentMethodServiceTest` está escrito para la versión duplicada/corrupta
  del servicio, por lo que tampoco compilará hasta resolver el punto 1.

---

## 7. Configuración duplicada y desalineada entre perfiles (Media)

- `application.properties` (~390 líneas) + `application-dev.properties` (~130) +
  `application-prod.properties` duplican bloques completos (datasource, Flyway, correo,
  JWT). Cualquier cambio hay que replicarlo en varios sitios.
- Flyway está configurado distinto:
  - `application.properties:153` → `baseline-version=5`
  - `application-dev.properties:65` → `baseline-version=5`
  - `application-prod.properties` → **no define** `baseline-version` (queda en 1)
  Esto produce comportamientos distintos entre perfiles sobre bases existentes.
- Los comentarios dicen "Flyway desactivado en desarrollo", pero `spring.flyway.enabled=true`
  en `application-dev.properties:62` (contradicción).
- `.gitignore` raíz + `frontend/.gitignore`; el backend no tiene `.gitignore` propio y
  `target/` no está versionado (correcto), pero la convención no es uniforme.

---

## 8. Controladores solapados, código muerto y nombrado ambiguo (Baja)

- `AccommodationController` y `AccommodationProfileController` comparten el mismo
  `@RequestMapping("/owners/accommodation")`; el primero maneja disable/enable/status/
  history/availability y el segundo profile/gallery/logo. El nombre no refleja la
  responsabilidad y el recurso queda repartido en dos clases.
- `AccommodationProfileController.java:140` tiene un endpoint **comentado**
  (`/* @PostMapping("/gallery") ... */`), lo que deja además
  `AccommodationImageRequest` como import sin uso real (código muerto).
- `AccommodationPaymentMethodController` usa base `/payment-methods` mientras el servicio
  y el modelo se llaman `AccommodationPaymentMethod*`: consistente, pero el prefijo
  `/owners/accommodation` se repite en 5 controladores en lugar de un `@RequestMapping`
  común.
- `model/AccommodationTemporaryDisable` contiene lógica de negocio que pertenece al
  servicio (ver punto 4).

---

## Verificación realizada

```
cd backend
./mvnw -q -DskipTests compile
```

Resultado: **BUILD FAILURE**. Archivos con errores de compilación:

1. `controller/AccommodationProfileController.java` — marcadores de conflicto.
2. `service/AccommodationProfileService.java` — método truncado.
3. `service/AccommodationPaymentMethodService.java` — clase/imports/métodos duplicados.
4. `service/CloudinaryImageService.java` — `try` sin `catch` y bloques duplicados.

Adicionalmente, las migraciones Flyway duplicadas (`V6`, `V7`) impedirían el arranque de
la aplicación aun resolviendo la compilación.

---

## Prioridad de corrección

1. **Resolver los 4 archivos corruptos** (conflictos/duplicados/truncamientos) y verificar
   `./mvnw test-compile`.
2. **Renumerar y ordenar las migraciones Flyway**; probar `flyway:migrate` sobre base
   limpia y sobre base existente.
3. Mover el acceso a repositorios de los controladores a los servicios y quitar
   `@Transactional` del controller.
4. Devolver DTOs (no entidades JPA) desde los servicios.
5. Corregir el paquete de `HostflowApplicationTests` y completar tests de controllers,
   seguridad y migraciones.
6. Unificar configuración de perfiles y `baseline-version`.
7. (Opcional, deuda técnica) dividir clases monolito y evaluar paquetes por dominio.
