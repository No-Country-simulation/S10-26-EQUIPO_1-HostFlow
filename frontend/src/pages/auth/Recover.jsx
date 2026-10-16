import { useState } from "react";
import { ArrowLeft, Mail } from "lucide-react";
import { Link } from "react-router";
import { Alert, AuthCard, AuthTitle, Button, Eyebrow, Field } from "../../components/ui";
import { DEMO_MODE } from "../../config/env";
import { isEmail } from "../../lib/format";
import { api } from "../../services";
import { describeError } from "../../services/errors";

export default function Recover() {
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (event) => {
    event.preventDefault();

    const email = String(new FormData(event.currentTarget).get("email") || "").trim();

    if (!isEmail(email)) {
      return setError("Ingresá un correo electrónico válido.");
    }

    setError("");
    setLoading(true);

    try {
      await api.recoverPassword(email);
      setSent(true);
    } catch (err) {
      setError(describeError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthCard>
      <Link
        className="mb-7.5 inline-flex items-center gap-1.5 text-[14px] font-semibold text-muted transition-colors duration-200 hover:text-green pointer-coarse:mb-3.5 pointer-coarse:min-h-11"
        to="/auth/login"
      >
        <ArrowLeft className="w-4.5" />
        Volver al ingreso
      </Link>

      {sent ? (
        <div className="py-5 text-center">
          <span className="mx-auto mb-6 grid size-18 place-items-center rounded-full bg-green-tint text-green">
            <Mail />
          </span>

          {DEMO_MODE ? (
            <>
              <AuthTitle>Simulación completada</AuthTitle>
              <p className="mb-6.25 leading-[1.6] text-muted">
                Estás en modo demostración: no se envió ningún correo.
              </p>
            </>
          ) : (
            <>
              <AuthTitle>Revisá tu correo</AuthTitle>
              <p className="mb-6.25 leading-[1.6] text-muted">
                Si la cuenta existe, vas a recibir instrucciones para recuperar tu contraseña.
              </p>
            </>
          )}

          <Button variant="secondary" onClick={() => setSent(false)}>
            Volver a intentar
          </Button>
        </div>
      ) : (
        <>
          <Eyebrow>Recuperar acceso</Eyebrow>

          <AuthTitle>¿Olvidaste tu contraseña?</AuthTitle>

          <p className="text-muted">Ingresá tu correo y te enviaremos las instrucciones.</p>

          <form className="mt-7 grid gap-4.5" onSubmit={submit} noValidate>
            <Field
              label="Correo electrónico"
              name="email"
              type="email"
              placeholder="tu@correo.com"
              autoComplete="email"
            />

            {error && <Alert>{error}</Alert>}

            <Button className="w-full" type="submit" disabled={loading}>
              {loading ? "Enviando…" : "Enviar instrucciones"}
            </Button>
          </form>
        </>
      )}
    </AuthCard>
  );
}
