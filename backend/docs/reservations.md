# Módulo de reservas

## 1. Descripción general

El módulo de reservas de HostFlow permite a los propietarios gestionar las estadías de sus alojamientos desde su creación hasta su finalización.

El flujo contempla la creación de una reserva por parte del propietario, el envío de un enlace de onboarding al huésped, la recopilación de sus datos, la selección de servicios adicionales, la confirmación de la reserva y el registro manual del pago.

HostFlow no procesa pagos. Los huéspedes realizan el pago utilizando los métodos configurados por el alojamiento y el propietario registra posteriormente su recepción en el sistema.

### Responsabilidades principales

- Crear y administrar reservas.
- Validar fechas, capacidad y disponibilidad del alojamiento.
- Generar y administrar enlaces de onboarding.
- Recopilar y conservar los datos del huésped asociados a cada estadía.
- Gestionar servicios adicionales y sus importes.
- Registrar el estado del pago.
- Gestionar cancelaciones y finalización de estadías.
- Enviar notificaciones por correo electrónico.

La lógica principal del módulo se encuentra en `ReservationService`.

## 2. Flujo general de una reserva

El ciclo habitual de una reserva es el siguiente:

1. El propietario autenticado crea una reserva e informa los datos preliminares del huésped y las fechas de estadía.
2. El backend valida el alojamiento, las fechas, la capacidad y la disponibilidad.
3. Se crea la reserva con estado `PENDING` y pago `PENDING`.
4. Se genera un enlace individual de onboarding con vigencia de 24 horas y se envía por correo electrónico.
5. El huésped accede al enlace y consulta los detalles de la estadía.
6. El huésped completa sus datos personales, alergias, preferencias y servicios adicionales.
7. El backend valida la información, calcula los importes y guarda los datos históricos de la estadía.
8. La reserva pasa a `CONFIRMED` y el enlace queda marcado como utilizado. El pago continúa en `PENDING`.
9. El huésped realiza el pago fuera de HostFlow utilizando alguno de los métodos informados por el alojamiento.
10. El propietario verifica la recepción del pago y lo registra manualmente.
11. Una vez superada la fecha de check-out y con el pago registrado, el propietario puede completar la reserva.

**Importante:** la confirmación del onboarding y el registro del pago son operaciones independientes. Una reserva puede estar `CONFIRMED` y continuar con el pago `PENDING`.

## 3. Estados y ciclo de vida

### 3.1. Estado de la reserva

El estado operativo se representa mediante `ReservationStatus`.

| Estado | Descripción | ¿Bloquea fechas? |
|---|---|---|
| `PENDING` | Reserva creada; falta completar el onboarding. | Sí |
| `CONFIRMED` | El huésped completó el onboarding. | Sí |
| `CANCELLED` | Reserva cancelada por el propietario. | No |
| `COMPLETED` | Estadía finalizada bajo las reglas del módulo. | No |
| `EXPIRED` | Valor legado conservado por compatibilidad. | No debe utilizarse en lógica nueva |

El flujo habitual es:

```text
             ┌──────────────┐
             │   PENDING    │
             └──────┬───────┘
                    │
          Onboarding completado
                    │
                    ▼
             ┌──────────────┐
             │  CONFIRMED   │
             └──────┬───────┘
                    │
       Pago registrado y check-out
             ya superado
                    │
                    ▼
             ┌──────────────┐
             │  COMPLETED   │
             └──────────────┘

    PENDING ────────┐
                    ▼
                CANCELLED
                    ▲
    CONFIRMED ──────┘
```

Las reservas `PENDING` y `CONFIRMED` bloquean la disponibilidad. La expiración del enlace de onboarding no modifica el estado de la reserva ni libera sus fechas.

### 3.2. Estado del pago

El pago se representa mediante `PaymentStatus`, independientemente del estado de la reserva.

| Estado | Descripción |
|---|---|
| `PENDING` | El pago todavía no fue registrado como recibido. |
| `PAID` | El propietario registró que recibió el pago. |

La transición a `PAID` se realiza mediante una acción explícita del propietario.

No existe una integración de procesamiento de pagos dentro de este flujo.

### 3.3. Condiciones para completar una reserva

Una reserva solamente puede pasar a `COMPLETED` cuando se cumplen simultáneamente estas condiciones:

