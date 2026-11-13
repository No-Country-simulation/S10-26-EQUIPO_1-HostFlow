// Alojamiento: proyección de la respuesta en el store + fallback local.
import { actions, store } from "../../state/store"
import { MAX_IMAGES } from "../../domain/lodging"
import { isEndpointUnavailable } from "../errors"
import { httpApi } from "../httpApi"
import { mergeLodging } from "./shared"

export const lodgingOps = {
  getGallery: {
    project: (gallery) => {
      const images = Array.isArray(gallery) ? gallery.slice(0, MAX_IMAGES) : []
      actions.updateLodging({
        galleryImages: images,
        images: images.map((image) => image.imageUrl || image),
      })
      return images
    },
    local: () => store.get().lodging.galleryImages || [],
  },

  getLodging: {
    project: (lodging) => {
      actions.updateLodging(mergeLodging(lodging))
      return store.get().lodging
    },
    // Lee /lodgings/mine. Mientras ese endpoint no exista, completa lo único
    // que el backend sí sabe (nombre del alojamiento y titularidad) desde el
    // dueño.
    local: async () => {
      const email = store.get().user?.email
      if (email) {
        try {
          const owner = await httpApi.getOwnerByEmail(email)
          actions.updateLodging(
            mergeLodging({
              name: owner?.accommodationName,
              owner: owner?.fullName,
              ownerEmail: owner?.email,
            }),
          )
        } catch (error) {
          if (!isEndpointUnavailable(error)) throw error
        }
      }
      return store.get().lodging
    },
  },

  updateLodging: {
    // El backend responde el alojamiento completo: lo adoptamos entero.
    project: (updated, patch) => {
      actions.updateLodging(mergeLodging({ ...patch, ...updated }))
      return store.get().lodging
    },
    local: (patch) => {
      actions.updateLodging(patch)
      return store.get().lodging
    },
  },

  updateBrand: {
    project: (_response, patch) => {
      actions.updateBrand(patch)
      return store.get().lodging.brand
    },
    local: (patch) => {
      actions.updateBrand(patch)
      return store.get().lodging.brand
    },
  },

  setLogo: {
    project: (response, dataUrl) => {
      const logoUrl = response?.logoUrl || response?.logo || dataUrl
      actions.setLogo(logoUrl)
      return logoUrl
    },
    local: (dataUrl) => {
      actions.setLogo(dataUrl)
      return dataUrl
    },
  },

  addImage: {
    // Fotos del alojamiento: máximo MAX_IMAGES; la primera es la portada.
    project: (res, dataUrl) => {
      const url = res?.imageUrl || (typeof res === "string" ? res : dataUrl)
      const current = store.get().lodging
      const galleryImages = [...(current.galleryImages || []), ...(res?.imageUrl ? [res] : [])].slice(0, MAX_IMAGES)
      actions.updateLodging({
        images: [...(current.images || []), url].filter(Boolean).slice(0, MAX_IMAGES),
        galleryImages,
      })
      return store.get().lodging.images
    },
    local: (dataUrl) => {
      actions.addImage(dataUrl)
      return store.get().lodging.images
    },
  },

  removeImage: {
    send: (indexOrId) => {
      if (typeof indexOrId === "number") {
        const imgObj = store.get().lodging.galleryImages?.[indexOrId]
        if (imgObj?.id) return [imgObj.id]
      }
      return [indexOrId]
    },
    project: (res, indexOrId) => {
      if (Array.isArray(res?.images)) {
        actions.updateLodging({ images: res.images.slice(0, MAX_IMAGES) })
      } else if (typeof indexOrId === "number") {
        const galleryImages = (store.get().lodging.galleryImages || []).filter((_, index) => index !== indexOrId)
        actions.removeImage(indexOrId)
        actions.updateLodging({ galleryImages })
      } else {
        const idx = store.get().lodging.galleryImages?.findIndex((img) => String(img.id) === String(indexOrId))
        const removeAt = idx !== -1 && idx !== undefined ? idx : 0
        const galleryImages = (store.get().lodging.galleryImages || []).filter((_, index) => index !== removeAt)
        actions.removeImage(removeAt)
        actions.updateLodging({ galleryImages })
      }
      return store.get().lodging.images
    },
    local: (index) => {
      actions.removeImage(typeof index === "number" ? index : 0)
      return store.get().lodging.images
    },
  },
}
