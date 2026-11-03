// Adaptador REAL hacia el backend Java Spring Boot (se usa cuando VITE_API_URL está definida).
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
import { ENDPOINTS as E } from "./endpoints"
import { request } from "./httpClient"

const withId = (endpoint, id) => ({
  ...endpoint,
  path: endpoint.path.replace("{id}", encodeURIComponent(id)),
})

// AuthResponse → usuario interno de la app.
const toUser = (response) => {
  const roles = response?.roles || []
  return {
    id: response?.userId ?? null,
    name: response?.fullName ?? "",
    email: response?.email ?? "",
    roles,
    role: roles.some((role) => /ADMIN/i.test(role)) ? "ADMIN" : "HOST",
  }
}

const toAuth = (response) => ({
  token: response?.token ?? null,
  tokenType: response?.tokenType ?? "Bearer",
  user: toUser(response),
})

export const httpApi = {
  // ── Auth ────────────────────────────────────────────────────────────────
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

  // ── Dueños de alojamiento (JWT) ─────────────────────────────────────────
  getOwnerByEmail: (email) =>
    request({ ...E.getOwnerByEmail, path: E.getOwnerByEmail.path.replace("{email}", encodeURIComponent(email)) }, {
      name: "getOwnerByEmail",
    }),

  getOwnerById: (id) => request(withId(E.getOwnerById, id), { name: "getOwnerById" }),

  getAllOwners: () => request(E.getAllOwners, { name: "getAllOwners" }),

  toggleOwnerStatus: (id, enabled) =>
    request(
      { ...withId(E.toggleOwnerStatus, id), path: `${withId(E.toggleOwnerStatus, id).path}?enabled=${enabled}` },
      { name: "toggleOwnerStatus" },
    ),
}
