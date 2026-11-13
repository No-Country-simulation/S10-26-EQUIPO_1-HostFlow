import { CalendarDays, MapPin, Users } from "lucide-react";
import { formatDate, plural } from "@utils/format";

// Tarjeta de la estadía (fechas, noches, huéspedes y ubicación) con la marca.
// La comparten la portada del onboarding y el paso de datos para que el huésped
// vea siempre la misma referencia de su reserva.
export default function StayCard({ booking, lodging, nights }) {
  if (!booking) return null;

  return (
    <div className="rounded-2xl border border-teal/40 bg-teal-soft p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-start gap-2.5">
          <span className="mt-0.5 grid size-9 shrink-0 place-items-center rounded-[var(--radius-chip)] bg-paper/70 text-teal-strong">
            <CalendarDays size={17} aria-hidden="true" />
          </span>
          <div>
            <p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-teal-strong">Tu estadía</p>
            <p className="mt-0.5 text-sm font-extrabold text-ink">
              {formatDate(booking.checkIn)} → {formatDate(booking.checkOut)}
            </p>
          </div>
        </div>
        <span className="rounded-full bg-lime-soft px-2.5 py-1 text-[11px] font-extrabold text-lime-ink">
          {plural(nights, "noche", "noches")}
        </span>
      </div>
      <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1.5 border-t border-teal/20 pt-3 text-xs font-semibold text-ink-soft">
        <span className="inline-flex items-center gap-1.5">
          <Users size={13} aria-hidden="true" /> {plural(booking.guests, "huésped", "huéspedes")}
        </span>
        <span className="inline-flex min-w-0 items-center gap-1.5">
          <MapPin size={13} className="shrink-0" aria-hidden="true" />
          <span className="truncate">{lodging.location}</span>
        </span>
      </div>
    </div>
  );
}
