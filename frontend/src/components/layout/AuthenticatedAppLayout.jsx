import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router";
import {
  Building2,
  CalendarDays,
  CalendarX,
  ConciergeBell,
  CreditCard,
  LayoutDashboard,
  LogOut,
  Menu,
  ShieldCheck,
  UserRound,
  X,
} from "lucide-react";
import { Avatar, Brand, ThemeToggle } from "@components/ui";
import { useSession } from "@features/auth/context/SessionContext";
import { useStore } from "@lib/state/useStore";
import { LodgingTheme } from "@lib/theme/LodgingTheme";
import { cn } from "@utils/format";

const hostNav = [
  { to: "/panel", label: "Panel", icon: LayoutDashboard, end: true },
  { to: "/panel/reservas", label: "Reservas", icon: CalendarDays },
  { to: "/panel/alojamiento", label: "Alojamiento", icon: Building2 },
  { to: "/panel/disponibilidad", label: "Disponibilidad", icon: CalendarX },
  { to: "/panel/pagos", label: "Pagos", icon: CreditCard },
  { to: "/panel/servicios", label: "Servicios", icon: ConciergeBell },
  { to: "/panel/perfil", label: "Perfil", icon: UserRound },
];

const adminNav = [{ to: "/admin", label: "Control interno", icon: ShieldCheck, end: true }];

// Estos dos componentes viven FUERA de AuthenticatedAppLayout a propósito: si se declaran
// dentro, React los remonta en cada render (pierden el foco y se reconstruyen).
function NavItems({ items, onNavigate }) {
  return (
    <nav className="grid gap-1">
      {items.map(({ to, label, icon: Icon, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          onClick={onNavigate}
          className={({ isActive }) =>
            cn(
              "group flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition-all",
              isActive
                ? "bg-lime text-lime-ink shadow-lime"
                : "text-ink-soft hover:bg-overlay hover:text-ink",
            )
          }
        >
          <Icon size={18} className="transition-transform group-hover:scale-110" />
          {label}
        </NavLink>
      ))}
    </nav>
  );
}

function UserCard({ user, isAdmin, onLogout }) {
  return (
    <div className="mt-auto rounded-2xl border border-line bg-cloud p-3">
      <div className="flex items-center gap-3">
        <Avatar name={user?.name} size="size-10" />
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-bold text-ink">{user?.name || "Invitado"}</p>
          <p className="truncate text-xs text-muted">{isAdmin ? "Administrador" : "Anfitrión"}</p>
        </div>
      </div>
      <button
        onClick={onLogout}
        className="mt-3 flex w-full cursor-pointer items-center justify-center gap-2 rounded-xl border border-line-strong bg-paper px-3 py-2.5 text-sm font-bold text-ink transition-all hover:-translate-y-px hover:border-danger hover:text-danger"
      >
        <LogOut size={16} /> Cerrar sesión
      </button>
    </div>
  );
}

export default function AuthenticatedAppLayout() {
  const { user, logout, isAdmin } = useSession();
  const lodging = useStore((s) => s.lodging);
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);

  // Drawer móvil: al abrir bloquea el scroll del fondo (el panel tiene el suyo)
  // y Escape lo cierra (consistente con el patrón del Modal).
  useEffect(() => {
    if (!mobileOpen) return undefined;
    const onKey = (event) => {
      if (event.key === "Escape") setMobileOpen(false);
    };
    document.addEventListener("keydown", onKey);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = previousOverflow;
    };
  }, [mobileOpen]);

  const nav = isAdmin ? adminNav : hostNav;

  const doLogout = () => {
    logout();
    navigate("/auth/login", { replace: true });
  };

  return (
    <LodgingTheme brand={isAdmin ? null : lodging?.brand} className="min-h-dvh text-ink">
      <div className="hf-surface min-h-dvh">
      {/* Sidebar escritorio */}
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-[264px] flex-col gap-6 border-r border-line bg-paper p-5 lg:flex">
        <Brand />
        <NavItems items={nav} />
        <UserCard user={user} isAdmin={isAdmin} onLogout={doLogout} />
      </aside>

      {/* Barra superior */}
      <header className="sticky top-0 z-30 flex items-center gap-3 border-b border-line bg-paper/85 px-4 py-3 backdrop-blur-md lg:pl-[288px] lg:pr-6">
        <button
          onClick={() => setMobileOpen(true)}
          aria-label="Abrir menú"
          className="grid size-10 cursor-pointer place-items-center rounded-xl border border-line bg-paper text-ink lg:hidden"
        >
          <Menu size={20} />
        </button>
        <div className="lg:hidden">
          <Brand size="text-[17px]" />
        </div>
        <p className="hidden text-sm font-semibold text-muted lg:block">
          {nav.find((item) => (item.end ? location.pathname === item.to : location.pathname.startsWith(item.to)))?.label ||
            "Panel"}
        </p>
        <div className="ml-auto flex items-center gap-2.5">
          <ThemeToggle />
          <Avatar name={user?.name} className="lg:hidden" />
        </div>
      </header>

      {/* Drawer móvil */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm animate-fade" onClick={() => setMobileOpen(false)} />
          <div className="absolute inset-y-0 left-0 flex w-[280px] max-w-[85vw] flex-col gap-6 overflow-y-auto overscroll-contain bg-paper p-5 animate-rise">
            <div className="flex items-center justify-between">
              <Brand />
              <button
                onClick={() => setMobileOpen(false)}
                aria-label="Cerrar menú"
                className="grid size-9 cursor-pointer place-items-center rounded-lg text-ink-soft hover:bg-overlay"
              >
                <X size={20} />
              </button>
            </div>
            <NavItems items={nav} onNavigate={() => setMobileOpen(false)} />
            <UserCard user={user} isAdmin={isAdmin} onLogout={doLogout} />
          </div>
        </div>
      )}

      <main className="min-w-0 px-4 py-6 pb-16 sm:px-6 lg:pl-[288px] lg:pr-8 lg:py-8">
        <div className="mx-auto max-w-6xl animate-rise" key={location.pathname}>
          <Outlet />
        </div>
      </main>
      </div>
    </LodgingTheme>
  );
}
