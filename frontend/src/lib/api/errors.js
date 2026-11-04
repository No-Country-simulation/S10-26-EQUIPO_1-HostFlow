// Errores de la capa de servicios y su traducción a texto para el usuario.

// El endpoint todavía no tiene `path` en endpoints.js (contrato pendiente).
export class ApiNotConfiguredError extends Error {
  constructor(name) {
    super(`Endpoint "${name}" sin configurar (ver src/lib/api/endpoints.js).`)
    this.name = "ApiNotConfiguredError"
  }
}

// Respuesta HTTP no exitosa (status > 0) o fallo de red (status 0).
export class ApiError extends Error {
  constructor(message, { status = 0, data = null } = {}) {
    super(message)
    this.name = "ApiError"
    this.status = status
    this.data = data
  }
}

// Texto amigable para mostrar al usuario a partir de cualquier error de servicio.
export function describeError(error, fallback = "No pudimos completar la operación. Intentá nuevamente.") {
  if (error instanceof ApiNotConfiguredError)
    return "La conexión con el servidor todavía no está configurada."
  if (error instanceof ApiError) {
    if (error.status === 0) return "No hay conexión con el servidor. Revisá tu internet e intentá de nuevo."
    if (error.status === 401) return "Tu sesión venció. Ingresá nuevamente."
    if (error.status >= 500) return "El servidor tuvo un problema. Intentá de nuevo en unos minutos."
    if (error.data?.message) return error.data.message
  }
  return fallback
}
