import { useEffect, useState } from "react";
import { Link } from "react-router";
import { Menu, X } from "lucide-react";
import { Brand, buttonClass, ThemeToggle } from "@components/ui";
import { cn } from "@utils/format";

// ── Header ──────────────────────────────────────────────────────────────────

export function Header() {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header
      className={cn(
        "sticky top-0 z-50 transition-all duration-300",
        scrolled ? "glass border-b border-line" : "bg-transparent",
      )}
    >
      <div className="mx-auto flex max-w-6xl items-center justify-between px-5 py-4 sm:px-6">
        <Brand />

        <nav className="hidden items-center gap-1 md:flex">
          {[
            ["#caracteristicas", "Características"],
            ["#como-funciona", "Cómo funciona"],
            ["#precios", "Precios"],
          ].map(([href, label]) => (
            <a
              key={href}
              href={href}
              className="relative rounded-lg px-4 py-2 text-sm font-semibold text-ink-soft transition-colors hover:text-ink"
            >
              {label}
            </a>
          ))}
        </nav>

        <div className="hidden items-center gap-2.5 md:flex">
          <ThemeToggle />
          <Link to="/auth/login" className="px-4 py-2 text-sm font-semibold text-ink-soft transition-colors hover:text-ink">
            Ingresar
          </Link>
          <Link to="/auth/registro" className={buttonClass("primary", "min-h-[44px]")}>
            Crear cuenta
          </Link>
        </div>

        <div className="flex items-center gap-2 md:hidden">
          <ThemeToggle />
          <button onClick={() => setOpen((v) => !v)} aria-label="Menú" className="grid size-10 cursor-pointer place-items-center rounded-xl border border-line bg-paper text-ink">
            {open ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>
      </div>

      {open && (
        <div className="glass border-t border-line px-5 py-4 md:hidden animate-fade">
          <nav className="grid gap-1">
            {[
              ["#caracteristicas", "Características"],
              ["#como-funciona", "Cómo funciona"],
              ["#precios", "Precios"],
            ].map(([href, label]) => (
              <a key={href} href={href} onClick={() => setOpen(false)} className="rounded-lg px-3 py-2.5 text-sm font-semibold text-ink-soft hover:bg-overlay">
                {label}
              </a>
            ))}
          </nav>
          <div className="mt-3 grid grid-cols-2 gap-2">
            <Link to="/auth/login" onClick={() => setOpen(false)} className={buttonClass("secondary")}>
              Ingresar
            </Link>
            <Link to="/auth/registro" onClick={() => setOpen(false)} className={buttonClass("primary")}>
              Crear cuenta
            </Link>
          </div>
        </div>
      )}
    </header>
  );
}
