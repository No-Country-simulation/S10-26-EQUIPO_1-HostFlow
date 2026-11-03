// Almacén del token de sesión emitido por el backend.
// El frontend NO genera ni interpreta tokens: solo guarda el que entrega el backend
// y lo envía en el encabezado Authorization (ver httpClient.js).
// Nota: si el backend usa cookie httpOnly, este almacén no se usa y bastará con
// agregar `credentials: "include"` en httpClient.js.
const KEY = "hostflow.auth.token"
let memoryToken = null

export const tokenStorage = {
  get() {
    if (memoryToken) return memoryToken
    try {
      return sessionStorage.getItem(KEY)
    } catch {
      return null
    }
  },
  set(token) {
    memoryToken = token
    try {
      sessionStorage.setItem(KEY, token)
    } catch {
      /* el almacenamiento puede no estar disponible; queda en memoria */
    }
  },
  clear() {
    memoryToken = null
    try {
      sessionStorage.removeItem(KEY)
    } catch {
      /* noop */
    }
  },
}
