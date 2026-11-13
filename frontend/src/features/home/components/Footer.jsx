import { Link } from "react-router";
import { ArrowUpRight, Sparkles } from "lucide-react";
import { Brand } from "@components/ui";

// ── Footer ──────────────────────────────────────────────────────────────────

export function Footer() {
  return (
    <footer className="relative overflow-hidden border-t border-line py-12">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 bottom-0 h-40 opacity-[0.06]"
        style={{
          backgroundImage:
            "repeating-linear-gradient(-45deg, var(--color-ink) 0 1px, transparent 1px 12px)",
        }}
      />
      <div className="relative mx-auto max-w-6xl px-5 sm:px-6">
        <div className="flex flex-col items-center justify-between gap-8 md:flex-row md:items-start">
          <div className="grid justify-items-center gap-3 md:justify-items-start">
            <Brand />
            <p className="max-w-xs text-center text-sm text-muted md:text-left">
              Gestión simple para alojamientos del Delta del Tigre.
            </p>
          </div>
          <nav className="flex flex-wrap justify-center gap-x-8 gap-y-3 text-sm font-semibold text-ink-soft">
            <a href="#caracteristicas" className="transition-colors hover:text-ink">Características</a>
            <a href="#precios" className="transition-colors hover:text-ink">Precios</a>
            <Link to="/auth/login" className="transition-colors hover:text-ink">Ingresar</Link>
            <Link to="/auth/registro" className="inline-flex items-center gap-1 transition-colors hover:text-ink">
              Crear cuenta <ArrowUpRight size={14} />
            </Link>
          </nav>
        </div>
        <div className="mt-10 flex flex-col items-center justify-between gap-3 border-t border-line pt-6 text-xs text-muted md:flex-row">
          <p>© 2026 HostFlow · NoCountry</p>
          <p className="inline-flex items-center gap-1.5">
            Hecho con dedicación en el Delta del Tigre <Sparkles size={13} className="text-teal-strong" aria-hidden="true" />
          </p>
        </div>
      </div>
    </footer>
  );
}
