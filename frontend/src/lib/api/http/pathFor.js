// Rellena los placeholders de una ruta: pathFor(E.removeImage, { id })
//   → "/owners/accommodation/gallery/12"
export const pathFor = (endpoint, params = {}) => ({
  ...endpoint,
  path: Object.entries(params).reduce(
    (path, [key, value]) => path.replace(`{${key}}`, encodeURIComponent(String(value))),
    endpoint.path,
  ),
})
