# HostFlow — Inventario de limpieza del flujo de reservas

> **Objetivo:** lista de trabajo para depurar el módulo de reservas sin borrar prematuramente código que todavía puede tener consumidores.  
> **Regla de trabajo:** primero rastrear referencias y confirmar uso; después modificar; finalmente eliminar. No hacer limpieza masiva ni cambios de esquema durante el inventario.

## 1. Criterio de clasificación

- [ ] **P0 — Bloqueante/seguridad:** corregir antes de considerar cerrado el flujo.
- [ ] **P1 — Contrato funcional:** contradice el plan aprobado o deja deuda funcional directa.
- [ ] **P2 — Limpieza segura:** legado o código redundante, pero solo después de verificar referencias.
- [ ] **P3 — Fuera de alcance:** pertenece a otro módulo o puede esperar a la integración.

Los checks están inicialmente vacíos: este documento es el backlog, no una declaración de que los puntos ya se corrigieron.

## 2. P0 — Validaciones que requieren revisión antes de limpiar

- [ ] **Revisar autorización del endpoint `GET /owners/reservations/check-availability`.** El controlador exige rol `OWNER`, pero recibe `accommodationId` del cliente y llama a `isAccommodationAvailable(accommodationId, ...)`. Verificar que el servicio compruebe que el alojamiento pertenece al owner autenticado; si no, ajustar el contrato para derivar el alojamiento del usuario autenticado.
- [ ] **Revisar validación de solapamientos bajo concurrencia.** La creación bloquea el alojamiento con `findByOwner_IdForUpdate`, lo que parece serializar creaciones del mismo alojamiento. Confirmar que el repositorio y la transacción realmente mantienen ese bloqueo en la base usada.
- [ ] **Verificar política de correo y transacciones.** `EmailService` captura excepciones internamente. Por tanto, el comentario de `createReservation` que afirma que un fallo de envío revierte la transacción no parece corresponder al comportamiento real: confirmar qué ocurre si el proveedor rechaza o no entrega el correo.
- [ ] **Verificar idempotencia de `mark-paid` y efectos de correo.** El servicio bloquea la reserva y evita duplicar la transición `PENDING → PAID`; confirmar pruebas de concurrencia y que un error de correo no revierte el pago.

## 3. P1 — Diferencias entre el plan aprobado y la implementación observada

### 3.1 Creación de reserva y contrato de entrada

- [ ] **Limpiar `CreateReservationRequest`.** Todavía incluye `guestPreferences` y `guestAllergies`, aunque `createReservation()` no los usa y el plan establece que alergias/preferencias se capturan en onboarding. Verificar consumidores frontend y retirar esos campos coordinadamente.
- [ ] **Revisar `linkExpirationHours`.** El DTO permite valores entre 1 y 168 y expone `getLinkExpirationHoursOrDefault()`, pero `createReservation()` y `createConfirmationLink()` usan 24 horas fijas. Decidir si se elimina del request o se conserva solo por compatibilidad; la regla acordada es 24 h no configurable por cliente.
- [ ] **Alinear documentación OpenAPI de `OwnerReservationController`.** Los comentarios del endpoint de creación aún dicen que el huésped debe tener un método de pago y que la expiración es configurable; el código actual valida métodos del alojamiento y usa 24 h.
- [ ] **Revisar condición previa del alojamiento.** El plan indica que todos los campos del perfil son válidos salvo número de baños, más logo, galería y método de pago activo. Comparar la validación real `validateAccommodationReadyForReservations()` con el perfil y las decisiones finales del equipo; no cambiarla sin confirmar el checklist de producto.

### 3.2 Onboarding y método de pago del huésped

- [ ] **Eliminar la dependencia funcional del método de pago del huésped en `Reservation`.** La entidad mantiene un campo `@Deprecated @Transient GuestPaymentMethod paymentMethod` que ya no forma parte del modelo persistido.
- [ ] **Revisar `ConfirmReservationRequest`.** Está marcado `@Deprecated` y solo contiene `paymentMethodId`; confirmar que no existan consumidores y retirarlo junto con cualquier endpoint/servicio legado que aún lo invoque.
- [ ] **Revisar `ReservationService.confirmReservation(String, Long)`.** Es un método `@Deprecated` que siempre lanza `BusinessException`. Buscar referencias en controladores, pruebas y otros servicios; si no tiene consumidores, eliminarlo.
- [ ] **Revisar `ReservationResponse.paymentMethod`.** El DTO todavía expone `GuestPaymentMethodResponse paymentMethod`; determinar si el mapper lo sigue rellenando desde el campo transitorio. Si no se usa en frontend/API, retirarlo coordinadamente.
- [ ] **Separar el submódulo `GuestPaymentMethod` del flujo de reservas, no borrarlo todavía.** `PublicGuestController`, `GuestService`, entidad, repositorio y DTOs implementan CRUD de métodos de pago de huésped. El plan dice que el huésped no necesita registrar un método de pago. Primero verificar si otro módulo o frontend lo consume; después decidir eliminación independiente.
- [ ] **Verificar que el endpoint público de onboarding no exija `paymentMethodId`.** El contrato vigente usa `CompleteReservationOnboardingRequest`, con datos del huésped, alergias, preferencias, adicionales y `boatTransferRequested`; `ConfirmReservationRequest` es legado.

### 3.3 Enlaces, estados y cancelación

