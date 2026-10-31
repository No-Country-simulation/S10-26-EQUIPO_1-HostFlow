# Cotización de transporte en lancha · contrato del JSON (PROVISIONAL)

> **Estado**: pendiente de confirmación. En el repositorio **no existe** un
> contrato, modelo o especificación de la API destino de transporte
> (ni en `backend/` ni en el frontend). La estructura de abajo es un
> **propuesto** a confirmar con el equipo dueño de esa API antes de integrarla.

## Qué hace hoy el frontend

1. El huésped, durante su onboarding (`/llegada/:lodgingId/:bookingId`), elige
   **Sí, quiero la cotización** y completa los datos del traslado.
2. Al confirmar la llegada, `createBoatQuoteRequest`
   (`src/lib/state/store.js`) genera el objeto JSON con `schema:
   hostflow.boat-quote.v1` y lo guarda en la reserva (`booking.boatQuote.payload`).
3. El JSON queda visible y copiable en la pantalla del huésped y en el detalle
   de la reserva del anfitrión.

**Esto NO significa que la integración externa esté funcionando:**

- El JSON **no se envía** a ninguna API (no hay endpoint conocido).
- El **correo** de notificación **no se envía**: el `EmailService` del backend
  solo cubre restablecimiento de contraseña y no expone un endpoint para
  notificar solicitudes. El estado queda como `delivery.email: "PENDING_BACKEND"`
  y `delivery.forward: "PENDING_INTEGRATION"`.
- Ninguna de las dos dependencias está simulada con éxito falso.

## Campos del payload (`hostflow.boat-quote.v1`)

| Ruta | Campo | Tipo | Origen | Notas |
|---|---|---|---|---|
| `schema` | `schema` | string | fijo | `"hostflow.boat-quote.v1"` |
| `createdAt` | `createdAt` | ISO-8601 | frontend | Momento de la solicitud. |
| `lodging` | `id`, `name`, `location`, `phone` | string | store (`lodging`) | Alojamiento del huésped. |
| `booking` | `id`, `checkIn`, `checkOut`, `guests` | string/number | reserva | `null` si no hay reserva asociada. |
| `guest` | `name` | string | formulario onboarding | Obligatorio. |
| `guest` | `email` | string | formulario onboarding | Obligatorio; formato email. |
| `guest` | `phone` | string | formulario onboarding | Recomendado (contacto directo). |
| `trip` | `service` | string | fijo | `"lancha_remis"`. |
| `trip` | `pickupPlace` | string | formulario | Obligatorio. Lugar de embarque. |
| `trip` | `pickupDate` | `YYYY-MM-DD` | formulario (o `booking.checkIn`) | Obligatorio. |
| `trip` | `pickupTime` | `HH:mm` | formulario | Recomendado. |
| `trip` | `dropoffPlace` | string | formulario (default: `lodging.location`) | Destino. |
| `trip` | `passengers` | number ≥ 1 | formulario | Obligatorio. |
| `trip` | `baggage` | number ≥ 0 | formulario | Equipaje grande. |
| `trip` | `notes` | string | formulario | Libre. |

## Ejemplo de payload

```json
{
  "schema": "hostflow.boat-quote.v1",
  "createdAt": "2026-10-26T15:04:12.000Z",
  "lodging": {
    "id": "lod_1",
    "name": "Cabañas del Arroyo",
    "location": "Tigre, Buenos Aires",
    "phone": "+54 9 11 5555 0101"
  },
  "booking": {
    "id": "bk_2",
    "checkIn": "2026-10-29",
    "checkOut": "2026-11-02",
    "guests": 2
  },
  "guest": {
    "name": "Lucas Fernández",
    "email": "lucas@example.com",
    "phone": "+54 9 11 5555 0202"
  },
  "trip": {
    "service": "lancha_remis",
    "pickupPlace": "Terminal fluvial de Tigre",
    "pickupDate": "2026-10-29",
    "pickupTime": "09:30",
    "dropoffPlace": "Tigre, Buenos Aires",
    "passengers": 2,
    "baggage": 2,
    "notes": ""
  }
}
```

## Pasos pendientes para la integración

1. **Confirmar el contrato** definitivo con la API de transporte (nombres de
   campos, autenticación, endpoint y método). Si difiere, ajustar
   `buildBoatQuotePayload()` en `src/lib/state/store.js` (único punto de mapeo).
2. **Enviar la solicitud**: agregar la operación en
   `src/lib/api/endpoints.js` + `httpApi.js` cuando exista el endpoint real.
   Nunca exponer claves ni credenciales en el frontend.
3. **Notificación por correo**: cuando el backend exponga un endpoint de
   notificación, reemplazar `delivery.email: "PENDING_BACKEND"` por la llamada
   real. No simular el envío mientras tanto.
