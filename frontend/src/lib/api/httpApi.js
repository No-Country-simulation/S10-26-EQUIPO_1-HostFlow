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
// La mayoría de los módulos apunta a endpoints ya implementados. Las acciones
// sin ruta de backend siguen usando el store local mediante la fachada
// src/lib/api/index.js y sus proyecciones en src/lib/api/storeOps/.
import { adminApi } from "./http/admin"
import { availabilityApi } from "./http/availability"
import { authApi } from "./http/auth"
import { bookingsApi } from "./http/bookings"
import { lodgingApi } from "./http/lodging"
import { onboardingApi } from "./http/onboarding"
import { ownersApi } from "./http/owners"
import { paymentsApi } from "./http/payments"
import { publicApi } from "./http/public"
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
  ...publicApi,
}