- Su estado actual es `CONFIRMED`.
- Su estado de pago es `PAID`.
- La fecha de check-out ya fue superada.

La implementación permite completar la reserva a partir del día siguiente al check-out, ya que no se maneja una hora específica de salida.

Esta transición también permite que la reserva sea considerada un ingreso asegurado por las operaciones del dashboard.

## 4. Creación y validación de reservas

La creación es una operación realizada exclusivamente por el propietario autenticado.

### 4.1. Datos iniciales

El propietario proporciona:

- Nombre y apellido del huésped.
- Correo electrónico.
- Teléfono (opcional).
- Fecha de check-in.
- Fecha de check-out.
- Cantidad de huéspedes.
- Solicitudes especiales (opcional).

Las alergias, preferencias y servicios adicionales no se reciben durante esta operación. Se recopilan posteriormente durante el onboarding.

### 4.2. Validaciones del alojamiento

Antes de crear una reserva, el backend comprueba que el alojamiento esté preparado para recibir reservas.

Entre los requisitos verificados se encuentran:

- Nombre del alojamiento.
- Cantidad de habitaciones válida.
- Capacidad de huéspedes válida.
- Precio y moneda base definidos.
- Datos de contacto válidos.
- Dirección.
- Configuración visual requerida.
- Logotipo y al menos una imagen de galería.
- Al menos un método de pago activo y válido.

Si el alojamiento no cumple los requisitos, la creación se rechaza.

### 4.3. Validaciones de fechas y capacidad

Las fechas deben cumplir las siguientes reglas:

- Check-in no puede ser anterior al día actual.
- Se permite realizar check-in el día actual.
- Check-out debe ser posterior a check-in.
- La estadía debe tener al menos una noche.
- La estadía no puede superar las 365 noches.
- La cantidad de huéspedes debe respetar la capacidad del alojamiento.

Si no se informa la cantidad de huéspedes, se utiliza el valor predeterminado de una persona.

### 4.4. Cálculo inicial

Al crear la reserva se toma el precio por noche y la moneda base configurados en el alojamiento.

El cálculo inicial es:

```text
subtotal = precioPorNoche × cantidadDeNoches

total = subtotal
```

En esta etapa todavía no se incluyen servicios adicionales.

Los importes y la moneda aplicados quedan registrados en la reserva.

### 4.5. Disponibilidad y concurrencia

La disponibilidad se valida considerando las reservas existentes del alojamiento.

Se utiliza un intervalo semiabierto:

```text
[checkIn, checkOut)
```

Esto significa que el día de check-out no bloquea una nueva reserva que comience ese mismo día.

Por ejemplo:

- Reserva A: del 10 al 15.
- Reserva B: del 15 al 18.

Ambas pueden coexistir porque la primera estadía termina el día en que comienza la segunda.

Las reservas `PENDING` y `CONFIRMED` se consideran activas para detectar solapamientos.

Para reducir conflictos cuando se crean reservas simultáneamente, la operación bloquea el registro del alojamiento durante la validación y creación mediante un bloqueo pesimista.

## 5. Enlaces de onboarding

Cada reserva nueva genera un enlace individual para que el huésped complete sus datos.

### 5.1. Generación

El enlace contiene un token UUID generado por el backend y tiene una vigencia fija de 24 horas.

Se envía al correo informado por el propietario al crear la reserva.

El enlace queda asociado a una reserva específica y se conserva un registro por cada emisión o renovación.

### 5.2. Consulta de información

El huésped puede consultar los detalles de la reserva utilizando el token.

La consulta permite obtener:

- Datos generales del alojamiento.
- Fechas y duración de la estadía.
- Cantidad de huéspedes.
- Importes y moneda base.
- Métodos de pago informativos del alojamiento.
- Servicios adicionales disponibles.
- Equivalencias monetarias cuando corresponda.

Consultar el enlace no lo consume. El uso se registra cuando el huésped completa el onboarding.

### 5.3. Uso único y validación

Para completar el onboarding, el backend comprueba que:

- El token exista.
- El enlace no haya expirado.
- El enlace no haya sido utilizado.
- El enlace no haya sido invalidado.
- La reserva asociada continúe en estado `PENDING`.
- El onboarding de la reserva no haya sido completado previamente.

