import { useState } from "react"
import { ArrowRight, Eye, EyeOff } from "lucide-react"
import { Link, useNavigate } from "react-router"
import { Brand, Button, Field } from "../../components/ui"
import { isEmail } from "../../lib/format"
import { api } from "../../services"
import { describeError } from "../../services/errors"

export default function Login() {
  const [show, setShow] = useState(false)
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const submit = async (event) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const email = String(form.get("email") || "").trim()
    const password = String(form.get("password") || "")
    if (!email || !password) return setError("Completá tu correo y contraseña.")
    if (!isEmail(email)) return setError("Ingresá un correo electrónico válido.")
    setError("")
    setLoading(true)
    try {
      await api.login({ email, password })
      navigate("/panel")
    } catch (err) {
      setError(
        err?.status === 401
          ? "Correo o contraseña incorrectos."
          : describeError(err, "No pudimos iniciar sesión. Intentá nuevamente."),
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-card">
      <div className="mobile-brand">
        <Brand />
      </div>
      <span className="eyebrow">Bienvenido de nuevo</span>
      <h2>Ingresá a tu cuenta</h2>
      <p className="muted">Gestioná tu alojamiento y recibí a tus huéspedes.</p>
      <form onSubmit={submit} className="form-stack" noValidate>
        <Field label="Correo electrónico" name="email" type="email" placeholder="tu@alojamiento.com" autoComplete="email" />
        <Field label="Contraseña">
          <div className="password">
            <input className="control" name="password" type={show ? "text" : "password"} placeholder="••••••••" autoComplete="current-password" />
            <button type="button" onClick={() => setShow(!show)} aria-label={show ? "Ocultar contraseña" : "Mostrar contraseña"}>
              {show ? <EyeOff /> : <Eye />}
            </button>
          </div>
        </Field>
        <div className="form-row">
          <span />
          <Link to="/auth/recuperar">¿Olvidaste tu contraseña?</Link>
        </div>
        {error && <div className="alert error" role="alert">{error}</div>}
        <Button className="full" type="submit" disabled={loading}>
          {loading ? "Ingresando…" : <>Ingresar <ArrowRight size={18} /></>}
        </Button>
      </form>
      <div className="divider">
        <span>¿Primera vez?</span>
      </div>
      <Link className="btn btn-secondary full" to="/auth/registro">
        Crear cuenta de alojamiento
      </Link>
    </div>
  )
}
