import { useRef, useState } from "react";
import { Link } from "react-router";
import { Eye, EyeOff, Moon, Sun } from "lucide-react";
import { DEMO_MODE } from "@lib/config/env";
import { useTheme } from "@lib/theme/ThemeContext";
import { cn, initials } from "@utils/format";

export { cn };

// ── Botones ─────────────────────────────────────────────────────────────────

const BUTTON =
  "inline-flex min-h-[46px] cursor-pointer select-none items-center justify-center gap-2 rounded-xl border border-transparent px-5 text-sm font-semibold tracking-[-0.005em] transition-[translate,scale,background,border-color,color,box-shadow] duration-200 hover:-translate-y-px active:translate-y-0 active:scale-[0.985] disabled:pointer-events-none disabled:opacity-50";

const BUTTON_VARIANTS = {
  primary: "bg-lime text-lime-ink shadow-lime hover:bg-lime-strong",
  teal: "bg-teal text-white hover:bg-teal-strong",
  dark: "bg-ink text-paper hover:opacity-90",
  secondary:
    "border border-line-strong bg-transparent text-ink hover:border-ink/40 hover:bg-overlay",
  ghost: "bg-transparent text-ink-soft hover:bg-overlay hover:text-ink",
  danger: "bg-danger text-white hover:brightness-105",
  soft: "bg-lime-soft text-lime-ink hover:brightness-95",
};

export const buttonClass = (variant = "primary", className) =>
  cn(BUTTON, BUTTON_VARIANTS[variant], className);

export function Button({ children, variant = "primary", className, icon: Icon, ...props }) {
  return (
    <button className={buttonClass(variant, className)} {...props}>
      {Icon && <Icon size={18} />}
      {children}
    </button>
  );
}

// ── Formularios ─────────────────────────────────────────────────────────────

const CONTROL =
  "min-h-[50px] w-full appearance-none rounded-xl border bg-paper px-3.5 py-3 text-[15px] font-medium text-ink outline-none transition-[border-color,box-shadow,background] duration-200 placeholder:font-normal placeholder:text-muted";

const CONTROL_OK =
  "border-line-strong hover:border-ink/35 focus:border-teal focus:shadow-[0_0_0_4px_var(--color-teal-soft)]";

const CONTROL_ERROR =
  "border-danger bg-[color-mix(in_srgb,var(--color-danger)_3%,var(--color-paper))] focus:shadow-[0_0_0_4px_var(--color-danger-soft)]";

export const controlClass = (error, className) =>
  cn(CONTROL, error ? CONTROL_ERROR : CONTROL_OK, className);

export function Field({ label, error, hint, as = "input", children, className, ...props }) {
  const Element = as;
  const custom = Boolean(children) && as === "input";

  return (
    <label className={cn("grid gap-1.5 text-[13.5px] font-semibold text-ink", className)}>
      {label && <span>{label}</span>}

      {custom ? (
        children
      ) : (
        <Element
          className={controlClass(
            error,
            cn(
              as === "textarea" && "min-h-28 resize-y leading-normal",
              as === "select" &&
                "appearance-none bg-[image:var(--select-arrow)] bg-[right_13px_center] bg-no-repeat pr-10",
            ),
          )}
          {...props}
        >
          {children}
        </Element>
      )}

      {error && <small className="text-[12.5px] font-semibold text-danger">{error}</small>}
      {!error && hint && <small className="text-[12.5px] font-normal text-muted">{hint}</small>}
    </label>
  );
}

export function PasswordField({ label = "Contraseña", error, hint, className, ...props }) {
  const [show, setShow] = useState(false);
  return (
    <Field label={label} error={error} hint={hint} className={className}>
      <div className="relative">
        <input
          className={controlClass(error, "pr-12")}
          type={show ? "text" : "password"}
          {...props}
        />
        <button
          type="button"
          onClick={() => setShow((v) => !v)}
          aria-label={show ? "Ocultar contraseña" : "Mostrar contraseña"}
          className="absolute right-1.5 top-1/2 grid size-9 -translate-y-1/2 cursor-pointer place-items-center rounded-lg text-muted transition-colors hover:bg-overlay hover:text-ink"
        >
          {show ? <EyeOff className="w-4" /> : <Eye className="w-4" />}
        </button>
      </div>
    </Field>
  );
}

export function Alert({ children, tone = "danger" }) {
  const tones = {
    danger: "border-danger/40 bg-danger-soft text-danger",
    success: "border-success/40 bg-success-soft text-success",
    info: "border-teal/40 bg-teal-soft text-teal",
    lime: "border-lime/60 bg-lime-soft text-lime-ink",
  };
  return (
    <div
      role="alert"
      className={cn(
        "flex items-start gap-2.5 rounded-xl border px-3.5 py-3 text-sm font-medium leading-snug",
        tones[tone],
      )}
    >
      <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-current" />
      <span>{children}</span>
    </div>
  );
}

// ── Marca ───────────────────────────────────────────────────────────────────

