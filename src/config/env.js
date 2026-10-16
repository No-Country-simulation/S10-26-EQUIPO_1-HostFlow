// Configuración de entorno.
// VITE_API_URL: URL base del backend Java Spring Boot (ej. https://api.midominio.com).
// Si NO está definida, la app corre en MODO DEMO: usa datos de prueba en memoria
// (src/services/demoApi.js) y lo informa en pantalla. Nada se envía a ningún servidor.
export const API_URL = (import.meta.env.VITE_API_URL || "").replace(/\/+$/, "")
export const DEMO_MODE = !API_URL
