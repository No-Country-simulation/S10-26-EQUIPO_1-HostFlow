// Muestras sugeridas: colores con vida, sin negro ni grises (no aportan
// identidad y ensucian la marca del alojamiento).
export const COLOR_PRESETS = [
  { name: "Teal", value: "#0fa3a3" },
  { name: "Lima", value: "#d4f53c" },
  { name: "Rosa", value: "#f4b8c4" },
  { name: "Azul", value: "#3b82f6" },
  { name: "Violeta", value: "#8b5cf6" },
  { name: "Terracota", value: "#e07a5f" },
];

// Los 3 colores tienen un ROL fijo: el dueño elige el color, no qué pinta.
export const SLOTS = [
  { id: "primary", label: "Marca", where: "Cabecera y portada del enlace, logo y datos." },
  { id: "accent", label: "Acción", where: "Botones, precio y servicios seleccionados." },
  { id: "tertiary", label: "Fondo", where: "Superficie del enlace y acentos suaves." },
];

// Umbral de jerarquía: por debajo de esta distancia (0-255) dos colores se
// perciben casi iguales y la marca pierde jerarquía visual.
export const MIN_DISTANCE = 60;
export const UNDO_MS = 6000;
