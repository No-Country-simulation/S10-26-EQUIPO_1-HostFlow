// Alojamiento del dueño autenticado (datos, marca, logo y fotos).
import { ENDPOINTS as E } from "../endpoints"
import { request } from "../httpClient"
import { pathFor } from "./pathFor"
import { toFile } from "../transforms"

export const lodgingApi = {
  getLodging: () => request(E.getLodging, { name: "getLodging" }),
  getGallery: () => request(E.getGallery, { name: "getGallery" }),

  // Guarda los datos editados en el panel de Alojamiento.
  updateLodging: async (patch) => {
    const body = {}
    if (patch.name !== undefined) body.name = patch.name
    if (patch.rooms !== undefined) body.roomCount = Number(patch.rooms)
    if (patch.bathrooms !== undefined) body.bathroomCount = Number(patch.bathrooms)
    if (patch.capacity !== undefined) body.guestCapacity = Number(patch.capacity)
    if (patch.pricePerNight !== undefined) body.basePrice = Number(patch.pricePerNight)
    if (patch.location !== undefined) body.address = patch.location
    if (patch.phone !== undefined) body.phone = patch.phone
    if (patch.tagline !== undefined) body.welcomeMessage = patch.tagline

    if (patch.brand?.primary) body.primaryColor = patch.brand.primary
    if (patch.brand?.accent) body.accentColor = patch.brand.accent
    if (patch.brand?.tertiary) body.tertiaryColor = patch.brand.tertiary
    if (patch.brand?.radius) body.backgroundPattern = patch.brand.radius

    try {
      return await request(E.updateLodging, { name: "updateLodging", body })
    } catch (err) {
      if (err?.status === 404) {
        return await request(E.createLodging, {
          name: "createLodging",
          body: {
            name: patch.name || "Mi alojamiento",
            roomCount: Number(patch.rooms ?? 1),
            bathroomCount: Number(patch.bathrooms ?? 1),
            guestCapacity: Number(patch.capacity ?? (patch.rooms ? patch.rooms * 2 : 2)),
            basePrice: Number(patch.pricePerNight ?? 85000),
            primaryColor: patch.brand?.primary || "#0fa3a3",
            accentColor: patch.brand?.accent || "#d4f53c",
            backgroundPattern: patch.brand?.radius || "rounded",
            welcomeMessage: patch.tagline || "",
            address: patch.location || "",
            phone: patch.phone || "",
          },
        })
      }
      throw err
    }
  },

  // Marca del alojamiento (colores, forma del borde y patrón del enlace).
  updateBrand: (patch) => {
    const body = {}
    if (patch.primary) body.primaryColor = patch.primary
    if (patch.accent) body.accentColor = patch.accent
    if (patch.tertiary) body.tertiaryColor = patch.tertiary
    if (patch.radius) body.backgroundPattern = patch.radius
    return request(E.updateBrand, { name: "updateBrand", body })
  },

  // Logo: sube el archivo a Cloudinary via multipart
  setLogo: async (fileOrDataUrl) => {
    if (!fileOrDataUrl) {
      return request(E.updateLodging, { name: "updateLodging", body: { logoUrl: "" } })
    }
    const file = toFile(fileOrDataUrl, "logo.png")
    if (!file) return null
    const formData = new FormData()
    formData.append("file", file)
    return request(E.setLogo, { name: "setLogo", body: formData })
  },

  // Galería: sube foto a Cloudinary
  addImage: async (fileOrDataUrl) => {
    const file = toFile(fileOrDataUrl, "gallery.jpg")
    if (!file) return null
    const formData = new FormData()
    formData.append("file", file)
    return request(E.addImage, { name: "addImage", body: formData })
  },

  removeImage: (idOrIndex) =>
    request(pathFor(E.removeImage, { id: idOrIndex }), { name: "removeImage" }),
}
