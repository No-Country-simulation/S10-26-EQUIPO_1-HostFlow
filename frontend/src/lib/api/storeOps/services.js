// Servicios adicionales: proyección + fallback local.
import { actions, store } from "../../state/store"

const toService = (service) => ({
  ...service,
  id: String(service.id),
  rawId: service.id,
  price: Number(service.basePrice ?? service.price ?? 0),
  priceType: service.priceType || "TOTAL",
  active: service.active !== false,
  enabled: service.active !== false,
  pricing: service.priceType === "PER_DAY" ? "PER_NIGHT" : "FIXED",
  description: service.description || "",
  images: service.images || [],
})

export const servicesOps = {
  getServices: {
    project: (response) => {
      if (Array.isArray(response)) {
        const mapped = response.map(toService)
        actions.setServices(mapped)
      }
      return store.get().services
    },
    // Un 404 en modo real no debe llenar el panel con servicios de demostración.
    local: () => {
      actions.setServices([])
      return []
    },
  },

  addService: {
    project: (created) => {
      const item = toService(created)
      actions.setServices([...store.get().services, item])
      return item
    },
    local: (data) => actions.addService(data),
  },

  updateService: {
    send: (id, patch) => {
      const current = store.get().services.find((service) => String(service.id) === String(id)) || {}
      return [id, { ...current, ...patch }]
    },
    project: (updated, id, patch) => {
      actions.updateService(id, updated ? toService(updated) : patch)
      return store.get().services.find((sv) => String(sv.id) === String(id))
    },
    local: (id, patch) => {
      actions.updateService(id, patch)
      return store.get().services.find((sv) => String(sv.id) === String(id))
    },
  },

  removeService: {
    project: (_response, id) => actions.removeService(id),
    local: (id) => actions.removeService(id),
  },

  toggleService: {
    send: (id) => {
      const current = store.get().services.find((sv) => String(sv.id) === String(id))
      const active = !(current?.active ?? current?.enabled)
      return [id, { ...current, active, enabled: active }]
    },
    project: (updated, id, patch) => {
      actions.updateService(id, updated ? toService(updated) : { active: patch.active, enabled: patch.active })
      return store.get().services.find((sv) => String(sv.id) === String(id))
    },
    local: (id) => {
      const current = store.get().services.find((service) => String(service.id) === String(id))
      const active = !(current?.active ?? current?.enabled)
      actions.updateService(id, { active, enabled: active })
      return store.get().services.find((service) => String(service.id) === String(id))
    },
  },

  getServiceImages: {
    project: (images, serviceId) => {
      const service = store.get().services.find((item) => String(item.id) === String(serviceId))
      if (service) actions.updateService(serviceId, { images })
      return images
    },
    local: (serviceId) => store.get().services.find((item) => String(item.id) === String(serviceId))?.images || [],
  },

  addServiceImage: {
    project: (image, serviceId) => {
      const service = store.get().services.find((item) => String(item.id) === String(serviceId))
      const images = [...(service?.images || []), image]
      if (service) actions.updateService(serviceId, { images })
      return image
    },
    local: (serviceId, dataUrl) => {
      const service = store.get().services.find((item) => String(item.id) === String(serviceId))
      const images = [...(service?.images || []), { imageUrl: dataUrl }]
      if (service) actions.updateService(serviceId, { images })
      return images.at(-1)
    },
  },

  removeServiceImage: {
    project: (_response, serviceId, imageId) => {
      const service = store.get().services.find((item) => String(item.id) === String(serviceId))
      const images = (service?.images || []).filter((image) => String(image.id) !== String(imageId))
      if (service) actions.updateService(serviceId, { images })
      return images
    },
    local: (serviceId, imageId) => {
      const service = store.get().services.find((item) => String(item.id) === String(serviceId))
      const images = (service?.images || []).filter((image, index) => String(image.id ?? index) !== String(imageId))
      if (service) actions.updateService(serviceId, { images })
      return images
    },
  },
}
