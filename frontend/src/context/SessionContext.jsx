import { createContext, useContext, useMemo } from "react";
import { api } from "../services";
import { useStore } from "../data/useStore";

const SessionContext = createContext(null);

export function SessionProvider({ children }) {
  const user = useStore((s) => s.user);

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isAdmin: user?.role === "ADMIN",
      // `actions` expone el servicio: en modo demo opera sobre el store reactivo;
      // con backend real apunta a httpApi. Las pantallas no conocen la diferencia.
      actions: api,
      logout: api.logout,
    }),
    [user],
  );

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("useSession debe usarse dentro de <SessionProvider>");
  return ctx;
}
