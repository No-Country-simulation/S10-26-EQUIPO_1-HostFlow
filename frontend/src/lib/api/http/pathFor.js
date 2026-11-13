// Rellena los placeholders de la ruta: pathFor(E.toggleBlockedDay, { date })
//   → "/lodgings/mine/blocked-days/2026-10-30"
export const pathFor = (endpoint, params = {}) => ({
  ...endpoint,
  path: Object.entries(params).reduce(
    (path, [key, value]) => path.replace(`{${key}}`, encodeURIComponent(String(value))),
    endpoint.path,
  ),
})
