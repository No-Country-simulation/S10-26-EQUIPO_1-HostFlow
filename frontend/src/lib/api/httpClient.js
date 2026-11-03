import { API_URL } from "../config/env"
import { ApiError, ApiNotConfiguredError } from "./errors"
import { tokenStorage } from "./tokenStorage"

// Se emite cuando una llamada autenticada recibe 401: la sesión venció o el token es inválido.
export const AUTH_EXPIRED_EVENT = "hostflow:auth-expired"

// Cliente fetch mínimo (JSON). `auth: true` agrega `Authorization: Bearer <token>`.
// Las 3 operaciones de acceso (login, registro, recuperar) usan `auth: false`.
export async function request(endpoint, { name = "endpoint", body, auth = true } = {}) {
  if (!API_URL || !endpoint?.path) throw new ApiNotConfiguredError(name)

  const headers = { Accept: "application/json" }
  if (body) headers["Content-Type"] = "application/json"
  const token = auth ? tokenStorage.get() : null
  if (token) headers.Authorization = `Bearer ${token}`

  let response
  try {
    response = await fetch(API_URL + endpoint.path, {
      method: endpoint.method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError("No se pudo conectar con el servidor.", { status: 0 })
  }

  let data = null
  if (response.status !== 204) {
    try {
      data = await response.json()
    } catch {
      data = null
    }
  }

  if (!response.ok) {
    if (response.status === 401 && auth) {
      tokenStorage.clear()
      window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT))
    }
    throw new ApiError(data?.message || `Error ${response.status}`, { status: response.status, data })
  }
  return data
}
