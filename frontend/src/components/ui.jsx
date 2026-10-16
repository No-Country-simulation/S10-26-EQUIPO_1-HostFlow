import { Waves } from "lucide-react";
import { Link } from "react-router";
import { DEMO_MODE } from "../config/env";
import { cn } from "../lib/format";

export { cn };

// ── Botones ─────────────────────────────────────────────────────────────

const BUTTON =
  "inline-flex min-h-[46px] cursor-pointer items-center justify-center gap-2.25 rounded-xl border border-transparent px-5 text-sm font-[650] tracking-[0.005em] transition-[translate,scale,transform,box-shadow,background,border-color,color] duration-200 hover:-translate-y-px active:translate-y-0 active:scale-[0.985] disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-50 disabled:shadow-none";

const BUTTON_VARIANTS = {
  primary:
    "bg-btn text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.2),0_6px_16px_rgba(30,101,88,0.28)] [&:not(:disabled)]:hover:shadow-[inset_0_1px_0_rgba(255,255,255,0.2),0_10px_24px_rgba(30,101,88,0.36)]",

  secondary:
    "border-2 border-[#144d43] bg-white text-[#144d43] shadow-[0_3px_8px_rgba(20,48,43,0.12)] hover:bg-green-soft hover:border-[#0f3a33] hover:shadow-md",
};

// Clases de un botón; sirve también para <Link> con aspecto de botón.
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

// ── Formularios ─────────────────────────────────────────────────────────

const CONTROL =
  "min-h-[50px] w-full rounded-xl border bg-white px-3.5 py-3 text-[15px] font-medium text-ink outline-none transition-[border-color,box-shadow,background] duration-200 placeholder:font-normal placeholder:text-[#9aa9a4]";

const CONTROL_OK =
  "border-line-strong shadow-[inset_0_1px_2px_rgba(20,48,43,0.04)] hover:border-[#b3c4bd] focus:border-green focus:shadow-[0_0_0_4px_var(--color-green-ring)]";

const CONTROL_ERROR =
  "border-danger bg-[#fffafa] shadow-[inset_0_1px_2px_rgba(20,48,43,0.04)] focus:shadow-[0_0_0_4px_rgba(179,65,47,0.16)]";

export const controlClass = (error, className) =>
  cn(CONTROL, error ? CONTROL_ERROR : CONTROL_OK, className);

export function Field({ label, error, hint, as = "input", children, ...props }) {
  const Element = as;
  const custom = children && as === "input";

  return (
    <label className="grid gap-1.75 text-[13.5px] font-bold text-ink">
      <span>{label}</span>

      {custom ? (
        children
      ) : (
        <Element
          className={controlClass(
            error,
            cn(
              as === "textarea" && "min-h-24 resize-y leading-normal",
              as === "select" &&
                "appearance-none bg-select bg-size-[18px] bg-position-[right_13px_center] bg-no-repeat pr-10",
            ),
          )}
          {...props}
        >
          {children}
        </Element>
      )}

      {error && <small className="text-[12.5px] font-semibold text-danger">{error}</small>}

      {hint && <small className="text-[12.5px] font-normal text-muted">{hint}</small>}
    </label>
  );
}

export function Alert({ children }) {
  return (
    <div
      role="alert"
      className="flex items-center gap-2.5 rounded-xl border border-[#f3cfc7] bg-danger-soft px-3.75 py-3.25 text-sm leading-[1.4] text-danger"
    >
      {children}
    </div>
  );
}

// ── Marca, textos y contenedores ────────────────────────────────────────

export function Brand({ light }) {
  return (
    <Link
      to="/"
      className={cn(
        "flex items-center gap-2.75 text-[19px] font-extrabold tracking-[-0.02em] pointer-coarse:min-h-11",
        light ? "text-white" : "text-green-dark",
      )}
    >
      <span
        className={cn(
          "grid size-9 place-items-center rounded-[11px] text-white",
          light
            ? "bg-white/[0.14] shadow-[inset_0_0_0_1px_rgba(255,255,255,0.16)]"
            : "bg-[linear-gradient(145deg,#4b8479,#144d43)] shadow-[0_4px_12px_rgba(30,101,88,0.3)]",
        )}
      >
        <Waves size={20} />
      </span>
      HostFlow
    </Link>
  );
}

export function Eyebrow({ light, className, children }) {
  return (
    <span
      className={cn(
        "mb-2.5 inline-flex items-center gap-2.25 text-xs font-extrabold uppercase tracking-[0.14em] before:h-0.5 before:w-4.5 before:rounded-sm before:bg-current before:opacity-[0.55] before:content-['']",
        light ? "text-[#cfe3db]" : "text-green",
        className,
      )}
    >
      {children}
    </span>
  );
}

export function AuthCard({ children }) {
  return (
    <div className="w-[min(100%,450px)] animate-rise rounded-[26px] border border-line bg-white px-10 py-10.5 shadow-md max-[600px]:border-0 max-[600px]:bg-transparent max-[600px]:p-0 max-[600px]:shadow-none">
      {children}
    </div>
  );
}

export function AuthTitle({ children }) {
  return <h2 className="mb-2 text-[34px] max-[600px]:text-[30px]">{children}</h2>;
}

export function DemoBanner({ className }) {
  if (!DEMO_MODE) return null;

  return (
    <div
      role="note"
      className={cn(
        "flex items-center justify-center gap-2.25 border-b border-[#f1dcc0] bg-[linear-gradient(90deg,#fbeedd,#fdf6ea)] px-4 py-2 text-center text-[12.5px] font-semibold leading-[1.35] text-[#87501f] before:size-1.75 before:shrink-0 before:rounded-full before:bg-copper before:shadow-[0_0_0_3px_rgba(196,116,47,0.2)] before:content-[''] max-[600px]:px-3.5 max-[600px]:py-1.5 max-[600px]:text-xs",
        className,
      )}
    >
      Modo demostración · datos de prueba. Nada se envía ni se guarda en ningún servidor.
    </div>
  );
}
