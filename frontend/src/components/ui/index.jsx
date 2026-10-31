import { useRef, useState } from "react";
import { Link } from "react-router";
import { CircleAlert, Eye, EyeOff, Moon, Sun } from "lucide-react";
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

// Aviso de error de un campo. Va debajo del input (no en un cartel suelto al
// final del formulario) pero con el mismo peso visual que una alerta: caja
// suave, borde y tilde de aviso, para que no pase desapercibido.
export function FieldError({ children, id }) {
  if (!children) return null;
  return (
    <span
      id={id}
      role="alert"
      className="animate-rise flex items-start gap-2 rounded-xl border border-danger/35 bg-danger-soft px-3 py-2 text-[12.5px] font-semibold leading-snug text-danger"
    >
      <CircleAlert size={14} className="mt-px shrink-0" aria-hidden="true" />
      <span>{children}</span>
    </span>
  );
}

export function Field({ label, error, hint, as = "input", children, className, ...props }) {
  const Element = as;
  const custom = Boolean(children) && as === "input";
  const errorId = error && props.name ? `${props.name}-error` : undefined;

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
          aria-describedby={errorId}
          {...props}
        >
          {children}
        </Element>
      )}

      <FieldError id={errorId}>{error}</FieldError>
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

// Logo de WhatsApp. lucide ya no incluye íconos de marcas, así que va como SVG
// propio: es el ícono que el huésped reconoce de inmediato.
export function WhatsAppIcon({ size = 16, className, ...props }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width={size}
      height={size}
      fill="currentColor"
      className={className}
      aria-hidden="true"
      {...props}
    >
      <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.297-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
    </svg>
  );
}

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

// Título de un formulario dentro de una Card. Queda pegado arriba (debajo de
// la barra del panel) mientras se completa el formulario, así sigue visible
// aun cuando el usuario llega al final y tiene que confirmar.
// Los márgenes negativos lo estiran hasta los bordes de la card.
export function StickyFormTitle({ icon: Icon, title, hint, aside, className, children }) {
  return (
    <div
      className={cn(
        "sticky top-[65px] z-20 -mx-6 flex flex-wrap items-center justify-between gap-2 border-b border-line bg-paper/95 px-6 pb-3 pt-2 backdrop-blur",
        className,
      )}
    >
      <div className="flex min-w-0 items-center gap-2">
        {Icon && (
          <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-teal-soft text-teal-strong">
            <Icon size={17} aria-hidden="true" />
          </span>
        )}
        <div className="min-w-0">
          <p className="text-sm font-bold text-ink">{title || children}</p>
          {hint && <p className="text-xs text-muted">{hint}</p>}
        </div>
      </div>
      {aside}
    </div>
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

// Logo del alojamiento: círculo limpio, sin marco blanco ni relleno, para que
// la marca se integre a la banda de color. La imagen se centra y cubre el
// círculo (encuadre tipo avatar), así se ve redonda en cualquier tamaño. Si no
// hay logo, muestra iniciales con el color de la cabecera de marca.
export function BrandLogo({ src, name = "", size = "md", alt, className }) {
  const dim = { sm: "size-9", md: "size-11", lg: "size-16" }[size] || "size-11";

  if (!src) {
    return (
      <span
        aria-hidden="true"
        className={cn("grid shrink-0 place-items-center rounded-full text-sm font-extrabold", dim, className)}
        style={{
          backgroundColor: "color-mix(in srgb, var(--hf-hero-ink, currentColor) 15%, transparent)",
          color: "var(--hf-hero-ink, currentColor)",
        }}
      >
        {initials(name) || "·"}
      </span>
    );
  }

  return (
    <span
      className={cn(
        "inline-block shrink-0 overflow-hidden rounded-full bg-cloud ring-1 ring-black/10 ring-inset",
        dim,
        className,
      )}
    >
      <img
        src={src}
        alt={alt || (name ? `Logo de ${name}` : "Logo")}
        className="block size-full object-cover"
      />
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
        "border-b border-lime/50 bg-paper px-4 py-2 text-center text-[12.5px] font-semibold text-ink-soft",
        className,
      )}
    >
      <strong className="font-bold text-lime-strong">Modo demostración</strong> · datos de prueba. Nada se envía a
      ningún servidor.
    </div>
  );
}