La operación utiliza un bloqueo sobre el enlace para proteger el procesamiento ante solicitudes concurrentes.

Una vez completado el onboarding, el enlace queda marcado como utilizado.

### 5.4. Expiración y renovación

La expiración del enlace no cancela la reserva.

Si el enlace expira, el propietario puede renovarlo mientras la reserva permanezca en `PENDING`.

La renovación:

1. Invalida los enlaces anteriores que todavía puedan utilizarse.
2. Genera un token nuevo.
3. Establece una nueva vigencia de 24 horas.
4. Envía el nuevo enlace al huésped.

La renovación no crea otra reserva ni modifica las fechas bloqueadas.

## 6. Onboarding y gestión de huéspedes

El onboarding permite completar la información que no se solicita durante la creación inicial.

### 6.1. Información recopilada

El huésped proporciona:

- Nombre y apellido.
- Número de documento.
- Correo electrónico.
- Teléfono.
- Alergias o restricciones alimentarias.
- Preferencias.
- Servicios adicionales seleccionados.
- Indicador de solicitud de traslado en barco.

El backend valida los datos recibidos y verifica que los servicios seleccionados pertenezcan al alojamiento y continúen activos.

No se reciben ni almacenan métodos de pago del huésped.

### 6.2. Perfil de huésped

La entidad `Guest` representa el perfil reutilizable de una persona.

El documento de identidad se utiliza para identificar al huésped y evitar duplicar perfiles. Si ya existe un perfil con ese documento, se reutiliza y se actualizan sus datos de contacto.

El correo electrónico no es único y puede repetirse entre distintos perfiles.

### 6.3. Información histórica por reserva

La entidad `ReservationGuestInfo` conserva los datos declarados para una estadía concreta.

Incluye:

- Nombre y apellido.
- Documento.
- Correo electrónico.
- Teléfono.
- Preferencias.
- Alergias.

Esta separación permite mantener un perfil reutilizable del huésped y, al mismo tiempo, conservar la información asociada a cada reserva.

Los datos preliminares de contacto almacenados en `Reservation` al crearla también se conservan como referencia de lo informado inicialmente por el propietario. No se sobrescriben con los datos posteriores del onboarding.

## 7. Servicios adicionales

Los servicios adicionales son opciones que cada alojamiento puede ofrecer al huésped durante el onboarding.

Su administración se realiza desde el módulo de servicios adicionales del alojamiento.

### 7.1. Catálogo

Cada servicio contiene, entre otros datos:

- Nombre.
- Descripción.
- Precio base.
- Tipo de cobro.
- Estado activo/inactivo.
- Imágenes asociadas.

El precio se define en la moneda base del alojamiento.

Los tipos de cobro disponibles son:

| Tipo | Comportamiento |
|---|---|
| `PER_DAY` | Se multiplica por la cantidad de unidades y noches. |
| `TOTAL` | Se multiplica por la cantidad de unidades, sin depender de las noches. |

### 7.2. Validación de selecciones

Durante el onboarding, el backend comprueba que:

- El servicio exista.
- Pertenezca al alojamiento de la reserva.
- Se encuentre activo.
- No se encuentre duplicado en la selección.
- La cantidad sea válida.

El cliente no determina el precio final. El backend consulta el catálogo y calcula los importes utilizando los precios registrados.

### 7.3. Cálculo

Para un servicio `PER_DAY`:

```text
totalServicio = precioUnitario × cantidad × noches
```

Para un servicio `TOTAL`:

```text
totalServicio = precioUnitario × cantidad
```

El total de servicios adicionales es la suma de los importes de cada selección.

Finalmente:

```text
totalReserva = subtotalAlojamiento + totalServiciosAdicionales
```

### 7.4. Snapshots históricos

Cada servicio seleccionado genera un registro en `ReservationAdditionalService`.

Se conserva:

- Referencia al servicio del catálogo, cuando existe.
- Nombre del servicio al momento de la reserva.
- Cantidad.
- Precio unitario aplicado.
- Tipo de cobro.
- Cantidad de noches aplicada, cuando corresponde.
- Total de la línea.
- Moneda.

Estos datos permiten mantener el importe y el detalle histórico de la reserva aunque el propietario modifique o elimine posteriormente el servicio del catálogo.

## 8. Monedas y cálculo de importes

