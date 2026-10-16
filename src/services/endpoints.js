// CONTRATO DE API DE AUTENTICACIÓN — PENDIENTE DE DEFINICIÓN CON EL BACKEND (Java Spring Boot).
//
// No se inventaron rutas. Cada endpoint tiene `path: null` hasta que el contrato real
// exista. Mientras sea null, el cliente lanza ApiNotConfiguredError y la interfaz muestra
// "La conexión con el servidor todavía no está configurada" (nunca finge una respuesta).
//
// Para completar: reemplazar `path` (ej. "/algo/login") y ajustar `method` si hace falta.
// Qué datos envía y espera el frontend en cada uno: ver httpApi.js.
export const ENDPOINTS = {
  login: { method: "POST", path: null },
  register: { method: "POST", path: null },
  recoverPassword: { method: "POST", path: null },
}
