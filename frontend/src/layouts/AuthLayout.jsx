import { Waves } from "lucide-react"
import { Outlet } from "react-router"
import { Brand, DemoBanner } from "../components/ui"

export default function AuthLayout() {
  return (
    <div className="auth-shell">
      <aside className="auth-story">
        <Brand light />
        <div className="auth-copy">
          <span className="eyebrow light">Gestión simple. Hospitalidad real.</span>
          <h1>
            Tu alojamiento,
            <br />
            más cerca de cada huésped.
          </h1>
          <p>
            Centralizá el check-in, los adicionales y los traslados en una
            experiencia pensada para el Delta.
          </p>
        </div>
        <Waves className="story-wave" />
      </aside>
      <main className="auth-main">
        <DemoBanner />
        <Outlet />
      </main>
    </div>
  )
}
