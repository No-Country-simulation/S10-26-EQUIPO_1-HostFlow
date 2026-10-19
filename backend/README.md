# HostFlow — Backend API

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%2B-blue)
![JWT](https://img.shields.io/badge/JWT-HS512-red)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI%203-85EA2D)

API REST del **sistema de gestión de alojamientos HostFlow**, construida con **Spring Boot 3.2.5 / Java 17**. Provee autenticación mediante **JWT (HS512)**, registro de dueños de alojamiento, control de acceso por **roles (ADMIN / OWNER)**, recuperación de contraseña por **código de 6 dígitos enviado por email** y documentación interactiva con **Swagger UI**.

---

## 1. Arquitectura general

El backend sigue el patrón de **capas desacopladas** de Spring:

```
HTTP Request
    │
    ▼
┌─────────────────┐
│  Controller     │  AuthController · OwnerController        (capa web, DTOs, validación)
└────────┬────────┘
         ▼
┌─────────────────┐
│  Service        │  AuthService · OwnerService ·            (lógica de negocio, @Transactional)
│                 │  PasswordResetService · EmailService · JwtService
└────────┬────────┘
         ▼
┌─────────────────┐
│  Repository     │  OwnerRepository · RoleRepository ·      (Spring Data JPA)
│                 │  PasswordResetTokenRepository
└────────┬────────┘
         ▼
┌─────────────────┐
│  Model (JPA)    │  Owner · Role · PasswordResetToken  ──►  PostgreSQL
└─────────────────┘

Security (transversal): SecurityConfig · JwtAuthenticationFilter ·
                        CustomUserDetailsService · BCryptPasswordEncoder
```

| Módulo | Clases | Responsabilidad |
|---|---|---|
| **Auth** | `AuthController`, `AuthService` | Registro de dueños, login, emisión de JWT Bearer. |
| **Password Recovery** | `PasswordResetController (en AuthController)`, `PasswordResetService`, `EmailService` | Código de 6 dígitos por email, validación y cambio de contraseña. |
| **Owners** | `OwnerController`, `OwnerService` | Consulta y administración de dueños (activar/desactivar cuentas). |
| **Security** | `SecurityConfig`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, `PasswordEncoderConfig` | Cadena de filtros, JWT stateless, CORS, BCrypt, roles. |
| **Config** | `SwaggerConfig`, `EmailConfig`, `WebConfig`, `DataInitializer` | OpenAPI, JavaMailSender, CORS web y seed de datos. |
| **Exceptions** | `GlobalExceptionHandler`, `BusinessException`, `ResourceNotFoundException` | Respuestas de error HTTP uniformes. |

---

## 2. Stack tecnológico (verificado en `pom.xml`)

| Tecnología | Versión | Uso |
|---|---|---|
| Java | **17** | Lenguaje y runtime |
| Spring Boot | **3.2.5** | Framework base (Web, Data JPA, Security, Validation, Mail, DevTools) |
| PostgreSQL Driver | (Spring Boot managed) | Base de datos de producción |
| Flyway Core | (Spring Boot managed) | Migraciones de esquema |
| JJWT (io.jsonwebtoken) | **0.12.5** | Generación y validación de tokens (algoritmo **HS512**) |
| springdoc-openapi | **2.5.0** | Swagger UI / OpenAPI 3 |
| Lombok | (Spring Boot managed) | Reducción de boilerplate (`@Getter`, `@Builder`, etc.) |
| Spring Boot Maven Plugin | 3.2.5 | Empaquetado en JAR ejecutable |

---

## 3. Requisitos previos — *instalación manual en el equipo*

El proyecto **no requiere instalar Maven** (incluye el *wrapper*), pero sí necesitas:

| Software | Versión | ¿Para qué? | Verificación |
|---|---|---|---|
| **JDK 17** (Temurin, Oracle o similar) | 17+ | Compilar y ejecutar el backend | `java -version` |
| **IntelliJ IDEA** | 2022+ | IDE recomendado (plugin *Spring Boot* preinstalado en Ultimate; en Community instalar *Spring Boot Helper*) | — |
| **PostgreSQL** | 13+ | Motor de base de datos (driver incluido en el proyecto) | `psql --version` |
| **pgAdmin** (opcional) | — | Administración visual de PostgreSQL | — |
| **Git** | 2.x | Clonar el repositorio | `git --version` |
| **Postman** (opcional) | — | Pruebas manuales de endpoints | — |

> ⚠️ **Importante:** si el `datasource` apunta a `localhost:5432`, PostgreSQL debe estar **corriendo** y la base de datos `hostflow` creada previamente:
> ```sql
> CREATE DATABASE hostflow;
> ```

---

## 4. Qué se descarga **automáticamente** al ejecutar

| # | Qué | De dónde | Dónde queda |
|---|---|---|---|
| 1 | **Maven** (la versión exacta del wrapper) | Internet (según `.mvn/wrapper/maven-wrapper.properties`) | `~/.m2/wrapper` |
| 2 | **Todas las dependencias** del `pom.xml` (Spring Boot starters, JJWT, springdoc, Lombok, PostgreSQL driver, Flyway…) | **Maven Central** | `~/.m2/repository` |
| 3 | **Plugins de build** (`spring-boot-maven-plugin`, compiler, surefire…) | Maven Central | caché local de Maven |
| 4 | **Esquema de base de datos** | Generado por **Hibernate** (`ddl-auto`) y/o por migraciones **Flyway** en `src/main/resources/db/migration` | PostgreSQL |
| 5 | **Datos iniciales (seed)** | `DataInitializer` inserta roles (`ROLE_ADMIN`, `ROLE_OWNER`) al arrancar | Tabla `roles` |

> En resumen: solo instalar JDK 17 + IntelliJ + PostgreSQL; **todo lo demás lo resuelve Maven automáticamente en el primer build**.

---

## 5. Configuración

Crear/editar `src/main/resources/application.properties` (o `application.yml`):

```properties
# ---------- Servidor ----------
server.port=8080
spring.application.name=HostFlow

# ---------- PostgreSQL ----------
spring.datasource.url=jdbc:postgresql://localhost:5432/hostflow
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# ---------- JWT (JwtService) ----------
jwt.secret=${JWT_SECRET}                # min. 64 bytes para HS512
jwt.expiration=86400000                 # access token: 24 h
jwt.refresh-expiration=604800000        # refresh token: 7 días
jwt.issuer=HostFlow

# ---------- Recuperación de contraseña (PasswordResetService) ----------
app.password-reset.expiration-minutes=60
app.password-reset.code-length=6

# ---------- Correo (EmailConfig / EmailService) ----------
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_APP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

> 🔐 **Buena práctica:** nunca versionar credenciales. Usar variables de entorno (`DB_PASSWORD`, `JWT_SECRET`, `MAIL_USERNAME`, `MAIL_APP_PASSWORD`) o un `application-local.properties` fuera del VCS.

---

## 6. Ejecución

### Opción A — Consola (Linux / macOS)
```bash
cd backend
./mvnw spring-boot:run
```

### Opción B — Consola (Windows)
```cmd
cd backend
mvnw.cmd spring-boot:run
```

### Opción C — JAR empaquetado
```bash
./mvnw clean package -DskipTests
java -jar target/hostflow-backend-1.0.0.jar
```

### Opción D — IntelliJ IDEA
1. `File → Open…` → seleccionar la carpeta que contiene `pom.xml`.
2. Esperar a que el IDE importe el proyecto Maven y descargue dependencias (barra inferior).
3. Localizar la clase principal `BackendApplication` y pulsar ▶️ **Run**.
4. Con **DevTools** activo, cualquier cambio en `src/main/java` reinicia la aplicación automáticamente (hot reload).

**Servicios disponibles al arrancar:**

| Recurso | URL |
|---|---|
| API base | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

---

## 7. Estructura del proyecto en IntelliJ IDEA

```
backend/
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties       ← versión de Maven a descargar
├── mvnw / mvnw.cmd                        ← Maven Wrapper (Linux/Mac · Windows)
├── pom.xml                                ← Spring Boot 3.2.5 · Java 17 · dependencias
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── hostflow/
        │           ├── BackendApplication.java               ← @SpringBootApplication (main)
        │           ├── config/
        │           │   ├── DataInitializer.java              ← seed de roles al arrancar
        │           │   ├── EmailConfig.java                  ← JavaMailSender
        │           │   ├── SwaggerConfig.java                ← OpenAPI / Swagger UI
        │           │   └── WebConfig.java                    ← configuración web/CORS
        │           ├── controller/
        │           │   ├── AuthController.java               ← /auth/** (público)
        │           │   └── OwnerController.java              ← /owners/** (JWT + roles)
        │           ├── dto/
        │           │   ├── request/
        │           │   │   ├── LoginRequest.java
        │           │   │   ├── OwnerRegisterRequest.java
        │           │   │   ├── ForgotPasswordRequest.java
        │           │   │   └── ResetPasswordRequest.java
        │           │   └── response/
        │           │       ├── AuthResponse.java
        │           │       ├── OwnerResponse.java
        │           │       └── PasswordResetResponse.java
        │           ├── enums/
        │           │   └── RoleType.java                     ← ROLE_ADMIN · ROLE_OWNER
        │           ├── exception/
        │           │   ├── BusinessException.java
        │           │   ├── ResourceNotFoundException.java
        │           │   └── GlobalExceptionHandler.java       ← @RestControllerAdvice
        │           ├── model/
        │           │   ├── Owner.java
        │           │   ├── Role.java
        │           │   └── PasswordResetToken.java
        │           ├── repository/
        │           │   ├── OwnerRepository.java
        │           │   ├── RoleRepository.java
        │           │   └── PasswordResetTokenRepository.java
        │           ├── security/
        │           │   ├── SecurityConfig.java               ← SecurityFilterChain · CORS · stateless
        │           │   ├── JwtAuthenticationFilter.java      ← filtro OncePerRequestFilter
        │           │   ├── CustomUserDetailsService.java     ← carga usuario por email
        │           │   └── PasswordEncoderConfig.java        ← BCryptPasswordEncoder @Bean
        │           └── service/
        │               ├── AuthService.java                  ← registerOwner() · login()
        │               ├── OwnerService.java                 ← consultas + toggleOwnerStatus()
        │               ├── PasswordResetService.java         ← forgotPassword() · resetPassword()
        │               ├── EmailService.java                 ← envío de correos
        │               └── JwtService.java                   ← HS512 · claims · expiración
        └── resources/
            ├── application.properties
            ├── static/
            ├── templates/                                    ← plantillas de correo
            └── db/migration/                                 ← migraciones Flyway (V1__, V2__…)
```

> **Regla de oro:** los controladores reciben/devuelven **DTOs**; las **entidades JPA** nunca se exponen directamente (`OwnerService.mapToResponse()` excluye la contraseña).

---

## 8. Endpoints de la API

### 8.1 Autenticación — `AuthController` (públicos, sin JWT)

| Método | Ruta | Body (DTO) | Respuesta | Descripción |
|---|---|---|---|---|
| `POST` | `/auth/register` | `OwnerRegisterRequest` | `201 → AuthResponse` | Registra un dueño: valida términos, unicidad de email, encripta BCrypt, asigna `ROLE_OWNER` y devuelve JWT. |
| `POST` | `/auth/login` | `LoginRequest` | `200 → AuthResponse` | Autentica credenciales con `AuthenticationManager` y devuelve JWT Bearer. |
| `POST` | `/auth/forgot-password` | `ForgotPasswordRequest` | `200 → PasswordResetRequestResponse` | Genera código de 6 dígitos, invalida tokens previos, lo envía por email. **Siempre responde éxito** (anti-enumeración). |
| `POST` | `/auth/reset-password` | `ResetPasswordRequest` | `200 → PasswordResetResponse` | Valida código (existente, no expirado, no usado), actualiza contraseña BCrypt, marca token usado y envía confirmación. |

> `SecurityConfig` también permite `/api/v1/auth/**` para cubrir variantes de prefijo.

#### Ejemplo — Login
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"martina@hostflow.com","password":"Secret123"}'
```
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "martina@hostflow.com",
  "fullName": "Martina Silva",
  "roles": ["ROLE_OWNER"]
}
```

### 8.2 Dueños — `OwnerController` (requieren `Authorization: Bearer <token>`)

| Método | Ruta | Rol requerido | Descripción |
|---|---|---|---|
| `GET` | `/owners/email/{email}` | `OWNER` o `ADMIN` | Detalle de un dueño por email. |
| `GET` | `/owners/{id}` | `OWNER` o `ADMIN` | Detalle de un dueño por ID. |
| `GET` | `/owners` | **`ADMIN` únicamente** | Listado completo de dueños. |
| `PATCH` | `/owners/{id}/status?enabled=true\|false` | **`ADMIN` únicamente** | Activa/desactiva la cuenta (`enabled=false` impide el login sin borrar datos). |

> La restricción se aplica con `@PreAuthorize("hasAnyRole('OWNER','ADMIN')")` y `@PreAuthorize("hasRole('ADMIN')")` gracias a `@EnableMethodSecurity`.

---

## 9. Modelo Entidad-Relación (MER)

```mermaid
erDiagram
    OWNER ||--o{ PASSWORD_RESET_TOKEN : "solicita (por email)"
    OWNER }o--o{ ROLE : "tiene (tabla puente owner_roles)"

    OWNER {
        BIGINT id PK "IDENTITY"
        VARCHAR full_name "NOT NULL · 150"
        VARCHAR accommodation_name "NOT NULL · 200"
        VARCHAR email "NOT NULL · UNIQUE · 150"
        VARCHAR password "NOT NULL · BCrypt hash"
        BOOLEAN terms_accepted "NOT NULL · default false"
        BOOLEAN enabled "NOT NULL · default true"
        TIMESTAMP created_at "NOT NULL · @CreationTimestamp"
        TIMESTAMP updated_at "NOT NULL · @UpdateTimestamp"
    }

    ROLE {
        BIGINT id PK "IDENTITY"
        VARCHAR role_name "NOT NULL · UNIQUE · 50 · EnumType.STRING"
    }

    OWNER_ROLES {
        BIGINT owner_id FK "→ owners.id"
        BIGINT role_id FK "→ roles.id"
    }

    PASSWORD_RESET_TOKEN {
        BIGINT id PK "IDENTITY"
        VARCHAR token "NOT NULL · 10 · código de 6 dígitos"
        VARCHAR email "NOT NULL · 150 · dueño solicitante"
        TIMESTAMP created_at "NOT NULL · @CreationTimestamp"
        TIMESTAMP expires_at "NOT NULL · ahora + 60 min"
        BOOLEAN used "NOT NULL · default false"
    }
```

### Reglas del modelo

1. **`OWNER` ↔ `ROLE` (N:M)** — relación `@ManyToMany(fetch = EAGER)` con tabla puente **`owner_roles`** (`owner_id`, `role_id`). Un dueño puede acumular varios roles; todo registro nuevo recibe `ROLE_OWNER` por defecto.
2. **`OWNER` ↔ `PASSWORD_RESET_TOKEN` (1:N lógica)** — el token **no usa FK**, almacena el `email` del solicitante. Cada solicitud nueva **elimina los tokens pendientes anteriores** (`findByEmailAndUsedFalse` + `deleteAll`) y el token es **de un solo uso** (`used=true` tras el cambio).
3. **Unicidades e integridad:** `owners.email` y `roles.role_name` son `UNIQUE`; `terms_accepted` y `enabled` son `NOT NULL` con valores por defecto.
4. **`Role.roleName`** se persiste como `VARCHAR` con `@Enumerated(EnumType.STRING)` (robusto ante reordenamientos del enum).
5. **Auditoría:** `created_at` / `updated_at` gestionados por Hibernate (`@CreationTimestamp` / `@UpdateTimestamp`).
6. **DDL:** gestionado por Hibernate (`spring.jpa.hibernate.ddl-auto=update`); en producción se recomienda `validate` + migraciones **Flyway** (`db/migration`).

---

## 10. Flujo de seguridad (JWT stateless)

```
1. Cliente ──POST /auth/login {email,password}──► AuthController
2. AuthService ──► AuthenticationManager ──► DaoAuthenticationProvider
   (CustomUserDetailsService carga el Owner por email; BCrypt verifica hash)
3. JwtService firma JWT HS512: {sub=email, iss=HostFlow, iat, exp=+24h}
4. Cliente guarda el token y lo envía en cada request:
      Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
5. JwtAuthenticationFilter (antes de UsernamePasswordAuthenticationFilter):
   - Endpoints públicos (/auth/**, swagger) → continúa sin validar
   - Extrae el token, valida firma y expiración, carga UserDetails
   - Establece SecurityContextHolder → request autenticada
6. @PreAuthorize evalúa roles a nivel de método
```

**Hardening aplicado:**
- 🔐 Contraseñas solo como hash **BCrypt** (`PasswordEncoderConfig`).
- 🔐 **Anti-enumeración:** `forgot-password` siempre responde éxito, aunque el email no exista.
- 🔐 **Anti-replay de códigos:** expiración de 60 min, un solo uso, invalidación de anteriores.
- 🔐 Emails **ofuscados** en respuestas (`u***@d***.com`).
- 🔐 Sesión **STATELESS** (sin cookies de sesión) + **CSRF deshabilitado** (API sin estado).
- 🔐 **CORS** restringido a orígenes de desarrollo (`localhost:3000/4200/8080`).

---

## 11. Manejo de excepciones (`GlobalExceptionHandler`)

| Excepción | HTTP | Escenario |
|---|---|---|
| `ResourceNotFoundException` | `404` | Dueño no encontrado por ID/email. |
| `BusinessException` | `400` | Email duplicado, términos no aceptados, código inválido/expirado/usado. |
| `MethodArgumentNotValidException` | `400` | Fallo de Bean Validation en DTOs (`@NotBlank`, `@Email`, `@Size`, …). |
| `UsernameNotFoundException` | `401` | Credenciales inexistentes (Spring Security). |
| Acceso denegado / sin autenticación | `401` / `403` | Token ausente/inválido o rol insuficiente (`@PreAuthorize`). |

---

## 12. Solución de problemas comunes

| Síntoma | Causa | Solución |
|---|---|---|
| `mvnw: Permission denied` (Linux/Mac) | Falta permiso de ejecución | `chmod +x mvnw` |
| `Port 8080 already in use` | Puerto ocupado | Cambiar `server.port` o liberar: `lsof -i :8080` → `kill <PID>` |
| `Connection refused` a PostgreSQL | Servidor BD caído o BD inexistente | Iniciar PostgreSQL y ejecutar `CREATE DATABASE hostflow;` |
| Error `jwt.secret` / app no arranca | Falta la propiedad o < 64 bytes (HS512) | Definir `JWT_SECRET` con ≥ 64 caracteres |
| El correo de recuperación no llega | SMTP bloqueado o credenciales | Usar *app password* (Gmail) o MailHog en desarrollo |
| `401 Unauthorized` en `/owners/**` | Token ausente o expirado | Header: `Authorization: Bearer <token>`; renovar vía login |
| `403 Forbidden` en listado de dueños | Rol insuficiente | Solo `ADMIN` puede `GET /owners` y cambiar estados |
| Lombok no genera getters en el IDE | Annotations processing desactivado | IntelliJ: `Settings → Build → Compiler → Annotation Processors → Enable` |

---

## 13. Buenas prácticas aplicadas en el código

- ✅ **DTOs de entrada/salida** separados de las entidades (`OwnerRegisterRequest`, `AuthResponse`, `OwnerResponse`…).
- ✅ **Bean Validation** en los DTOs (`@Valid` en los controladores).
- ✅ **Hash BCrypt** y tokens de recuperación con expiración y un solo uso.
- ✅ **Configuración externalizada** (`@Value` con valores por defecto seguros, ej. `spring.mail.username:no-reply@hostflow.com`).
- ✅ **Transaccionalidad** explícita (`@Transactional`, lecturas con `readOnly = true`).
- ✅ **Documentación viva** con anotaciones OpenAPI (`@Operation`, `@ApiResponses`, `@SecurityRequirement`, `@Tag`).
- ✅ **Respuestas de error uniformes** vía `@RestControllerAdvice`.
- ✅ **Constructor injection** con Lombok `@RequiredArgsConstructor` (inmutabilidad y testabilidad).

---

*Documentación técnica del equipo Backend — HostFlow v1.0.0*
