// Alojamiento: proyección de la respuesta en el store + fallback local.
import { actions, store } from "../../state/store"
import { MAX_IMAGES } from "../../domain/lodging"
import { isEndpointUnavailable } from "../errors"
import { httpApi } from "../httpApi"
import { mergeLodging } from "./shared"

export const lodgingOps = {
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
    project: (_response, dataUrl) => {
      actions.setLogo(dataUrl)
      return dataUrl
    },
    local: (dataUrl) => {
      actions.setLogo(dataUrl)
      return dataUrl
    },
  },

  addImage: {
    // Fotos del alojamiento: máximo MAX_IMAGES; la primera es la portada.
    project: (lodging, dataUrl) => {
      if (Array.isArray(lodging?.images))
        actions.updateLodging({ images: lodging.images.slice(0, MAX_IMAGES) })
      else actions.addImage(dataUrl)
      return store.get().lodging.images
    },
    local: (dataUrl) => {
      actions.addImage(dataUrl)
      return store.get().lodging.images
    },
  },

  removeImage: {
    project: (lodging, index) => {
      if (Array.isArray(lodging?.images))
        actions.updateLodging({ images: lodging.images.slice(0, MAX_IMAGES) })
      else actions.removeImage(index)
      return store.get().lodging.images
    },
    local: (index) => {
      actions.removeImage(index)
      return store.get().lodging.images
    },
  },
}