export function LogoMark({ className }) {
  return (
    <svg viewBox="0 0 40 40" className={className} aria-hidden="true">
      <rect width="40" height="40" rx="12" fill="var(--color-lime)" />
      <path
        d="M11 14.5 20 8.5l9 6"
        stroke="var(--color-ink)"
        strokeWidth="2.6"
        fill="none"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M13 17v8.5A2.5 2.5 0 0 0 15.5 28h9a2.5 2.5 0 0 0 2.5-2.5V17"
        stroke="var(--color-ink)"
        strokeWidth="2.6"
        fill="none"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M12 30.5c2.7-2 5.3-2 8 0s5.3 2 8 0"
        stroke="var(--color-teal-strong)"
        strokeWidth="2.4"
        fill="none"
        strokeLinecap="round"
      />
    </svg>
  );
}

export function Brand({ className, size = "text-[19px]" }) {
  return (
    <Link
      to="/"
      className={cn(
        "inline-flex items-center gap-2.5 font-extrabold tracking-[-0.02em] text-ink",
        size,
        className,
      )}
    >
      <LogoMark className="size-8 drop-shadow-sm" />
      <span className="font-display italic">
        Host<span className="text-teal-strong not-italic">Flow</span>
      </span>
    </Link>
  );
}

export function Eyebrow({ className, children }) {
  return (
    <span
      className={cn(
        "mb-2 inline-flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-teal-strong before:h-0.5 before:w-4 before:rounded-sm before:bg-current before:content-['']",
        className,
      )}
    >
      {children}
    </span>
  );
}

// ── Contenedores y texto ────────────────────────────────────────────────────

export function Card({ className, children, as: Element = "div", ...props }) {
  return (
    <Element className={cn("card", className)} {...props}>
      {children}
    </Element>
  );
}

