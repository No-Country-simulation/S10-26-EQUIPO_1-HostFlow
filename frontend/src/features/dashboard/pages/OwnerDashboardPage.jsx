import { Link } from "react-router";
import {
  ArrowRight,
  ArrowUpRight,
  BedDouble,
  CalendarDays,
  CalendarX,
  CreditCard,
  Sparkles,
  Wallet,
} from "lucide-react";
import { Badge, Card, Stat } from "@components/ui";
import { BookingSummaryCard } from "../components/BookingSummaryCard";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { addDays, formatMoney, toISODate } from "@utils/format";

export default function HostDashboardPage() {
  const { user, actions } = useSession();
  const bookings = useStore((s) => s.bookings);
  const lodging = useStore((s) => s.lodging);
  const blockedDays = useStore((s) => s.blockedDays);

  const today = toISODate();
  const upcoming = bookings
    .filter((b) => b.checkIn >= today && b.status !== "CANCELLED")
    .sort((a, b) => a.checkIn.localeCompare(b.checkIn));

  const next30 = addDays(today, 30);
  const next = upcoming.find((b) => b.checkIn <= next30);
  // Los ingresos se suman solo en la moneda principal del alojamiento:
  // mezclar ARS con USD/EUR daría un total sin sentido.
  const inWindow = upcoming.filter((b) => b.checkIn <= next30);
  const localBookings = inWindow.filter((b) => b.currency === lodging.currency);
  const foreignBookings = inWindow.filter((b) => b.currency !== lodging.currency);
  const revenue = localBookings.reduce((sum, b) => sum + b.total, 0);
  const pendingCount = upcoming.filter((b) => b.status === "PENDING").length;

  const quick = [
    { to: "/panel/reservas", label: "Ver reservas", icon: CalendarDays },
    { to: "/panel/alojamiento", label: "Datos del alojamiento", icon: BedDouble },
    { to: "/panel/disponibilidad", label: "Bloquear días", icon: CalendarX },
    { to: "/panel/pagos", label: "Métodos de pago", icon: CreditCard },
  ];

  return (
    <div className="grid gap-8">
      {/* Encabezado */}
      <section
        className="relative overflow-hidden rounded-[26px] p-6 text-white sm:p-8"
        style={{
          background: `linear-gradient(135deg, ${lodging.brand.primary} 0%, #0b6b6b 55%, ${lodging.brand.primary} 130%)`,
        }}
      >
        <div className="absolute inset-0 dot-grid opacity-20 [mask-image:radial-gradient(ellipse_at_right,black,transparent_70%)]" />
        <div className="relative flex flex-wrap items-end justify-between gap-5">
          <div>
            <Badge tone="lime" className="mb-3 border-0">
              <Sparkles size={12} /> Panel del anfitrión
            </Badge>
            <h1 className="text-[clamp(26px,4vw,38px)] font-extrabold leading-tight">
              Hola, {user?.name?.split(" ")[0] || "anfitrión"}
            </h1>
            <p className="mt-2 max-w-lg text-sm text-white/75">
              {lodging.name} · {lodging.location}
            </p>
          </div>
          <Link
            to="/panel/reservas"
            className="inline-flex items-center gap-2 rounded-xl bg-white/95 px-4 py-2.5 text-sm font-bold text-[#111] transition hover:-translate-y-0.5 hover:bg-white"
          >
            Gestionar reservas <ArrowRight size={16} />
          </Link>
        </div>
      </section>

      {/* Próxima reserva */}
      {next && (
        <section className="animate-rise">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-bold text-ink">Próxima llegada</h2>
            <Link to="/panel/reservas" className="inline-flex items-center gap-1 text-sm font-semibold text-teal-strong hover:text-teal">
              Ver todas <ArrowUpRight size={15} />
            </Link>
          </div>
          <BookingSummaryCard
            booking={next}
            onConfirm={(id) => actions.setBookingStatus(id, "CONFIRMED")}
            onCancel={(id) => actions.setBookingStatus(id, "CANCELLED")}
          />
        </section>
      )}

      {/* Métricas */}
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Stat label="Próximas reservas" value={upcoming.length} hint={pendingCount ? `${pendingCount} pendiente${pendingCount === 1 ? "" : "s"}` : "Todo al día"} tone="lime" icon={CalendarDays} />
        <Stat
          label="Ingresos próximos 30 días"
          value={formatMoney(revenue, lodging.currency)}
          hint={
            foreignBookings.length
              ? `+ ${foreignBookings.length} reserva${foreignBookings.length === 1 ? "" : "s"} en otra moneda`
              : undefined
          }
          tone="teal"
          icon={Wallet}
        />
        <Stat label="Habitaciones" value={lodging.rooms} hint={`${lodging.bathrooms} baños`} tone="pink" icon={BedDouble} />
        <Stat label="Días bloqueados" value={blockedDays.length} tone="lime" icon={CalendarX} />
      </section>

      {/* Reservas próximas */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-bold text-ink">Reservas próximas</h2>
          <Badge tone="teal">{upcoming.length} en total</Badge>
        </div>
        {upcoming.length === 0 ? (
          <Card className="p-8 text-center text-sm text-muted">
            No hay reservas próximas. ¡Compartí el enlace de tu alojamiento para recibir huéspedes!
          </Card>
        ) : (
          <div className="grid gap-4 lg:grid-cols-2">
            {upcoming.slice(0, 4).map((booking) => (
              <BookingSummaryCard
                key={booking.id}
                booking={booking}
                onConfirm={(id) => actions.setBookingStatus(id, "CONFIRMED")}
                onCancel={(id) => actions.setBookingStatus(id, "CANCELLED")}
              />
            ))}
          </div>
        )}
      </section>

      {/* Accesos rápidos */}
      <section>
        <h2 className="mb-3 text-lg font-bold text-ink">Accesos rápidos</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {quick.map(({ to, label, icon: Icon }) => (
            <Link key={to} to={to}>
              <Card className="card-hover group flex h-full flex-col gap-3 p-5">
                <span className="grid size-11 place-items-center rounded-xl bg-teal-soft text-teal-strong transition-colors group-hover:bg-lime-soft group-hover:text-lime-ink">
                  <Icon size={20} />
                </span>
                <span className="flex items-center justify-between font-bold text-ink">
                  {label}
                  <ArrowRight size={16} className="text-muted transition-transform group-hover:translate-x-1" />
                </span>
              </Card>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
