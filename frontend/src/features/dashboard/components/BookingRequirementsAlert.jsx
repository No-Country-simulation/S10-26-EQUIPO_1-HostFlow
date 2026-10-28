import { Link } from "react-router";
import { ArrowRight, CircleAlert } from "lucide-react";
import { cn } from "@utils/format";

// Alerta de requisitos previos del alojamiento para poder reservar.
// Lista cada dato faltante con un acceso directo a la pantalla donde se
// completa: el anfitrión no tiene que buscar dónde arreglarlo.
// Se usa en el listado (versión amplia) y dentro del alta de reserva (`compact`,
// donde el espacio es corto y no conviene empujar los campos hacia abajo).
export function BookingRequirementsAlert({ requirements = [], intro = "Completá estos datos antes de crear la reserva.", compact = false }) {
  if (!requirements.length) return null;

  return (
    <div role="alert" className="overflow-hidden rounded-2xl border border-danger/35 bg-danger-soft">
      <div className={cn("flex items-start gap-3", compact ? "p-3.5" : "p-4 sm:p-5")}>
        <span
          className={cn(
            "grid shrink-0 place-items-center rounded-full bg-danger/15 text-danger",
            compact ? "size-8" : "size-10",
          )}
        >
          <CircleAlert size={compact ? 16 : 20} aria-hidden="true" />
        </span>

        <div className="min-w-0 flex-1">
          <div className="flex items-start justify-between gap-2">
            <p
              className={cn(
                "min-w-0 font-extrabold leading-snug text-danger",
                compact ? "text-sm" : "font-display text-base",
              )}
            >
              Falta información para poder reservar
            </p>
            <span
              className={cn(
                "shrink-0 rounded-full bg-danger font-extrabold uppercase tracking-wide text-white",
                compact ? "mt-0.5 px-1.5 py-0.5 text-[9px]" : "mt-0.5 px-2 py-0.5 text-[10px]",
              )}
            >
              {requirements.length} {requirements.length === 1 ? "dato" : "datos"}
            </span>
          </div>

          {intro && (
            <p className={cn("mt-0.5 font-medium leading-relaxed text-danger/85", compact ? "text-[11px]" : "text-xs")}>
              {intro}
            </p>
          )}

          <ul className={cn("grid", compact ? "mt-2 gap-1.5" : "mt-3 gap-2")}>
            {requirements.map((item) => (
              <li key={item.key}>
                <Link
                  to={item.to}
                  className={cn(
                    "group flex items-center justify-between gap-3 rounded-xl border border-danger/25 bg-paper font-semibold text-ink shadow-card transition-all duration-200 hover:-translate-y-px hover:border-danger hover:shadow-pop",
                    compact ? "px-3 py-1.5 text-xs" : "px-3.5 py-2.5 text-sm",
                  )}
                >
                  <span className="flex min-w-0 items-center gap-2">
                    <span className="size-1.5 shrink-0 rounded-full bg-danger" aria-hidden="true" />
                    <span className="min-w-0">{item.label}</span>
                  </span>
                  <span
                    className={cn(
                      "inline-flex shrink-0 items-center gap-1 font-extrabold text-danger",
                      compact ? "text-[10px]" : "text-xs",
                    )}
                  >
                    Ir a {item.page}
                    <ArrowRight
                      size={compact ? 11 : 13}
                      aria-hidden="true"
                      className="transition-transform group-hover:translate-x-0.5"
                    />
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
