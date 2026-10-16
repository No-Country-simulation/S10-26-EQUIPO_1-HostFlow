import { useState } from "react"
import { ArrowLeft, ArrowRight } from "lucide-react"
import { Link, useNavigate } from "react-router"
import { Button, Field } from "../../components/ui"
import { isEmail } from "../../lib/format"
import { api } from "../../services"
import { describeError } from "../../services/errors"

export default function Register() {
  const navigate = useNavigate()
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState("")
  const [loading, setLoading] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const data = {
      fullName: String(form.get("fullName") || "").trim(),
      lodgingName: String(form.get("lodgingName") || "").trim(),
      email: String(form.get("email") || "").trim(),
      password: String(form.get("password") || ""),
    }
    const next = {}
    if (!data.fullName) next.fullName = "Ingresá tu nombre y apellido."
    if (!data.lodgingName) next.lodgingName = "Ingresá el nombre del alojamiento."
    if (!isEmail(data.email)) next.email = "Ingresá un correo electrónico válido."
    if (data.password.length < 8) next.password = "Usá al menos 8 caracteres."
    if (!form.get("terms")) next.terms = "Tenés que aceptar los términos para continuar."
    setErrors(next)
    setFormError("")
    if (Object.keys(next).length) return
    setLoading(true)
    try {
      await api.register(data)
      navigate("/panel")
    } catch (err) {
      setFormError(describeError(err, "No pudimos crear la cuenta. Intentá nuevamente."))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-card">
      <Link className="back" to="/auth/login">
        <ArrowLeft /> Volver
      </Link>
      <span className="eyebrow">Empezá hoy</span>
      <h2>Creá la cuenta de tu alojamiento</h2>
      <p className="muted">Los huéspedes no necesitan cuenta: completan su llegada desde el enlace de tu alojamiento.</p>
      <form className="form-stack" onSubmit={submit} noValidate>
        <Field label="Nombre y apellido" name="fullName" placeholder="Ej. Martina Silva" error={errors.fullName} autoComplete="name" />
        <Field label="Nombre del alojamiento" name="lodgingName" placeholder="Ej. Cabañas del Arroyo" error={errors.lodgingName} />
        <Field label="Correo electrónico" name="email" type="email" placeholder="nombre@correo.com" error={errors.email} autoComplete="email" />
        <Field label="Contraseña" name="password" type="password" placeholder="Mínimo 8 caracteres" error={errors.password} autoComplete="new-password" />
        <label className="check small">
          <input type="checkbox" name="terms" /> Acepto los términos y la política de privacidad.
        </label>
        {errors.terms && <small className="field-error" style={{ color: "var(--danger)" }}>{errors.terms}</small>}
        {formError && <div className="alert error" role="alert">{formError}</div>}
        <Button className="full" type="submit" disabled={loading}>
          {loading ? "Creando cuenta…" : <>Crear mi cuenta <ArrowRight /></>}
        </Button>
      </form>
      <p className="center muted small">
        ¿Ya tenés cuenta? <Link to="/auth/login">Ingresá</Link>
      </p>
    </div>
  )
}
