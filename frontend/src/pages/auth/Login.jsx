import { useState } from "react";
import { ArrowRight, Eye, EyeOff } from "lucide-react";
import { Link, useNavigate } from "react-router";
import {
  Alert,
  AuthCard,
  AuthTitle,
  Brand,
  Button,
  buttonClass,
  controlClass,
  Eyebrow,
  Field,
} from "../../components/ui";
import { isEmail } from "../../lib/format";
import { api } from "../../services";
import { describeError } from "../../services/errors";

export default function Login() {
  const [show, setShow] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const submit = async (event) => {
    event.preventDefault();

    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") || "").trim();
    const password = String(form.get("password") || "");

    if (!email || !password) {
      return setError("Completá tu correo y contraseña.");
    }

    if (!isEmail(email)) {
      return setError("Ingresá un correo electrónico válido.");
    }

    setError("");
    setLoading(true);

    try {
      await api.login({ email, password });
      navigate("/panel");
    } catch (err) {
      setError(
        err?.status === 401
          ? "Correo o contraseña incorrectos."
          : describeError(err, "No pudimos iniciar sesión. Intentá nuevamente."),
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthCard>
      <div className="mb-8.5 hidden max-[800px]:block">
        <Brand />
      </div>

      <Eyebrow>Bienvenido de nuevo</Eyebrow>

      <AuthTitle>Ingresá a tu cuenta</AuthTitle>

      <p className="text-muted">Gestioná tu alojamiento y recibí a tus huéspedes.</p>

      <form onSubmit={submit} className="mt-7 grid gap-4.5" noValidate>
        <Field
          label="Correo electrónico"
          name="email"
          type="email"
          placeholder="tu@alojamiento.com"
          autoComplete="email"
        />

        <Field label="Contraseña">
          <div className="relative">
            <input
              className={controlClass(false, "pr-13")}
              name="password"
              type={show ? "text" : "password"}
              placeholder="••••••••"
              autoComplete="current-password"
            />

            <button
              type="button"
              onClick={() => setShow(!show)}
              aria-label={show ? "Ocultar contraseña" : "Mostrar contraseña"}
              className="absolute right-1.5 top-1.5 grid size-9.5 cursor-pointer place-items-center rounded-[9px] border-0 bg-transparent text-muted hover:bg-green-soft hover:text-green pointer-coarse:right-0.75 pointer-coarse:top-0.75 pointer-coarse:size-11"
            >
              {show ? <EyeOff className="w-4.5" /> : <Eye className="w-4.5" />}
            </button>
          </div>
        </Field>

        <div className="flex items-center justify-between text-[13.5px] font-[650] text-green">
          <span />

          <Link
            to="/auth/recuperar"
            className="underline-offset-3 hover:underline pointer-coarse:inline-flex pointer-coarse:min-h-11 pointer-coarse:items-center"
          >
            ¿Olvidaste tu contraseña?
          </Link>
        </div>

        {error && <Alert>{error}</Alert>}

        <Button className="w-full" type="submit" disabled={loading}>
          {loading ? (
            "Ingresando…"
          ) : (
            <>
              Ingresar <ArrowRight size={18} />
            </>
          )}
        </Button>
      </form>

      <div className="my-7.5 h-px bg-line text-center">
        <span className="relative -top-2.5 bg-white px-3.5 text-[12.5px] text-muted max-[600px]:bg-paper">
          ¿Primera vez?
        </span>
      </div>

      <Link className={buttonClass("secondary", "w-full")} to="/auth/registro">
        Crear cuenta de alojamiento
      </Link>
    </AuthCard>
  );
}
