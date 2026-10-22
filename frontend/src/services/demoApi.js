// Adaptador de DEMOSTRACIÓN (se usa cuando VITE_API_URL NO está definida).
// Implementa el MISMO contrato que httpApi.js, pero en memoria: no valida credenciales
// contra un servidor, no envía nada a la red y no emite tokens reales.
// Las operaciones mutan el store reactivo de src/data/store.js.
import { actions, store } from "../data/store"

export const demoApi = {
  ...actions,
  logout: actions.logout,
  isAuthenticated: () => Boolean(store.get().user),
}
