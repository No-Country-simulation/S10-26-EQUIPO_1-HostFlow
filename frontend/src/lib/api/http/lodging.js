// Alojamiento del dueño autenticado (datos, marca, logo y fotos).
import { ENDPOINTS as E } from "../endpoints"
import { request } from "../httpClient"
import { pathFor } from "./pathFor"

export const lodgingApi = {
  getLodging: () => request(E.getLodging, { name: "getLodging" }),

  // Guarda los datos editados en el panel de Alojamiento.
  updateLodging: (patch) => request(E.updateLodging, { name: "updateLodging", body: patch }),

  // Marca del alojamiento (colores, forma del borde y patrón del enlace).
  updateBrand: (patch) => request(E.updateBrand, { name: "updateBrand", body: patch }),

  // Logo y fotos: el backend guarda la referencia; el front sube dataURL.
  setLogo: (dataUrl) => request(E.setLogo, { name: "setLogo", body: { image: dataUrl } }),

  addImage: (dataUrl) => request(E.addImage, { name: "addImage", body: { image: dataUrl } }),

  removeImage: (index) => request(pathFor(E.removeImage, { index }), { name: "removeImage" }),
}
