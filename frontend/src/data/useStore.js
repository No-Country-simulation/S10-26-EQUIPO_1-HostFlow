import { useSyncExternalStore } from "react";
import { store } from "./store";

// Subscripción reactiva al store de aplicación.
// `selector` se aplica fuera del snapshot para no romper la igualdad por referencia.
export function useStore(selector = (s) => s) {
  const state = useSyncExternalStore(store.subscribe, store.get, store.get);
  return selector(state);
}