### 8.1. Moneda base

Los importes de la reserva se calculan y persisten en la moneda base del alojamiento.

La reserva conserva:

- Precio por noche.
- Subtotal del alojamiento.
- Total de servicios adicionales.
- Total final.
- Moneda aplicada.

Las equivalencias monetarias no reemplazan estos valores ni modifican los importes almacenados.

### 8.2. Equivalencias informativas

Durante el onboarding se pueden presentar equivalencias en USD y EUR cuando el alojamiento dispone de un método de pago internacional utilizable:

- Una billetera digital (`DIGITAL_WALLET`) válida.
- Un método que identifique PayPal en su nombre o proveedor.

Las equivalencias se calculan mediante `CurrencyConversionService` y se incluyen en `currencyEquivalents` cuando la conversión está disponible.

El campo `currency` continúa indicando la moneda base de los importes originales.

Por ejemplo, si la moneda base es ARS, el huésped puede visualizar:

- Importe original en ARS.
- Equivalencia informativa en USD.
- Equivalencia informativa en EUR.

Si no corresponde mostrar equivalencias o no se encuentra una conversión disponible, la lista puede estar vacía o contener solamente las monedas convertibles.

### 8.3. Servicios adicionales

La consulta de servicios adicionales también puede incluir precios convertidos mediante `CurrencyConversionService`.

Esta presentación es informativa. Los precios utilizados para calcular y persistir una reserva siguen siendo los del catálogo en la moneda base del alojamiento.

## 9. Métodos de pago

Los métodos de pago pertenecen al alojamiento, no al huésped.

El propietario puede administrar distintos métodos, como:

- `BANK_TRANSFER`
- `DIGITAL_WALLET`
- `CASH`
- `CARD`
- `OTHER`

Durante el onboarding se presentan al huésped los métodos activos y válidos configurados por el alojamiento, junto con las instrucciones y datos necesarios para realizar el pago.

HostFlow no captura credenciales bancarias del huésped, no procesa transacciones ni confirma pagos automáticamente.

El propietario es responsable de verificar la recepción del dinero.

## 10. Registro de pagos

El registro del pago se realiza mediante la acción `mark-paid` del propietario.

### Requisitos

Para registrar un pago, la reserva debe:

- Pertenecer al propietario autenticado.
- Encontrarse en estado `CONFIRMED`.
- Tener completado el onboarding y contar con la información del huésped.

Al registrar el pago:

- `paymentStatus` pasa a `PAID`.
- Se registra la fecha y hora en `paidAt`.
- Se registra el propietario que marcó el pago en `paymentMarkedBy`.

La operación es idempotente: si el pago ya está en `PAID`, repetir la solicitud no vuelve a modificarlo ni duplica las notificaciones.

Después de confirmar la transacción se envían correos de notificación al huésped y al propietario. Un fallo de envío no revierte el pago ya registrado.

## 11. Cancelaciones

El propietario puede cancelar reservas en estado `PENDING` o `CONFIRMED`.

También se permite cancelar una reserva cuyo pago ya figure como `PAID`, siempre que todavía no esté completada.

Al cancelar:

- El estado pasa a `CANCELLED`.
- Se registra el motivo, si fue proporcionado.
- Se registra la fecha de cancelación.
- Se invalidan los enlaces pendientes de uso.
- Las fechas dejan de bloquear la disponibilidad.
- Se intenta notificar al huésped por correo después de confirmar la transacción.

No se permite cancelar una reserva `COMPLETED` ni volver a cancelar una reserva ya cancelada.

**Importante:** HostFlow no procesa reembolsos. La cancelación de una reserva pagada representa un cambio de estado operativo; cualquier devolución debe gestionarse fuera de la plataforma.

## 12. Correos y notificaciones

El módulo utiliza `EmailService` para comunicar eventos relevantes del ciclo de vida.

Entre las notificaciones contempladas se encuentran:

| Evento | Destinatario / finalidad |
|---|---|
| Creación de reserva | Enviar al huésped el enlace de onboarding. |
| Renovación del enlace | Enviar al huésped el nuevo enlace. |
| Cambio de correo durante onboarding | Notificar al correo anterior del huésped. |
| Cancelación | Informar al huésped sobre la cancelación. |
| Registro del pago | Notificar al huésped y al propietario. |