export function Badge({ tone = "neutral", className, children }) {
  const tones = {
    neutral: "bg-overlay text-ink-soft",
    lime: "bg-lime-soft text-lime-ink",
    teal: "bg-teal-soft text-teal-strong",
    pink: "bg-pink-soft text-pink-strong",
    success: "bg-success-soft text-success",
    danger: "bg-danger-soft text-danger",
    ink: "bg-ink text-paper",
  };
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide",
        tones[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}

export function Avatar({ name = "", className, size = "size-9" }) {
  return (
    <span
      className={cn(
        "grid shrink-0 place-items-center rounded-full bg-teal-soft text-xs font-extrabold text-teal-strong",
        size,
        className,
      )}
    >
      {initials(name) || "·"}
    </span>
  );
}

export function SectionHeading({ eyebrow, title, text, center, className, children }) {
  return (
    <div className={cn("max-w-2xl", center && "mx-auto text-center", className)}>
      {eyebrow && <Eyebrow>{eyebrow}</Eyebrow>}
      {title && (
        <h2 className="text-[clamp(26px,3.4vw,40px)] font-bold leading-[1.1] text-ink">{title}</h2>
      )}
      {text && <p className="mt-3.5 text-[15.5px] leading-relaxed text-muted">{text}</p>}
      {children}
    </div>
  );
}

export function EmptyState({ icon: Icon, title, text, action }) {
  return (
    <div className="grid place-items-center gap-3 rounded-2xl border border-dashed border-line-strong px-6 py-14 text-center">
      {Icon && (
        <span className="grid size-12 place-items-center rounded-2xl bg-teal-soft text-teal-strong">
          <Icon className="w-6" />
        </span>
      )}
      <div>
        <p className="font-bold text-ink">{title}</p>
        {text && <p className="mt-1 max-w-sm text-sm text-muted">{text}</p>}
      </div>
      {action}
    </div>
  );
}

export function Spinner({ className }) {
  return (
    <span
      className={cn(
        "size-5 animate-spin rounded-full border-2 border-current border-t-transparent",
        className,
      )}
      aria-hidden="true"
    />
  );
}

export function Stat({ label, value, hint, tone = "lime", icon: Icon }) {
  const tones = {
    lime: "bg-lime-soft text-lime-ink",
    teal: "bg-teal-soft text-teal-strong",
    pink: "bg-pink-soft text-pink-strong",
  };
  return (
    <div className="card card-hover p-5">
      <span className={cn("mb-4 grid size-10 place-items-center rounded-xl", tones[tone])}>
        {Icon ? <Icon size={19} /> : <span className="size-2.5 rounded-full bg-current" />}
      </span>
      <p className="text-[clamp(24px,2.6vw,32px)] font-extrabold leading-none text-ink">{value}</p>
      <p className="mt-1.5 text-sm font-semibold text-ink-soft">{label}</p>
      {hint && <p className="text-xs text-muted">{hint}</p>}
    </div>
  );
}

// ── Switch ──────────────────────────────────────────────────────────────────

export function Switch({ checked, onChange, label, description }) {
  return (
    <label className="flex cursor-pointer items-start gap-3">
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        aria-label={label}
        onClick={() => onChange?.(!checked)}
        className={cn(
          "relative mt-0.5 h-6 w-11 shrink-0 rounded-full border transition-colors duration-200",
          checked ? "border-lime bg-lime" : "border-line-strong bg-overlay",
        )}
      >
        <span
          className={cn(
            "absolute top-1/2 size-5 -translate-y-1/2 rounded-full transition-all duration-200",
            checked ? "left-[calc(100%-22px)] bg-lime-ink" : "left-0.5 bg-muted",
          )}
        />
      </button>
      {(label || description) && (
        // El `<label>` nativo ya forwardea los clicks de esta zona al botón
        // (button es "labelable"): tocar el texto alterna el switch una sola vez.
        <span className="grid">
          {label && <span className="text-sm font-semibold text-ink">{label}</span>}
          {description && <span className="text-xs text-muted">{description}</span>}
        </span>
      )}
    </label>
  );
}

// ── OTP (6 dígitos) ─────────────────────────────────────────────────────────

export function OtpInput({ value = "", onChange, length = 6, autoFocus = true, error }) {
  const refs = useRef([]);
  // Espejo del valor: compone correctamente cambios seguidos dentro del mismo tick.
  const latest = useRef(value);
  latest.current = value;

  const digits = Array.from({ length }, (_, i) => value[i] || "");

  const setAt = (index, digit) => {
    const chars = latest.current.padEnd(length, " ").split("");
    chars[index] = digit;
    const next = chars.join("").trimEnd();
    latest.current = next;
    onChange?.(next.slice(0, length));
  };

  const handleChange = (index) => (event) => {
    const char = event.target.value.replace(/\D/g, "").slice(-1);
    if (!char) return;
    setAt(index, char);
    if (index < length - 1) refs.current[index + 1]?.focus();
  };

  const handleKeyDown = (index) => (event) => {
    if (event.key === "Backspace" && !digits[index] && index > 0) {
      refs.current[index - 1]?.focus();
    }
    if (event.key === "ArrowLeft" && index > 0) refs.current[index - 1]?.focus();
    if (event.key === "ArrowRight" && index < length - 1) refs.current[index + 1]?.focus();
  };

  const handlePaste = (event) => {
    event.preventDefault();
    const text = (event.clipboardData.getData("text") || "").replace(/\D/g, "").slice(0, length);
    if (!text) return;
    onChange?.(text);
    refs.current[Math.min(text.length, length - 1)]?.focus();
  };

  return (
    <div className="flex justify-between gap-1.5 sm:gap-2" onPaste={handlePaste}>
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(el) => (refs.current[index] = el)}
          inputMode="numeric"
          autoComplete={index === 0 ? "one-time-code" : "off"}
          maxLength={1}
          value={digit}
          autoFocus={autoFocus && index === 0}
          onChange={handleChange(index)}
          onKeyDown={handleKeyDown(index)}
          aria-label={`Dígito ${index + 1}`}
          className={cn(
            "size-10 appearance-none rounded-xl border bg-paper text-center text-lg font-extrabold text-ink outline-none transition-all duration-200 placeholder:text-muted sm:size-12 sm:text-xl",
            error
              ? "border-danger"
              : "border-line-strong hover:border-ink/35 focus:border-teal focus:shadow-[0_0_0_4px_var(--color-teal-soft)]",
          )}
        />
      ))}
    </div>
  );
}

// ── Tema claro/oscuro ───────────────────────────────────────────────────────

export function ThemeToggle({ className }) {
  const { isDark, toggle } = useTheme();
  return (
    <button
      type="button"
      onClick={toggle}
      aria-label={isDark ? "Cambiar a fondo claro" : "Cambiar a fondo oscuro"}
      title={isDark ? "Cambiar a fondo claro" : "Cambiar a fondo oscuro"}
      className={cn(
        "grid size-10 cursor-pointer place-items-center rounded-xl border border-line bg-paper text-ink-soft shadow-card transition-all duration-200 hover:-translate-y-px hover:text-ink",
        className,
      )}
    >
      <span className="relative grid place-items-center">
        <Sun
          className={cn(
            "size-[18px] transition-all duration-300",
            isDark ? "scale-100 opacity-100" : "absolute scale-50 opacity-0",
          )}
        />
        <Moon
          className={cn(
            "size-[18px] transition-all duration-300",
            isDark ? "absolute scale-50 opacity-0" : "scale-100 opacity-100",
          )}
        />
      </span>
    </button>
  );
}

// ── Demo banner ─────────────────────────────────────────────────────────────

export function DemoBanner({ className }) {
  if (!DEMO_MODE) return null;
  return (
    <div
      role="note"
      className={cn(
        "border-b border-lime/40 bg-lime-soft px-4 py-2 text-center text-[12.5px] font-semibold text-lime-ink",
        className,
      )}
    >
      Modo demostración · datos de prueba. Nada se envía a ningún servidor.
    </div>
  );
}
