import { useEffect, useState } from "react";
import { ArrowLeft, ArrowRight, CheckCircle2, Mail, RotateCcw, ShieldCheck } from "lucide-react";
import { Link, useNavigate } from "react-router";
import {
  Alert,
  buttonClass,
  Eyebrow,
  Field,
  OtpInput,
  PasswordField,
  Spinner,
} from "@components/ui";
import { isEmail, maskEmail } from "@utils/format";
import { useSession } from "../context/SessionContext";
import { DEMO_MODE } from "@lib/config/env";

const RESEND_SECONDS = 30;

// Recuperación de clave en 3 pasos:
//   1. email → se solicita el código
//   2. OTP (6 dígitos) → se verifica
//   3. nueva clave + confirmación → se define
export default function PasswordRecoveryPage() {
  const navigate = useNavigate();
  const { actions } = useSession();

  const [step, setStep] = useState("email");
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [demoCode, setDemoCode] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [countdown, setCountdown] = useState(0);

  useEffect(() => {
    if (countdown <= 0) return;
    const timer = setTimeout(() => setCountdown((c) => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [countdown]);

  const requestCode = async (targetEmail) => {
    setError("");
    setLoading(true);
    try {
      const result = await actions.forgotPassword(targetEmail);
      setDemoCode(result?.demoCode || "");
      setStep("otp");
      setOtp("");
      setCountdown(RESEND_SECONDS);
    } catch (err) {
      setError(err?.message || "No pudimos enviar el código. Intentá nuevamente.");
    } finally {
      setLoading(false);
    }
  };

  const submitEmail = async (event) => {
    event.preventDefault();
    const value = String(new FormData(event.currentTarget).get("email") || "").trim();
    if (!isEmail(value)) return setError("Ingresá un correo electrónico válido.");
    setEmail(value);
    await requestCode(value);
  };

  const submitOtp = (event) => {
    event.preventDefault();
    if (otp.length !== 6) return setError("Ingresá los 6 dígitos del código.");
    setError("");
    setStep("password");
  };

  const submitPassword = async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const password = String(form.get("password") || "");
    const confirm = String(form.get("confirm") || "");

    if (password.length < 8) return setError("Usá al menos 8 caracteres.");
    if (confirm !== password) return setError("Las contraseñas no coinciden.");

    setError("");
    setLoading(true);
    try {
      await actions.resetPassword({ email, code: otp, password });
      setStep("done");
    } catch (err) {
      // El backend valida el código al restablecer la clave: si es inválido
      // se vuelve al paso del OTP para que el usuario lo reingrese.
      if (err?.status === 400) {
        setOtp("");
        setStep("otp");
        setError(err?.message || "El código es inválido o expiró.");
        return;
      }
      setError(err?.message || "No pudimos actualizar la contraseña.");
    } finally {
      setLoading(false);
    }
  };

  if (step === "done") {
    return (
      <div className="text-center animate-pop">
        <span className="mx-auto mb-5 grid size-16 place-items-center rounded-full bg-success-soft text-success">
          <CheckCircle2 size={30} />
        </span>
        <h1 className="text-[clamp(26px,6vw,34px)] font-bold">Contraseña actualizada</h1>
        <p className="mx-auto mt-2.5 max-w-sm text-sm text-muted">
          Tu nueva clave está lista. Ya podés ingresar con ella.
        </p>
        <button onClick={() => navigate("/auth/login")} className={buttonClass("primary", "mt-7 w-full")}>
          Ir al inicio de sesión <ArrowRight size={18} />
        </button>
      </div>
    );
  }

  return (
    <div>
      <Link to="/auth/login" className="mb-5 inline-flex items-center gap-1.5 text-sm font-semibold text-muted transition-colors hover:text-ink">
        <ArrowLeft className="w-4" /> Volver al ingreso
      </Link>

      {/* Stepper */}
      <ol className="mb-6 flex items-center gap-2 text-xs font-bold text-muted">
        {["Correo", "Código", "Nueva clave"].map((label, index) => {
          const active = index === { email: 0, otp: 1, password: 2 }[step];
          const done = index < { email: 0, otp: 1, password: 2 }[step];
          return (
            <li key={label} className="flex items-center gap-2">
              <span
                className={`grid size-6 place-items-center rounded-full text-[11px] ${
                  done ? "bg-success text-white" : active ? "bg-lime text-lime-ink" : "bg-overlay text-muted"
                }`}
              >
                {done ? "✓" : index + 1}
              </span>
              <span className={active ? "text-ink" : ""}>{label}</span>
              {index < 2 && <span className="h-px w-6 bg-line" />}
            </li>
          );
        })}
      </ol>

      {step === "email" && (
        <div className="animate-fade">
          <Eyebrow>Recuperar acceso</Eyebrow>
          <h1 className="text-[clamp(26px,6vw,34px)] font-bold leading-tight">¿Olvidaste tu contraseña?</h1>
          <p className="mt-2.5 text-sm text-muted">Ingresá tu correo y te enviaremos un código de 6 dígitos.</p>

          <form onSubmit={submitEmail} className="card mt-6 grid gap-4 p-6 sm:p-7" noValidate>
            <Field label="Correo electrónico" name="email" type="email" placeholder="tu@correo.com" autoComplete="email" />
            {error && <Alert>{error}</Alert>}
            <button type="submit" disabled={loading} className={buttonClass("primary", "w-full")}>
              {loading ? (
                <>
                  <Spinner /> Enviando…
                </>
              ) : (
                <>
                  Enviar código <ArrowRight size={18} />
                </>
              )}
            </button>
          </form>
        </div>
      )}

      {step === "otp" && (
        <div className="animate-fade">
          <span className="mb-4 grid size-12 place-items-center rounded-2xl bg-teal-soft text-teal-strong">
            <Mail size={22} />
          </span>
          <Eyebrow>Verificación</Eyebrow>
          <h1 className="text-[clamp(26px,6vw,34px)] font-bold leading-tight">Ingresá el código</h1>
          <p className="mt-2.5 text-sm text-muted">
            Enviamos un código de 6 dígitos a <span className="font-semibold text-ink">{maskEmail(email)}</span>.
          </p>

          {DEMO_MODE && demoCode && (
            <div className="mt-4 rounded-xl border border-lime/60 bg-lime-soft px-4 py-3 text-sm font-semibold text-lime-ink">
              Código de demostración: <span className="font-extrabold tracking-widest">{demoCode}</span>
            </div>
          )}

          <form onSubmit={submitOtp} className="card mt-6 grid gap-5 p-6 sm:p-7" noValidate>
            <div className="grid gap-1.5">
              <span className="text-[13.5px] font-semibold text-ink">Código de verificación</span>
              <OtpInput value={otp} onChange={setOtp} error={Boolean(error)} />
              {error && <small className="text-xs font-semibold text-danger">{error}</small>}
            </div>

            <button type="submit" disabled={loading} className={buttonClass("primary", "w-full")}>
              Verificar código <ShieldCheck size={18} />
            </button>

            <button
              type="button"
              disabled={countdown > 0 || loading}
              onClick={() => requestCode(email)}
              className="mx-auto inline-flex items-center gap-1.5 text-sm font-semibold text-teal-strong transition-opacity hover:text-teal disabled:opacity-50"
            >
              <RotateCcw size={14} />
              {countdown > 0 ? `Reenviar en ${countdown}s` : "Reenviar código"}
            </button>
          </form>
        </div>
      )}

      {step === "password" && (
        <div className="animate-fade">
          <Eyebrow>Nueva contraseña</Eyebrow>
          <h1 className="text-[clamp(26px,6vw,34px)] font-bold leading-tight">Definí tu nueva clave</h1>
          <p className="mt-2.5 text-sm text-muted">Escribila dos veces para evitar errores.</p>

          <form onSubmit={submitPassword} className="card mt-6 grid gap-4 p-6 sm:p-7" noValidate>
            <PasswordField label="Nueva contraseña" name="password" placeholder="Mínimo 8 caracteres" autoComplete="new-password" />
            <PasswordField label="Confirmar nueva contraseña" name="confirm" placeholder="Repetí la contraseña" autoComplete="new-password" />
            {error && <Alert>{error}</Alert>}
            <button type="submit" disabled={loading} className={buttonClass("primary", "w-full")}>
              {loading ? (
                <>
                  <Spinner /> Guardando…
                </>
              ) : (
                <>
                  Guardar contraseña <CheckCircle2 size={18} />
                </>
              )}
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
