// Auth (públicos): login, registro y recuperación de contraseña.
//
// Cada función corresponde a una entrada de endpoints.js. Los cuerpos y las
// respuestas están alineados con los DTOs del backend:
//   · LoginRequest          { email, password }
//   · OwnerRegisterRequest  { fullName, accommodationName, email, password, termsAccepted }
//   · ForgotPasswordRequest { email }
//   · ResetPasswordRequest  { email, code, newPassword }
//   · AuthResponse          { token, tokenType, userId, email, fullName, roles }
//
// La conversión al formato interno de la interfaz (`{ token, user }`) se hace
// AQUÍ, nunca en las pantallas.
import { ENDPOINTS as E } from "../endpoints"
import { request } from "../httpClient"
import { toAuth } from "../transforms"

export const authApi = {
  login: async (credentials) =>
    toAuth(await request(E.login, { name: "login", body: credentials, auth: false })),

  register: async (data) =>
    toAuth(
      await request(E.register, {
        name: "register",
        body: {
          fullName: data.fullName,
          accommodationName: data.accommodationName ?? data.lodgingName,
          email: data.email,
          password: data.password,
          termsAccepted: data.termsAccepted ?? true,
        },
        auth: false,
      }),
    ),

  // Devuelve { message, email, expirationMinutes }.
  forgotPassword: (email) =>
    request(E.forgotPassword, { name: "forgotPassword", body: { email }, auth: false }),

  // El backend espera `code` (6 dígitos) y `newPassword`.
  resetPassword: ({ email, code, newPassword }) =>
    request(E.resetPassword, {
      name: "resetPassword",
      body: { email, code, newPassword },
      auth: false,
    }),
}
