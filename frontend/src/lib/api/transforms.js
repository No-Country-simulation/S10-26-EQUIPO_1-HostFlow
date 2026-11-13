// Transformaciones PURAS de los DTO del backend al formato interno de la
// interfaz. No tocan el store: todo lo que necesita el estado local vive en
// storeOps.js, y la composición (respuesta exitosa vs. endpoint pendiente)
// se resuelve en src/lib/api/index.js.

// AuthResponse → usuario interno de la app.
export const toUser = (response) => {
  const roles = response?.roles || []
  return {
    id: response?.userId ?? null,
    name: response?.fullName ?? "",
    email: response?.email ?? "",
    roles,
    role: roles.some((role) => /ADMIN/i.test(role)) ? "ADMIN" : "HOST",
  }
}

export const toAuth = (response) => ({
  token: response?.token ?? null,
  tokenType: response?.tokenType ?? "Bearer",
  user: toUser(response),
})

// OwnerResponse → fila del módulo de administración. El backend de dueños no
// expone la cantidad de habitaciones, así que queda en null y la tabla la
// muestra como "—" en lugar de inventar un número.
export const toAdminLodging = (owner) => ({
  id: String(owner.id),
  name: owner.accommodationName || "Alojamiento sin nombre",
  owner: owner.fullName || "Sin nombre",
  ownerEmail: owner.email || "",
  rooms: null,
  status: owner.enabled ? "ACTIVE" : "PAUSED",
  enabled: Boolean(owner.enabled),
  createdAt: owner.createdAt,
  roles: owner.roles || [],
})

// Convierte un dataUrl base64 o Blob/File a File real para subidas multipart a Spring Boot
export function toFile(fileOrDataUrl, defaultFilename = "upload.jpg") {
  if (!fileOrDataUrl) return null
  if (typeof File !== "undefined" && fileOrDataUrl instanceof File) return fileOrDataUrl
  if (typeof Blob !== "undefined" && fileOrDataUrl instanceof Blob) {
    return new File([fileOrDataUrl], defaultFilename, { type: fileOrDataUrl.type || "image/jpeg" })
  }
  if (typeof fileOrDataUrl === "string" && fileOrDataUrl.startsWith("data:")) {
    const parts = fileOrDataUrl.split(",")
    const mimeMatch = parts[0].match(/:(.*?);/)
    const mime = mimeMatch ? mimeMatch[1] : "image/jpeg"
    const bstr = atob(parts[1])
    let n = bstr.length
    const u8arr = new Uint8Array(n)
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n)
    }
    return new File([u8arr], defaultFilename, { type: mime })
  }
  return null
}
