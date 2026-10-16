import { useState } from "react"
import { ArrowLeft, Mail } from "lucide-react"
import { Link } from "react-router"
import { Button, Field } from "../../components/ui"
import { DEMO_MODE } from "../../config/env"
import { isEmail } from "../../lib/format"
import { api } from "../../services"
import { describeError } from "../../services/errors"

export default function Recover() {
  const [sent, setSent] = useState(false)
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    const email = String(new FormData(event.currentTarget).get("email") || "").trim()
    if (!isEmail(email)) return setError("Ingresá un correo electrónico válido.")
    setError("")
    setLoading(true)
    try {
      await api.recoverPassword(email)
      setSent(true)
    } catch (err) {
      setError(describeError(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-card">
      <Link className="back" to="/auth/login">
        <ArrowLeft /> Volver al ingreso
      </Link>
      {sent ? (
        <div className="success-state">
          <span>
            <Mail />
          </span>
          {DEMO_MODE ? (
            <>
              <h2>Simulación completada</h2>
              <p>Estás en modo demostración: no se envió ningún correo.</p>
            </>
          ) : (
            <>
              <h2>Revisá tu correo</h2>
              <p>Si la cuenta existe, vas a recibir instrucciones para recuperar tu contraseña.</p>
            </>
          )}
          <Button variant="secondary" onClick={() => setSent(false)}>
            Volver a intentar
          </Button>
        </div>
      ) : (
        <>
          <span className="eyebrow">Recuperar acceso</span>
          <h2>¿Olvidaste tu contraseña?</h2>
          <p className="muted">Ingresá tu correo y te enviaremos las instrucciones.</p>
          <form className="form-stack" onSubmit={submit} noValidate>
            <Field label="Correo electrónico" name="email" type="email" placeholder="tu@correo.com" autoComplete="email" />
            {error && <div className="alert error" role="alert">{error}</div>}
            <Button className="full" type="submit" disabled={loading}>
              {loading ? "Enviando…" : "Enviar instrucciones"}
            </Button>
          </form>
        </>
      )}
    </div>
  )
}
