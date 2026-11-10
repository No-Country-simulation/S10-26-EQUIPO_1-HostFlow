# HostFlow — Mapa de archivos del flujo de reservas

> **Fuente:** estructura y código del `backend.zip` recibido. Este mapa indica dónde vive cada responsabilidad para facilitar el rastreo antes de limpiar. No significa que todos estos archivos deban modificarse.

## 1. Núcleo del flujo

| Capa | Archivo | Responsabilidad observada |
|---|---|---|
| Controller owner | `src/main/java/com/hostflow/controller/OwnerReservationController.java` | Crear, listar, consultar, marcar pago, cancelar, renovar enlace, consultar disponibilidad y completar reserva. |
| Controller público | `src/main/java/com/hostflow/controller/PublicReservationController.java` | `GET /public/reservations/confirm` y `POST /public/reservations/confirm` para leer y completar onboarding mediante token. |
| Servicio principal | `src/main/java/com/hostflow/service/ReservationService.java` | Orquesta creación, validaciones, enlace, onboarding, snapshots de adicionales, moneda equivalente, cancelación, pago y cierre. |
| Servicio de correo | `src/main/java/com/hostflow/service/EmailService.java` | Correo inicial, correo de cancelación y correos al marcar pago; plantillas HTML y equivalencias informativas. |
| Plantilla confirmación | `src/main/resources/templates/emails/reservation-confirmation-guest.html` | Plantilla de confirmación de reserva/pago para el huésped. |
| Plantilla notificación owner | `src/main/resources/templates/emails/reservation-notification-owner.html` | Notificación al propietario. |

### Nota sobre el correo inicial con branding

El ZIP base inspeccionado contiene el envío inicial desde `EmailService`, pero no incluye el template nuevo `reservation-link-guest.html` ni el cambio de `EmailService` del patch de branding aplicado después. Según el estado informado por el equipo, ese patch ya está integrado y funcionando en la rama. Al actualizar este mapa desde el checkout actual, agregar:
- `src/main/resources/templates/emails/reservation-link-guest.html`
- versión actualizada de `src/main/java/com/hostflow/service/EmailService.java`

## 2. Entidades y estados

| Archivo | Responsabilidad |
|---|---|
| `model/Reservation.java` | Reserva, contacto preliminar, huésped vinculado, importes/snapshots, estado operativo, estado de pago, indicador de lancha, fechas y auditoría. Conserva un campo transitorio/deprecado `GuestPaymentMethod paymentMethod`. |
| `model/ReservationLink.java` | Token, URL, expiración, uso, invalidación y reactivaciones. |
| `model/ReservationGuestInfo.java` | Datos definitivos de onboarding: documento, alergias, preferencias y contacto. |
| `model/ReservationAdditionalService.java` | Snapshot de cada adicional seleccionado, precio, cantidad, modalidad y moneda aplicada. |
| `model/Guest.java` | Perfil reutilizable del huésped creado/encontrado al completar onboarding. |
| `model/GuestPaymentMethod.java` | Métodos de pago asociados al huésped; no forma parte del flujo aprobado de reserva y queda pendiente de rastreo antes de eliminar. |
| `model/Accommodation.java` | Alojamiento propietario de la reserva, moneda base, marca, capacidad, precio y métodos de pago propios. |
| `enums/ReservationStatus.java` | Estados `PENDING`, `CONFIRMED`, `CANCELLED`, `EXPIRED`, `COMPLETED`; `EXPIRED` requiere revisión. |
| `enums/PaymentStatus.java` | Estados de pago `PENDING` y `PAID`, separados del estado de reserva. |
| `enums/ServicePriceType.java` | Modalidad de precio de servicios adicionales. |
| `enums/Currency.java` | Monedas utilizadas por reserva y equivalencias. |

## 3. Repositorios

| Archivo | Uso |
|---|---|
| `repository/ReservationRepository.java` | Persistencia y consultas de reservas; incluye búsqueda con bloqueo para `mark-paid` y consulta de reservas por alojamiento/huésped. |
| `repository/ReservationLinkRepository.java` | Búsqueda de enlaces por token, bloqueo pesimista, histórico por reserva y último enlace. |
| `repository/GuestRepository.java` | Buscar huésped por email/documento y persistir perfil. |
| `repository/AdditionalServiceRepository.java` | Obtener servicios activos del alojamiento y validar que el seleccionado pertenece al alojamiento de la reserva. |
| `repository/AccommodationRepository.java` | Obtener alojamiento del owner y bloquear fila durante creación para serializar disponibilidad. |

## 4. DTOs de entrada

| Archivo | Función / estado |
|---|---|
| `dto/request/CreateReservationRequest.java` | Datos preliminares de reserva: nombre/apellido, email, teléfono, fechas, cantidad, solicitudes; también contiene campos legacy de alergias/preferencias y expiración configurable que deben revisarse. |
| `dto/request/CompleteReservationOnboardingRequest.java` | Contrato actual de onboarding: datos personales/documento, alergias, preferencias, adicionales y `boatTransferRequested`. |
| `dto/request/ConfirmReservationRequest.java` | DTO legacy `@Deprecated`, solo `paymentMethodId`; candidato a retirar tras buscar referencias. |

