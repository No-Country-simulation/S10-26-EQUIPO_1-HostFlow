// Adaptador REAL hacia el backend Java Spring Boot (se usa cuando VITE_API_URL está definida).
//
// ── Capa PURA de HTTP ──────────────────────────────────────────────────────
// Cada función corresponde a una entrada de endpoints.js y vive en su módulo
// de dominio (http/auth.js, http/lodging.js, http/bookings.js, …). Solo
// ejecuta el request y devuelve la respuesta ya traducida al formato interno
// de la interfaz: ESTE ARCHIVO NO IMPORTA NI ESCRIBE EL STORE.
//
//   · LoginRequest          { email, password }
//   · OwnerRegisterRequest  { fullName, accommodationName, email, password, termsAccepted }
//   · ForgotPasswordRequest { email }
//   · ResetPasswordRequest  { email, code, newPassword }
//   · AuthResponse          { token, tokenType, userId, email, fullName, roles }
//
// La conversión al formato interno de la interfaz (`{ token, user }`) se hace
// AQUÍ, nunca en las pantallas.
//
// ── Módulos todavía no implementados en el backend ─────────────────────────
// Auth y Owners existen; el resto (alojamiento, disponibilidad, reservas,
// servicios, pagos y estadísticas de administración) está RESERVADO en el
// contrato. Quién detecta que el endpoint aún no responde (404 / 405 / 501, o
// falta de conexión), cómo se cae al store local y cómo se proyecta cada
// respuesta exitosa en el store vive en src/lib/api/index.js (la fachada, único
// punto que resuelve el fallback) junto con src/lib/api/storeOps.js. De ese
// modo NINGUNA pantalla se rompe y, cuando el backend publique los endpoints,
// la app pasa a usarlos sin tocar las pantallas.
import { adminApi } from "./http/admin"
import { availabilityApi } from "./http/availability"
import { authApi } from "./http/auth"
import { bookingsApi } from "./http/bookings"
import { lodgingApi } from "./http/lodging"
import { onboardingApi } from "./http/onboarding"
import { ownersApi } from "./http/owners"
import { paymentsApi } from "./http/payments"
import { servicesApi } from "./http/services"

export const httpApi = {
  ...authApi,
  ...ownersApi,
  ...lodgingApi,
  ...availabilityApi,
  ...bookingsApi,
  ...onboardingApi,
  ...servicesApi,
  ...paymentsApi,
  ...adminApi,
}
