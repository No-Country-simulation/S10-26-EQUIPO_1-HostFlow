import { Waves } from "lucide-react";
import { Link } from "react-router";
import { DEMO_MODE } from "../config/env";
import { cn } from "../lib/format";

export { cn };

export function Button({ children, variant = "primary", className, icon: Icon, ...props }) {
  return (
    <button className={cn("btn", `btn-${variant}`, className)} {...props}>
      {Icon && <Icon size={18} />}
      {children}
    </button>
  );
}

export function Field({ label, error, hint, as = "input", children, ...props }) {
  const Element = as;
  // Con <input> los hijos son un control personalizado (ej. contraseña);
  // con <select> los hijos son las <option>.
  const custom = children && as === "input";
  return (
    <label className="field">
      <span>{label}</span>
      {custom ? (
        children
      ) : (
        <Element className={cn("control", error && "control-error")} {...props}>
          {children}
        </Element>
      )}
      {error && <small className="field-error">{error}</small>}
      {hint && <small>{hint}</small>}
    </label>
  );
}

export function Brand({ light }) {
  return (
    <Link to="/" className={cn("brand", light && "brand-light")}>
      <span>
        <Waves size={20} />
      </span>
      HostFlow
    </Link>
  );
}

export function DemoBanner() {
  if (!DEMO_MODE) return null;
  return (
    <div className="demo-banner" role="note">
      Modo demostración · datos de prueba. Nada se envía ni se guarda en ningún servidor.
    </div>
  );
}