## 5. DTOs de salida

| Archivo | Función |
|---|---|
| `dto/response/ReservationResponse.java` | Respuesta para panel del owner; revisar campos heredados, especialmente `paymentMethod`. |
| `dto/response/ReservationLinkResponse.java` | Datos del enlace para el owner. |
| `dto/response/ReservationOnboardingDetailsResponse.java` | Datos públicos para renderizar onboarding: alojamiento, reserva, métodos de pago del alojamiento, servicios y equivalencias. |
| `dto/response/ReservationOnboardingConfirmationResponse.java` | Resumen final tras onboarding, métodos de pago del alojamiento y equivalencias. |
| `dto/response/GuestResponse.java` | Datos de perfil Guest. |
| `dto/response/GuestPaymentMethodResponse.java` | DTO del submódulo de métodos de pago del huésped; no confundir con métodos de pago del alojamiento. |

## 6. Servicios relacionados

| Archivo | Relación con reservas |
|---|---|
| `service/GuestService.java` | Resuelve y gestiona perfiles Guest; también implementa CRUD de métodos de pago del huésped, que es un flujo separado/legacy respecto de reservas. |
| `service/AdditionalServiceService.java` | CRUD de catálogo de adicionales del alojamiento. La reserva solo consume servicios activos y persiste snapshots. |
| `service/AccommodationPaymentMethodService.java` | Gestiona métodos de pago del alojamiento que se muestran al huésped. |
| `service/CurrencyConversionService.java` | Convierte importes para equivalencias informativas; no reemplaza moneda ni total base de la reserva. |
| `service/OwnerDashboardService.java` | Puede consumir estados/ingresos de reservas para los indicadores del panel; revisar antes de cambiar `COMPLETED`. |

## 7. Controladores y endpoints relacionados indirectamente

| Archivo | Relación |
|---|---|
| `controller/PublicGuestController.java` | Endpoints para crear/buscar Guest y CRUD de métodos de pago del huésped. Es el principal punto de entrada del submódulo a evaluar, no borrar a ciegas. |
| `controller/AdditionalServiceController.java` | Administra catálogo de adicionales que se ofrece durante onboarding. |
| `controller/AccommodationPaymentMethodController.java` | Métodos de pago del alojamiento; requisito previo y datos visibles en onboarding. |
| `controller/AccommodationCurrencyController.java` | Configuración de monedas habilitadas para equivalencias. |
| `controller/OwnerDashboardController.java` | Expone métricas que pueden depender del estado de reserva. |
| `controller/AdminDashboardController.java` | Revisar solo si consulta datos agregados de reservas. |

## 8. Configuración y persistencia

| Archivo | Relevancia |
|---|---|
| `src/main/resources/application.properties` | Configuración general, URL frontend, correo, persistencia y parámetros del servicio. |
| `src/main/resources/db/migration/V8__create_guests_and_reservations_tables.sql` | Creación inicial de tablas de huéspedes/reservas/enlaces según migración histórica. |
| `src/main/resources/db/migration/V9__multicurrency_and_additional_services.sql` | Cambios de moneda y adicionales. |
| `src/main/resources/db/migration/V10__complete_guests_reservations_and_payment_methods.sql` | Ajustes posteriores de Guest/Reservation/payment methods; revisar antes de proponer retirar relaciones. |
| `src/main/java/com/hostflow/config/SecurityConfig.java` | Reglas globales de seguridad y rutas públicas/owner. |
| `src/test/java/com/hostflow/controller/PublicReservationOnboardingIntegrationTest.java` | Prueba de integración principal del onboarding público. |

## 9. Búsquedas recomendadas antes de borrar

Ejecutar desde la raíz del repositorio completo (backend y frontend):

```bash
# Dependencias del método de pago del huésped
grep -RInE "GuestPaymentMethod|guestPaymentMethod|paymentMethodId|payment-methods" .

# DTO y campos legacy
grep -RInE "ConfirmReservationRequest|guestAllergies|guestPreferences|linkExpirationHours|paymentMethod" .

# Estados y transiciones
grep -RInE "ReservationStatus\\.EXPIRED|EXPIRED|CONFIRMED|COMPLETED|PaymentStatus" .

# Enlaces y operaciones
grep -RInE "reactivate-link|mark-paid|check-availability|/complete|public/reservations/confirm" .

# Campos de respuesta usados por frontend
grep -RInE "paymentMethod|confirmationCode|paymentStatus|boatTransferRequested" .
```

## 10. Fuera de este mapa

No se incluyen como objetivo de limpieza los módulos completos de cambio de moneda, adicionales, perfil del alojamiento ni métodos de pago del alojamiento. Se listan solo los puntos de contacto con el flujo de reserva.
