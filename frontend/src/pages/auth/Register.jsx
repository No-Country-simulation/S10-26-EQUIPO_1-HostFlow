import { useState } from "react";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { Link, useNavigate } from "react-router";
import { Alert, buttonClass, Eyebrow, Field, PasswordField, Spinner } from "../../components/ui";
import { isEmail } from "../../lib/format";
import { useSession } from "../../context/SessionContext";

const EMAIL_TAKEN = "Ese correo ya tiene una cuenta. Ingresá o recuperá tu contraseña.";

export default function Register() {
  const navigate = useNavigate();
  const { actions } = useSession();
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const data = {
      fullName: String(form.get("fullName") || "").trim(),
      lodgingName: String(form.get("lodgingName") || "").trim(),
      email: String(form.get("email") || "").trim(),
      password: String(form.get("password") || ""),
      confirm: String(form.get("confirm") || ""),
    };

    const next = {};
    if (!data.fullName) next.fullName = "Ingresá tu nombre y apellido.";
    if (!data.lodgingName) next.lodgingName = "Ingresá el nombre del alojamiento.";
    if (!isEmail(data.email)) next.email = "Ingresá un correo electrónico válido.";
    if (data.password.length < 8) next.password = "Usá al menos 8 caracteres.";
    if (data.confirm !== data.password) next.confirm = "Las contraseñas no coinciden.";
    if (!form.get("terms")) next.terms = "Tenés que aceptar los términos para continuar.";

    setErrors(next);
    setFormError("");
    if (Object.keys(next).length) return;

    setLoading(true);
    try {
      const { user } = await actions.register({
        fullName: data.fullName,
        accommodationName: data.lodgingName,
        email: data.email,
        password: data.password,
        termsAccepted: true,
      });
      navigate(user.role === "ADMIN" ? "/admin" : "/panel", { replace: true });
    } catch (err) {
      setFormError(err?.status === 409 ? EMAIL_TAKEN : err?.message || "No pudimos crear la cuenta. Intentá nuevamente.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Link to="/auth/login" className="mb-5 inline-flex items-center gap-1.5 text-sm font-semibold text-muted transition-colors hover:text-ink">
        <ArrowLeft className="w-4" /> Volver
      </Link>

      <Eyebrow>Empezá hoy</Eyebrow>
      <h1 className="text-[clamp(26px,6vw,34px)] font-bold leading-tight">Creá la cuenta de tu alojamiento</h1>
      <p className="mt-2.5 text-sm text-muted">
        Los huéspedes no necesitan cuenta: completan su llegada desde el enlace de tu alojamiento.
      </p>

      <form onSubmit={submit} className="card mt-6 grid gap-4 p-6 sm:p-7" noValidate>
        <Field label="Nombre y apellido" name="fullName" placeholder="Ej. Martina Silva" error={errors.fullName} autoComplete="name" />
        <Field label="Nombre del alojamiento" name="lodgingName" placeholder="Ej. Cabañas del Arroyo" error={errors.lodgingName} />
        <Field label="Correo electrónico" name="email" type="email" placeholder="nombre@correo.com" error={errors.email} autoComplete="email" />
        <div className="grid gap-4 sm:grid-cols-2">
          <PasswordField name="password" placeholder="Mínimo 8 caracteres" error={errors.password} autoComplete="new-password" />
          <PasswordField label="Confirmar contraseña" name="confirm" placeholder="Repetí la contraseña" error={errors.confirm} autoComplete="new-password" />
        </div>

        <label className="flex cursor-pointer items-start gap-2.5 text-sm text-ink-soft">
          <input type="checkbox" name="terms" className="mt-0.5 size-4 shrink-0 accent-teal" />
          Acepto los términos y la política de privacidad.
        </label>
        {errors.terms && <small className="text-xs font-semibold text-danger">{errors.terms}</small>}

        {formError && <Alert>{formError}</Alert>}

        <button type="submit" disabled={loading} className={buttonClass("primary", "w-full")}>
          {loading ? (
            <>
              <Spinner /> Creando cuenta…
            </>
          ) : (
            <>
              Crear mi cuenta <ArrowRight size={18} />
            </>
          )}
        </button>
      </form>

      <p className="mt-5 text-center text-sm text-muted">
        ¿Ya tenés cuenta?{" "}
        <Link to="/auth/login" className="font-semibold text-teal-strong transition-colors hover:text-teal">
          Ingresá
        </Link>
      </p>
    </div>
  );
}
