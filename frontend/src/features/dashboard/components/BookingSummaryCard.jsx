import { ArrowRight, CalendarDays, Eye, Users } from "lucide-react";
import { Badge } from "@components/ui";
import { bookingGrandTotal, bookingServices, daysBetween, formatDateShort, formatMoney, plural, serviceLineDetail, serviceLineTotal } from "@utils/format";

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

// Estado del pago de la reserva (entregable D).
const PAYMENT_STATUS = {
  PENDING: { tone: "pink", label: "Pago pendiente" },
  PAID: { tone: "success", label: "Pagado" },
};

export function PaymentStatusBadge({ status }) {
  const config = PAYMENT_STATUS[status] || PAYMENT_STATUS.PENDING;
  return <Badge tone={config.tone}>{config.label}</Badge>;
}

export function BookingSummaryCard({ booking, onConfirm, onCancel, onView }) {
  const { guestName, guestEmail, checkIn, checkOut, guests, total, currency, status, additional } = booking;
  const nights = daysBetween(checkIn, checkOut);
  // Total REAL: alojamiento + servicios adicionales registrados por el huésped.
  const services = bookingServices(booking);
  const grandTotal = bookingGrandTotal(booking);

  return (
    <article className="card card-hover p-4 sm:p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-lime-soft text-lime-ink">
            <CalendarDays size={20} />
          </span>
          <div className="min-w-0">
            <p className="truncate font-bold text-ink">{guestName}</p>
            <p className="truncate text-xs text-muted">{guestEmail}</p>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <PaymentStatusBadge status={booking.paymentStatus} />
          <StatusBadge status={status} />
        </div>
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
          {formatMoney(grandTotal, currency)}
          <span className="ml-1.5 text-xs font-semibold text-muted">{currency}</span>
          {services.length > 0 && (
            <span className="ml-2 align-middle text-xs font-semibold text-muted">
              alojamiento + {plural(services.length, "servicio", "servicios")}
            </span>
          )}
        </p>
        {(onConfirm || onCancel || onView) && (
          <div className="flex flex-wrap gap-2">
            {onView && (
              <button
                type="button"
                onClick={() => onView(booking.id)}
                aria-label={`Ver detalle de la reserva de ${booking.guestName}`}
                className="inline-flex cursor-pointer items-center gap-1.5 rounded-lg bg-ink px-3.5 py-2 text-xs font-bold text-paper transition hover:opacity-90"
              >
                <Eye size={14} aria-hidden="true" /> Ver detalle
              </button>
            )}
            {onConfirm && status === "PENDING" && (
              <button
                type="button"
                onClick={() => onConfirm(booking.id)}
                aria-label={`Confirmar la reserva de ${booking.guestName}`}
                className="cursor-pointer rounded-lg bg-teal px-3.5 py-2 text-xs font-bold text-white transition hover:bg-teal-strong"
              >
                Confirmar
              </button>
            )}
            {onCancel && status !== "CANCELLED" && status !== "COMPLETED" && (
              <button
                type="button"
                onClick={() => onCancel(booking.id)}
                aria-label={`Cancelar la reserva de ${booking.guestName}`}
                className="cursor-pointer rounded-lg border border-line-strong px-3.5 py-2 text-xs font-bold text-ink-soft transition hover:border-danger hover:text-danger"
              >
                Cancelar
              </button>
            )}
          </div>
        )}
      </div>

      {/* Detallado del total: alojamiento + adicionales registrados */}
      {services.length > 0 && (
        <details className="mt-3 border-t border-line pt-3 text-xs">
          <summary className="cursor-pointer font-bold text-ink-soft hover:text-ink">
            Ver detallado del total ({plural(services.length, "adicional", "adicionales")})
          </summary>
          <ul className="mt-2 grid gap-1 text-muted">
            <li className="flex min-w-0 items-center justify-between gap-3">
              <span className="min-w-0 truncate">Alojamiento · {plural(nights, "noche", "noches")}</span>
              <span className="shrink-0 font-semibold text-ink">{formatMoney(total, currency)}</span>
            </li>
            {services.map((item, index) => (
              <li key={item.serviceId || index} className="flex min-w-0 items-center justify-between gap-3">
                <span className="min-w-0 truncate">
                  {item.name}
                  {serviceLineDetail(item, nights, guests) && (
                    <span className="ml-1 font-semibold">{serviceLineDetail(item, nights, guests)}</span>
                  )}
                </span>
                <span className="shrink-0 font-semibold text-ink">{formatMoney(serviceLineTotal(item, nights, guests), currency)}</span>
              </li>
            ))}
            <li className="flex min-w-0 items-center justify-between gap-3 border-t border-line pt-1.5 font-bold text-ink">
              <span>Total</span>
              <span className="shrink-0">{formatMoney(grandTotal, currency)}</span>
            </li>
          </ul>
        </details>
      )}
    </article>
  );
}
