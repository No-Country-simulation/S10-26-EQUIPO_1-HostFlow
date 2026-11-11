import { API_URL } from "../config/env"
import { ApiError, ApiNotConfiguredError } from "./errors"
import { tokenStorage } from "./tokenStorage"

// Se emite cuando una llamada autenticada recibe 401: la sesión venció o el token es inválido.
export const AUTH_EXPIRED_EVENT = "hostflow:auth-expired"

// Cliente fetch estándar.
// Soporta JSON y FormData (para subida de archivos multipart como fotos o logos).
// `auth: true` agrega `Authorization: Bearer <token>`.
export async function request(endpoint, { name = "endpoint", body, auth = true } = {}) {
  if (!API_URL || !endpoint?.path) throw new ApiNotConfiguredError(name)

  const headers = { Accept: "application/json" }
  const isFormData = typeof FormData !== "undefined" && body instanceof FormData

  // Si no es FormData, enviamos JSON
  if (body && !isFormData) {
    headers["Content-Type"] = "application/json"
  }

  const token = auth ? tokenStorage.get() : null
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  let response
  try {
    response = await fetch(API_URL + endpoint.path, {
      method: endpoint.method,
      headers,
      body: isFormData ? body : (body ? JSON.stringify(body) : undefined),
    })
  } catch {
    throw new ApiError("No se pudo conectar con el servidor backend.", { status: 0 })
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

    const message =
      data?.message ||
      data?.error ||
      (Array.isArray(data?.errors) ? data.errors.map((e) => e.defaultMessage || e).join(", ") : null) ||
      (typeof data === "string" ? data : `Error ${response.status}`)

    throw new ApiError(message, { status: response.status, data })
  }

  return data
}
