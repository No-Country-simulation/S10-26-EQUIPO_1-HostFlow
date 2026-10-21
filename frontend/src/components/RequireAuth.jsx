import { useEffect } from "react"
import { Navigate, Outlet, useLocation, useNavigate } from "react-router"
import { useSession } from "../context/SessionContext"
import { AUTH_EXPIRED_EVENT } from "../services/httpClient"

// Protege rutas privadas: sin sesión redirige al login, y si el backend responde 401
// en una llamada autenticada, también vuelve al login.
export default function RequireAuth({ adminOnly = false }) {
  const { isAuthenticated, isAdmin } = useSession()
  const location = useLocation()
  const navigate = useNavigate()

  // El cliente HTTP avisa cuando el token deja de ser válido (401).
  useEffect(() => {
    const onExpired = () => navigate("/auth/login", { replace: true })
    window.addEventListener(AUTH_EXPIRED_EVENT, onExpired)
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, onExpired)
  }, [navigate])

  if (!isAuthenticated) {
    return <Navigate to="/auth/login" state={{ from: location.pathname }} replace />
  }

  if (adminOnly && !isAdmin) {
    return <Navigate to="/panel" replace />
  }

  return <Outlet />
}
