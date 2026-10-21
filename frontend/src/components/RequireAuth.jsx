import { Navigate, Outlet, useLocation } from "react-router"
import { useSession } from "../context/SessionContext"

// Protege rutas privadas: sin sesión redirige al login, y si el backend responde 401
// en una llamada autenticada, también vuelve al login.
export default function RequireAuth({ adminOnly = false }) {
  const { isAuthenticated, isAdmin } = useSession()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/auth/login" state={{ from: location.pathname }} replace />
  }

  if (adminOnly && !isAdmin) {
    return <Navigate to="/panel" replace />
  }

  return <Outlet />
}