### Consideraciones transaccionales

No todos los correos tienen el mismo comportamiento ante un fallo:

- **Creación y renovación del enlace:** se ejecutan en modo estricto. Si el envío falla, la operación se revierte para evitar dejar una reserva o renovación sin el enlace entregado.
- **Cancelación y registro del pago:** las notificaciones se ejecutan después del commit. Un fallo de correo no revierte el cambio de negocio ya persistido.
- **Cambio de correo del huésped:** la notificación al correo anterior se intenta después de confirmar el onboarding y no debe revertirlo.

## 13. API del módulo

Los controladores principales son:

- `OwnerReservationController`
- `PublicReservationController`

Los servicios y configuraciones relacionados se administran desde controladores específicos del alojamiento.

### 13.1. Endpoints del propietario

Base: `/owners/reservations`

Todos requieren autenticación JWT y rol `OWNER`.

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/owners/reservations` | Crear una reserva y generar el enlace de onboarding. |
| `GET` | `/owners/reservations` | Listar las reservas del alojamiento propio. |
| `GET` | `/owners/reservations/{reservationId}` | Consultar el detalle de una reserva propia. |
| `POST` | `/owners/reservations/{reservationId}/reactivate-link` | Renovar el enlace de onboarding. |
| `POST` | `/owners/reservations/{reservationId}/mark-paid` | Registrar manualmente un pago recibido. |
| `POST` | `/owners/reservations/{reservationId}/complete` | Completar una estadía que cumple los requisitos. |
| `POST` | `/owners/reservations/{reservationId}/cancel` | Cancelar una reserva. |
| `GET` | `/owners/reservations/check-availability` | Consultar disponibilidad de fechas. |

Para consultar disponibilidad se envían `checkIn` y `checkOut` como parámetros de tipo fecha (`YYYY-MM-DD`).

La autorización no depende únicamente del rol: cada operación comprueba que la reserva o el alojamiento correspondan al propietario autenticado.

### 13.2. Endpoints públicos de onboarding

Base: `/public/reservations`

No requieren JWT. El acceso se controla mediante el token individual del enlace.

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/public/reservations/confirm?token={token}` | Consultar la información de la reserva para el onboarding. |
| `POST` | `/public/reservations/confirm?token={token}` | Completar el onboarding y confirmar la reserva. |

El token debe incluirse en ambas operaciones. La consulta no consume el enlace; el envío del onboarding sí.

### 13.3. Endpoints relacionados

Estos controladores administran recursos utilizados por el módulo:

**Servicios adicionales**

Base: `/owners/accommodation/additional-services`

- `GET`: listar servicios.
- `POST`: crear un servicio.
- `PATCH /{serviceId}`: actualizar un servicio.
- `DELETE /{serviceId}`: eliminar un servicio.
- `GET /{serviceId}/images`: consultar imágenes.
- `POST /{serviceId}/images`: agregar imágenes.
- `DELETE /{serviceId}/images/{imageId}`: eliminar una imagen.

**Métodos de pago del alojamiento**

Base: `/owners/accommodation/payment-methods`

- `GET`: listar métodos de pago.
- `POST`: crear un método de pago.
- `PATCH /{methodId}`: actualizar un método.
- `DELETE /{methodId}`: eliminar un método.

Estos endpoints también requieren autenticación y validan la pertenencia de los recursos al alojamiento del propietario.

Los contratos completos, DTOs, parámetros y ejemplos de respuesta están documentados mediante anotaciones OpenAPI en los controladores.

## 14. Modelo de datos principal

Las entidades más relevantes del módulo son:

| Entidad | Responsabilidad |
|---|---|
| `Reservation` | Información principal, fechas, importes y estados de la reserva. |
| `ReservationLink` | Tokens de onboarding, vigencia, uso e invalidación. |
| `Guest` | Perfil reutilizable del huésped. |
| `ReservationGuestInfo` | Datos personales, alergias y preferencias de una estadía. |
| `ReservationAdditionalService` | Servicios seleccionados y sus importes históricos. |
| `AdditionalService` | Catálogo de servicios ofrecidos por un alojamiento. |
| `AccommodationPaymentMethod` | Métodos de pago informativos del alojamiento. |

### Relaciones principales

