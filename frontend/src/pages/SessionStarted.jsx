import { LogOut } from "lucide-react"
import { useNavigate } from "react-router"
import { Brand, Button, DemoBanner } from "../components/ui"
import { DEMO_MODE } from "../config/env"
import { api } from "../services"

// PANTALLA TEMPORAL posterior al ingreso. Existe solo para poder comprobar el flujo de
// autenticación de punta a punta (login/registro → sesión → cerrar sesión).
// Login y Registro navegan a "/panel": reemplazá esta pantalla por el panel real
// cuando se incorpore ese módulo.
export default function SessionStarted() {
  const navigate = useNavigate()

  const logout = () => {
    api.logout()
    navigate("/auth/login")
  }

  return (
    <main className="auth-main" style={{ minHeight: "100vh" }}>
      <DemoBanner />
      <div className="auth-card">
        <div style={{ marginBottom: 28 }}>
          <Brand />
        </div>
        <span className="eyebrow">Sesión iniciada</span>
        <h2>Autenticación completada</h2>
        <p className="muted" style={{ marginBottom: 24 }}>
          {DEMO_MODE
            ? "Estás en modo demostración: no se validó ninguna credencial ni se creó ninguna sesión real."
            : "Tu sesión está activa. Esta pantalla es temporal: el panel todavía no está incluido en esta versión."}
        </p>
        <Button variant="secondary" className="full" icon={LogOut} onClick={logout}>
          Cerrar sesión
        </Button>
      </div>
    </main>
  )
}
