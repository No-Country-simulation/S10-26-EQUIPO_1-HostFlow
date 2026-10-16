// Adaptador REAL hacia el backend Java Spring Boot (se usa cuando VITE_API_URL está definida).
// Cada función corresponde a una entrada de endpoints.js.
//
// ── Qué necesita el frontend del backend (todo por CONFIRMAR) ────────────────────────────
// Convenciones generales a acordar:
//   • Cuerpo y respuesta en JSON. Los errores deberían traer `{ "message": "..." }`: ese
//     texto se muestra al usuario (errors.js). Sin `message`, se usa un texto genérico.
//   • Sesión: hoy el frontend asume un token en el cuerpo de la respuesta (`{ token }`),
//     lo guarda en sessionStorage y lo envía como `Authorization: Bearer <token>`.
//     PENDIENTE decidir: Bearer vs cookie httpOnly (si es cookie, agregar
//     `credentials: "include"` en httpClient.js y dejar de usar tokenStorage),
//     expiración/refresh del token y CORS del origen del frontend.
//
// ── login ────────────────────────────────────────────────────────────────────────────────
//   Entrada:   { email, password }
//   Respuesta: { token }  (la interfaz solo necesita el token para considerar iniciada la sesión)
//   Errores:   401 → la interfaz muestra "Correo o contraseña incorrectos."
//   PENDIENTE: ruta, datos del usuario en la respuesta (si se requieren), roles, bloqueo
//              por intentos fallidos.
//
// ── register ─────────────────────────────────────────────────────────────────────────────
//   Entrada:   { fullName, lodgingName, email, password }  (contraseña mínima validada en
//              la interfaz: 8 caracteres; el backend debe validar sus propias reglas)
//   Respuesta: { token }  → hoy la interfaz inicia sesión automáticamente tras registrar.
//   Errores:   cualquier error con `message` se muestra tal cual (ej. correo ya registrado).
//   PENDIENTE: ruta, código de error para correo duplicado, si el registro inicia sesión
//              automáticamente o requiere verificar el correo, qué se crea con
//              `lodgingName`, y si los términos se registran en el servidor (la interfaz solo
//              exige el checkbox, no envía su valor).
//
// ── recoverPassword ──────────────────────────────────────────────────────────────────────
//   Entrada:   { email }
//   Respuesta: éxito sin contenido relevante (200/204). La interfaz muestra siempre
//              "Si la cuenta existe, vas a recibir instrucciones", por lo que se recomienda
//              que el backend responda igual exista o no el correo.
//   PENDIENTE: ruta, envío del correo, y TODO el flujo posterior (enlace con token,
//              pantalla para definir la nueva contraseña y su endpoint). Esa pantalla y
//              ese endpoint NO existen aún en el frontend.
//
// ── Fuera de alcance / pendiente de definir ──────────────────────────────────────────────
//   • Cierre de sesión en el servidor (hoy `logout` solo borra el token local).
//   • Endpoint de "usuario actual" para validar/restaurar la sesión al recargar.
//   • Cambio de contraseña estando autenticado, verificación de correo, 2FA, roles.
//
// Si el JSON del backend difiere de lo descrito, la conversión se hace AQUÍ, no en las pantallas.
import { ENDPOINTS as E } from "./endpoints"
import { request } from "./httpClient"

export const httpApi = {
  login: (credentials) => request(E.login, { name: "login", body: credentials, auth: false }),
  register: (data) => request(E.register, { name: "register", body: data, auth: false }),
  recoverPassword: (email) =>
    request(E.recoverPassword, { name: "recoverPassword", body: { email }, auth: false }),
}