```text
Accommodation
    │
    ├── Reservation
    │      │
    │      ├── ReservationLink (histórico)
    │      │
    │      ├── Guest
    │      │
    │      ├── ReservationGuestInfo
    │      │
    │      └── ReservationAdditionalService
    │
    ├── AdditionalService
    │
    └── AccommodationPaymentMethod
```

Una reserva pertenece a un alojamiento. Puede vincularse a un perfil `Guest` y mantiene su propia información histórica mediante `ReservationGuestInfo` y `ReservationAdditionalService`.

Los enlaces se conservan como registros asociados a la reserva para mantener el historial de emisiones y renovaciones.

## 15. Organización del código

Los componentes principales relacionados con el módulo se encuentran en los siguientes paquetes:

| Paquete | Responsabilidad |
|---|---|
| `controller` | Exposición de endpoints REST. |
| `service` | Reglas de negocio y coordinación de operaciones. |
| `repository` | Consultas y persistencia. |
| `model` | Entidades JPA. |
| `dto/request` | Datos de entrada y validaciones. |
| `dto/response` | Contratos de salida. |
| `enums` | Estados y tipos utilizados por el módulo. |

Clases principales:

- `ReservationService`: lógica central del ciclo de vida.
- `EmailService`: envío de correos.
- `GuestService`: resolución y gestión de perfiles de huéspedes.
- `AdditionalServiceService`: administración del catálogo de servicios.
- `CurrencyConversionService`: conversión de importes para presentación.

## 16. Consideraciones para mantenimiento

Al modificar el módulo, es importante conservar las siguientes reglas:

1. No mezclar el estado operativo de la reserva con el estado del pago.
2. No liberar fechas cuando un enlace de onboarding simplemente expira.
3. No permitir que una reserva `PENDING` o `CONFIRMED` sea ignorada en las validaciones de disponibilidad.
4. No confiar en los importes enviados por el cliente para calcular servicios adicionales.
5. No sobrescribir los snapshots históricos con cambios posteriores del catálogo o del perfil del huésped.
6. No permitir que un token utilizado, expirado o invalidado complete nuevamente el onboarding.
7. No registrar pagos desde el flujo público del huésped.
8. No asumir que `CONFIRMED` significa que el pago fue recibido.
9. No completar una reserva sin pago registrado y sin haber superado el check-out.
10. No asumir que cancelar una reserva pagada implica procesar un reembolso.
11. Mantener las verificaciones de pertenencia al propietario autenticado en todas las operaciones privadas.
12. Mantener las notificaciones posteriores al commit cuando el fallo de correo no deba revertir la operación de negocio.

### Migraciones y persistencia

El esquema de persistencia del módulo debe mantenerse alineado con las entidades y relaciones descritas.

Los cambios estructurales de base de datos deben acompañarse de las migraciones correspondientes y revisarse junto con las configuraciones de Flyway utilizadas por cada entorno.

La configuración de migraciones puede variar entre perfiles. Antes de modificarla, comprobar la configuración efectiva del entorno donde se ejecutará el cambio.

### Pruebas

Al modificar reglas del módulo, conviene cubrir al menos los siguientes comportamientos:

- Creación de reservas y validaciones de fechas, capacidad y disponibilidad.
- Solapamiento de reservas y límites entre check-in y check-out.
- Generación, consulta, expiración, renovación y uso único de enlaces.
- Reutilización de perfiles de huéspedes y conservación de snapshots.
- Selección y cálculo de servicios adicionales.
- Independencia entre estado de reserva y estado de pago.
- Registro idempotente de pagos.
- Cancelación y liberación de disponibilidad.
- Requisitos para completar una reserva.
- Autorización y pertenencia de los recursos al propietario.
- Envío de correos y comportamiento transaccional ante fallos.

## 17. Referencias de implementación

Para consultar o modificar el comportamiento, comenzar por:

- `ReservationService`
- `OwnerReservationController`
- `PublicReservationController`
- `ReservationRepository`
- `ReservationLinkRepository`
- `Reservation`
- `ReservationLink`
- `Guest`
- `ReservationGuestInfo`
- `ReservationAdditionalService`
- `ReservationStatus`
- `PaymentStatus`
- `EmailService`

Esta documentación describe el comportamiento implementado y debe actualizarse cuando cambien las reglas de negocio, los contratos API o las responsabilidades del módulo.