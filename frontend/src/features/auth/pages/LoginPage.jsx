import { useState } from "react";
import { ArrowRight, LogIn, ShieldCheck, UserRound } from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router";
import {
  Alert,
  buttonClass,
  Eyebrow,
  Field,
  PasswordField,
  Spinner,
} from "@components/ui";
import { isEmail } from "@utils/format";
import { useSession } from "../context/SessionContext";
import { DEMO_MODE } from "@lib/config/env";

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { actions } = useSession();

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const redirectTo = location.state?.from || "/panel";

  const finish = (role) => {
    navigate(role === "ADMIN" ? "/admin" : redirectTo, { replace: true });
  };

  const submit = async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") || "").trim();
    const password = String(form.get("password") || "");

    if (!email || !password) return setError("Completá tu correo y contraseña.");
    if (!isEmail(email)) return setError("Ingresá un correo electrónico válido.");

    setError("");
    setLoading(true);
    try {
      const { user } = await actions.login({ email, password });
      finish(user.role);
    } catch (err) {
      setError(err?.status === 401 ? "Correo o contraseña incorrectos." : err?.message || "No pudimos iniciar sesión.");
    } finally {
      setLoading(false);
    }
  };

  const quickLogin = async (email) => {
    setError("");
    setLoading(true);
    try {
      const { user } = await actions.demoLogin(email);
      finish(user.role);
    } catch (err) {
      setError(err?.message || "No pudimos iniciar sesión.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="text-center">
        <Eyebrow className="justify-center">Bienvenido de nuevo</Eyebrow>
        <h1 className="text-[clamp(28px,6vw,38px)] font-bold leading-tight">Ingresá a tu cuenta</h1>
        <p className="mt-2.5 text-sm text-muted">Gestioná tu alojamiento y recibí a tus huéspedes.</p>
      </div>

      <form onSubmit={submit} className="card mt-7 grid gap-4 p-6 sm:p-7" noValidate>
        <Field label="Correo electrónico" name="email" type="email" placeholder="tu@alojamiento.com" autoComplete="email" />
        <PasswordField name="password" placeholder="••••••••" autoComplete="current-password" />

        <div className="flex justify-end">
          <Link to="/auth/recuperar" className="text-sm font-semibold text-teal-strong transition-colors hover:text-teal">
            ¿Olvidaste tu contraseña?
          </Link>
        </div>

        {error && <Alert>{error}</Alert>}

        <button type="submit" disabled={loading} className={buttonClass("primary", "w-full")}>
          {loading ? (
            <>
              <Spinner /> Ingresando…
            </>
          ) : (
            <>
              Ingresar <ArrowRight size={18} />
            </>
          )}
        </button>
      </form>

      <div className="mt-5 flex items-center gap-3 text-xs font-semibold uppercase tracking-wide text-muted">
        <span className="h-px flex-1 bg-line" />
        ¿Todavía no tenés cuenta?
        <span className="h-px flex-1 bg-line" />
      </div>

      <Link to="/auth/registro" className={buttonClass("secondary", "mt-4 w-full")}>
        Crear cuenta de alojamiento
      </Link>

      {DEMO_MODE && (
        <div className="mt-6 grid grid-cols-2 gap-3">
          <button
            type="button"
            onClick={() => quickLogin("anfitrion@hostflow.app")}
            className="inline-flex min-h-[46px] cursor-pointer items-center justify-center gap-2 rounded-xl border border-line bg-cloud px-3 text-xs font-bold text-ink-soft transition-all hover:-translate-y-px hover:border-teal hover:text-teal-strong"
          >
            <UserRound size={15} /> Demo anfitrión
          </button>
          <button
            type="button"
            onClick={() => quickLogin("admin@hostflow.app")}
            className="inline-flex min-h-[46px] cursor-pointer items-center justify-center gap-2 rounded-xl border border-line bg-cloud px-3 text-xs font-bold text-ink-soft transition-all hover:-translate-y-px hover:border-teal hover:text-teal-strong"
          >
            <ShieldCheck size={15} /> Demo admin
          </button>
        </div>
      )}

      <p className="mt-6 flex items-center justify-center gap-2 text-center text-xs text-muted">
        <LogIn size={13} /> Protegido con OTP y sesión segura
      </p>
    </div>
  );
}
