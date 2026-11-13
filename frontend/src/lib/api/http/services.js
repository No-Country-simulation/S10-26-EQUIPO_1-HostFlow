// Servicios adicionales del alojamiento.
import { ENDPOINTS as E } from "../endpoints"
import { request } from "../httpClient"
import { pathFor } from "./pathFor"
import { toFile } from "../transforms"

const serviceBody = (data) => ({
  name: data.name,
  basePrice: Number(data.price ?? data.basePrice ?? 0),
  priceType: data.priceType || (data.pricing === "PER_NIGHT" ? "PER_DAY" : "TOTAL"),
  active: data.active !== false,
  description: data.description || "",
})

export const servicesApi = {
  getServices: async () => {
    const list = await request(E.getServices, { name: "getServices" })
    return Array.isArray(list)
      ? list.map((s) => ({
          id: String(s.id),
          rawId: s.id,
          name: s.name,
          price: Number(s.basePrice ?? s.price ?? 0),
          priceType: s.priceType || "TOTAL",
          active: s.active !== false,
          enabled: s.active !== false,
          description: s.description || "",
          images: s.images || [],
        }))
      : []
  },

  addService: (data) =>
    request(E.addService, {
      name: "addService",
      body: serviceBody(data),
    }),

  updateService: (id, patch) =>
    request(pathFor(E.updateService, { id }), {
      name: "updateService",
      body: serviceBody(patch),
    }),

  removeService: (id) => request(pathFor(E.removeService, { id }), { name: "removeService" }),

  toggleService: (id, patch) =>
    request(pathFor(E.toggleService, { id }), {
      name: "toggleService",
      body: serviceBody(patch),
    }),

  getServiceImages: (serviceId) =>
    request(pathFor(E.getServiceImages, { serviceId }), { name: "getServiceImages" }),

  addServiceImage: async (serviceId, fileOrDataUrl) => {
    const file = toFile(fileOrDataUrl, "service-image.jpg")
    if (!file) return null
    const body = new FormData()
    body.append("file", file)
    return request(pathFor(E.addServiceImage, { serviceId }), { name: "addServiceImage", body })
  },

  removeServiceImage: (serviceId, imageId) =>
    request(pathFor(E.removeServiceImage, { serviceId, imageId }), { name: "removeServiceImage" }),
}
