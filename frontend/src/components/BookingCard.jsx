import { ArrowRight, CalendarDays, Users } from "lucide-react";
import { Badge } from "./ui";
import { daysBetween, formatDateShort, formatMoney } from "../lib/format";

const STATUS = {
  CONFIRMED: { tone: "success", label: "Confirmada" },
  PENDING: { tone: "pink", label: "Pendiente" },
  CANCELLED: { tone: "danger", label: "Cancelada" },
  COMPLETED: { tone: "neutral", label: "Completada" },
};

export function StatusBadge({ status }) {
  const config = STATUS[status] || { tone: "neutral", label: status };
  return <Badge tone={config.tone}>{config.label}</Badge>;
}

export function BookingCard({ booking, onConfirm, onCancel }) {
  const { guestName, guestEmail, checkIn, checkOut, guests, total, currency, status, additional } = booking;
  const nights = daysBetween(checkIn, checkOut);

  return (
    <article className="card card-hover p-4 sm:p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <span className="grid size-11 place-items-center rounded-xl bg-lime-soft text-lime-ink">
            <CalendarDays size={20} />
          </span>
          <div>
            <p className="font-bold text-ink">{guestName}</p>
            <p className="text-xs text-muted">{guestEmail}</p>
          </div>
        </div>
        <StatusBadge status={status} />
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-x-5 gap-y-2 text-sm">
        <span className="font-semibold text-ink">
          {formatDateShort(checkIn)} <ArrowRight className="inline w-3.5" /> {formatDateShort(checkOut)}
        </span>
        <span className="inline-flex items-center gap-1.5 text-muted">
          <Users size={15} /> {guests} huésped{guests === 1 ? "" : "es"}
        </span>
        <span className="text-muted">{nights} noche{nights === 1 ? "" : "s"}</span>
        {additional && <span className="rounded-full bg-teal-soft px-2.5 py-1 text-xs font-semibold text-teal-strong">{additional}</span>}
      </div>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-line pt-3.5">
        <p className="text-lg font-extrabold text-ink">
          {formatMoney(total, currency)}
          <span className="ml-1.5 text-xs font-semibold text-muted">{currency}</span>
        </p>
        {(onConfirm || onCancel) && (
          <div className="flex gap-2">
            {onConfirm && status === "PENDING" && (
              <button
                onClick={() => onConfirm(booking.id)}
                className="cursor-pointer rounded-lg bg-teal px-3.5 py-2 text-xs font-bold text-white transition hover:bg-teal-strong"
              >
                Confirmar
              </button>
            )}
            {onCancel && status !== "CANCELLED" && status !== "COMPLETED" && (
              <button
                onClick={() => onCancel(booking.id)}
                className="cursor-pointer rounded-lg border border-line-strong px-3.5 py-2 text-xs font-bold text-ink-soft transition hover:border-danger hover:text-danger"
              >
                Cancelar
              </button>
            )}
          </div>
        )}
      </div>
    </article>
  );
}