- [ ] **Revisar `ReservationStatus.EXPIRED`.** El enum conserva `EXPIRED` y su Javadoc afirma que el vencimiento del enlace expira la reserva. El plan exige que expire el enlace, no la reserva. Buscar usos reales del estado y datos existentes antes de retirarlo.
- [ ] **Alinear `ReservationStatus` Javadoc con el comportamiento real.** Actualmente describe `CONFIRMED` como reserva con pago procesado, pero el código cambia a `CONFIRMED` al completar onboarding y mantiene `paymentStatus=PENDING`.
- [ ] **Revisar `ReservationLink.isValid()` y los métodos `invalidate()` / `markAsUsed()`.** Confirmar que validen expiración, uso e invalidación y que el enlace solo se consuma después de un onboarding exitoso.
- [ ] **Alinear endpoint `reactivate-link`.** El request acepta `expirationHours`, pero el servicio ignora ese valor y crea siempre un enlace de 24 h. Decidir si eliminar el parámetro del endpoint para reflejar el contrato.
- [ ] **Definir política del correo de cancelación.** `cancelReservation()` envía `sendReservationCancelledEmail()` al huésped. El acuerdo funcional del flujo había limitado los correos principales a: (1) enlace inicial y (2) confirmación después de marcar pago. Confirmar si el correo de cancelación es una excepción aceptada o debe salir del alcance; no eliminarlo sin decisión explícita.
- [ ] **Confirmar cancelación y pago.** El servicio permite cancelar reservas no completadas, incluso si están pagadas; definir si se permite cancelar una reserva `PAID` y qué representa esa acción sin reembolso integrado.

### 3.4 Pago manual y cierre operativo

- [ ] **Mantener separados `ReservationStatus` y `PaymentStatus`.** No volver a inferir pago a partir de `CONFIRMED`.
- [ ] **Confirmar regla de `completeReservation()`.** Actualmente permite completar una reserva `CONFIRMED` una vez pasado el check-out, incluso si `paymentStatus` sigue `PENDING`. El plan no termina de definir si debe exigirse pago para completar; decidirlo con producto.
- [ ] **Verificar impacto de `COMPLETED` en dashboard/ingresos.** El controlador y servicio describen esta acción como registro de ingresos. Confirmar que el dashboard no contabilice dos veces ni confunda total reservado con dinero efectivamente recibido.

## 4. P2 — Limpieza de código legado o redundante (después del rastreo)

- [ ] **Revisar `ReservationService.getReservationByConfirmationCode()`.** No aparece como endpoint en `OwnerReservationController` ni `PublicReservationController`; buscar consumidores internos antes de eliminar.
- [ ] **Revisar `ReservationService.listReservationsByGuest()`.** No aparece expuesto en los controladores revisados; buscar usos en otros servicios y tests.
- [ ] **Revisar métodos del `ReservationLinkRepository` que no se usan.** Contrastar `findByTokenAndReservation_Id()` y `findAllByReservation_Id()` con referencias reales antes de quitar cualquiera.
- [ ] **Revisar imports/campos inyectados no utilizados en `OwnerReservationController`.** En el snapshot, `ownerRepository` sí se usa para resolver el usuario; comprobar el resto con IDE/compilador antes de borrar.
- [ ] **Actualizar comentarios y versiones de clases que describen el flujo anterior** (pago del huésped, confirmación con pago procesado, expiración de reserva), sin cambiar lógica en la misma tarea.
- [ ] **Eliminar compatibilidad transitoria `@Transient GuestPaymentMethod paymentMethod` y `getTaxes()` de `Reservation`** solo cuando `ReservationResponse`, mappers, frontend y pruebas ya no los referencien.
- [ ] **Revisar migraciones históricas `V8` y `V10` respecto de `guest_payment_method_id` y `guest_payment_methods`.** No editar migraciones ya aplicadas. Si finalmente se elimina la relación/tabla, preparar una migración nueva y revisar datos existentes.

## 5. P3 — Mantener fuera de esta limpieza de reservas

- [ ] No reestructurar el módulo de cambio de moneda ni los servicios adicionales salvo que un defecto impida completar una reserva.
- [ ] No cambiar snapshots de importes ni el cálculo de moneda base mientras se inventaría el flujo.
- [ ] No rehacer el módulo de alojamiento, métodos de pago del alojamiento o perfil del propietario.
- [ ] No introducir cambios de migraciones hasta acordar el esquema final y la estrategia del equipo.
- [ ] No eliminar clases solo porque no se ven en la pantalla actual: primero buscar referencias en backend, frontend, tests, datos y documentación.

## 6. Secuencia recomendada para ejecutar la limpieza

- [ ] **Paso 1 — Congelar línea base:** guardar commit/hash de la rama y confirmar que el backend compila y las pruebas actuales pasan.
- [ ] **Paso 2 — Mapa de referencias:** buscar cada símbolo del inventario en todo el repositorio (backend + frontend), incluyendo tests y llamadas API.
- [ ] **Paso 3 — Contratos:** acordar cambios de request/response y endpoints antes de borrar DTOs o campos.
- [ ] **Paso 4 — Depuración por grupos pequeños:** primero comentarios/contratos obsoletos; después métodos legados sin consumidores; finalmente relaciones/tablas con migración nueva si corresponde.
- [ ] **Paso 5 — Pruebas de regresión:** creación, consulta pública, onboarding único, renovación, cancelación, mark-paid, emails y complete.
- [ ] **Paso 6 — Integración:** revisar conflictos con la rama de Carlos y con `development`; no mezclar refactorizaciones ajenas.

## 7. Evidencia que falta para cerrar el inventario

- [ ] Código del frontend que crea reservas, consume onboarding y muestra el resumen.
- [ ] Llamadas API reales a `check-availability`, `reactivate-link`, `complete` y métodos de pago del huésped.
- [ ] Confirmación del comportamiento deseado del correo de cancelación.
- [ ] Confirmación de si una reserva puede pasar a `COMPLETED` sin estar `PAID`.
- [ ] Estado real de la rama después de integrar el patch del correo de enlace (el `backend.zip` usado como base no contiene el template nuevo de enlace descrito en la revisión anterior).
