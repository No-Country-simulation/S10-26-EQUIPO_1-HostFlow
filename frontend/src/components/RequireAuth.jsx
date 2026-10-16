import { useEffect } from "react"
import { Navigate, Outlet, useNavigate } from "react-router"
import { api } from "../services"
import { AUTH_EXPIRED_EVENT } from "../services/httpClient"

// Protege rutas privadas: sin sesión redirige al login, y si el backend responde 401
// en una llamada autenticada, también vuelve al login.
export default function RequireAuth() {
  const navigate = useNavigate()

  useEffect(() => {
    const onExpired = () => navigate("/auth/login", { replace: true })
    window.addEventListener(AUTH_EXPIRED_EVENT, onExpired)
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, onExpired)
  }, [navigate])

  if (!api.isAuthenticated()) return <Navigate to="/auth/login" replace />
  return <Outlet />
}
