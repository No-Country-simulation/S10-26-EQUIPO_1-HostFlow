// Adaptador de DEMOSTRACIÓN (se usa cuando VITE_API_URL NO está definida).
// Implementa la misma interfaz que httpApi.js, pero sin red: NO valida credenciales,
// NO guarda usuarios, NO envía nada a un servidor y NO emite tokens reales.
const delay = (ms = 350) => new Promise((resolve) => setTimeout(resolve, ms))

export const demoApi = {
  login: async () => {
    await delay()
    return { token: "demo-token", demo: true }
  },
  register: async () => {
    await delay()
    return { token: "demo-token", demo: true }
  },
  recoverPassword: async () => {
    await delay()
    return { demo: true }
  },
}
