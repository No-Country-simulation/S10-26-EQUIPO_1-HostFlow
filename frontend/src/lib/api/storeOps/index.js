// Operaciones sobre el store local que usa la fachada (src/lib/api/index.js).
//
// El adaptador remoto (httpApi.js) solo habla HTTP. Este módulo aporta las
// mitades que sí necesitan el estado de la app, y por operación puede traer:
//
//   · `send`    → ajusta los argumentos ANTES del request, cuando el backend
//                 necesita datos que salen del store (p. ej. `blocked` o
//                 `lodgingId`).
//   · `project` → se ejecuta cuando el backend RESPONDE: escribe la respuesta
//                 en el store (las pantallas se suscriben con `useStore` y no
//                 guardan el resultado de las llamadas) y devuelve el valor
//                 que promete la API.
//   · `local`   → se ejecuta cuando el endpoint TODAVÍA NO EXISTE (404 / 405 /
//                 501 o sin conexión): reproduce la operación en memoria.
//
// La DECISIÓN de cuándo usar uno u otro es exclusiva de index.js; acá solo
// vive el cuerpo de cada caso. Los helpers que necesitan leer el estado están
// en storeOps/shared.js.
import { adminOps } from "./admin"
import { availabilityOps } from "./availability"
import { bookingsOps } from "./bookings"
import { lodgingOps } from "./lodging"
import { onboardingOps } from "./onboarding"
import { paymentsOps } from "./payments"
import { servicesOps } from "./services"

export const storeOps = {
  ...lodgingOps,
  ...availabilityOps,
  ...bookingsOps,
  ...onboardingOps,
  ...servicesOps,
  ...paymentsOps,
  ...adminOps,
}
