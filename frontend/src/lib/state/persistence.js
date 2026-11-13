// Persistencia de la demo en localStorage.
//
// Aislada del núcleo pub/sub para que store.js solo se ocupe del estado y de
// las acciones. Si el navegador bloquea el almacenamiento, todo sigue
// funcionando en memoria (no se rompe la app).
import { normalizeStored } from "./normalizeState";
import { buildInitialState } from "./seedData";

export const STORAGE_KEY = "hostflow.app.v1";

export function loadState() {
  try {
    let raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      // Migración desde la versión anterior (sessionStorage por pestaña).
      const legacy = sessionStorage.getItem(STORAGE_KEY);
      if (legacy) {
        localStorage.setItem(STORAGE_KEY, legacy);
        raw = legacy;
      }
    }
    if (raw) return normalizeStored(JSON.parse(raw), buildInitialState());
  } catch {
    /* almacenamiento no disponible */
  }
  return buildInitialState();
}

export function persistState(state) {
  try {
    const { pendingOtp, lastOtp, ...persistable } = state;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(persistable));
  } catch {
    /* noop */
  }
}

// Otra pestaña/ventana de ESTE navegador cambió el estado → se lo notificamos al
// store para que el enlace de llegada abierto en paralelo vea los cambios del
// anfitrión (pago aceptado, nuevas reservas, marca, etc.) sin recargar.
export function watchStorage(onChange) {
  try {
    const handler = (event) => {
      if (event.key !== STORAGE_KEY || !event.newValue) return;
      try {
        // normalizeStored sin re-persistir: evita ciclos entre pestañas.
        onChange(normalizeStored(JSON.parse(event.newValue), buildInitialState()));
      } catch {
        /* JSON inválido: ignorar */
      }
    };
    window.addEventListener("storage", handler);
    return () => window.removeEventListener("storage", handler);
  } catch {
    /* entorno sin window (SSR/tests) */
    return () => {};
  }
}
