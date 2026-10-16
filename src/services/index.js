// Punto único de acceso a datos para la interfaz.
// Las pantallas importan `api` y nunca hablan con fetch ni con datos de prueba.
// Sin VITE_API_URL → demoApi (modo demo). Con VITE_API_URL → httpApi (backend real).
import { DEMO_MODE } from "../config/env"
import { demoApi } from "./demoApi"
import { httpApi } from "./httpApi"
import { tokenStorage } from "./tokenStorage"

const impl = DEMO_MODE ? demoApi : httpApi

// El token lo emite el backend; aquí solo se conserva para enviarlo después.
const keepToken = (result) => {
  if (result?.token) tokenStorage.set(result.token)
  return result
}

export const api = {
  login: async (credentials) => keepToken(await impl.login(credentials)),
  register: async (data) => keepToken(await impl.register(data)),
  recoverPassword: (email) => impl.recoverPassword(email),
  logout: () => tokenStorage.clear(),
  // En demo siempre hay acceso para poder recorrer la pantalla posterior al ingreso.
  isAuthenticated: () => DEMO_MODE || Boolean(tokenStorage.get()),
}
