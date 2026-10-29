import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router";
import {
  CalendarDays,
  CalendarX,
  ChevronLeft,
  ChevronRight,
  Info,
  Trash2,
  UserCheck,
} from "lucide-react";
import { Alert, Badge, Button, Card } from "@components/ui";
import Modal from "@components/ui/Modal";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { DEMO_MODE } from "@lib/config/env";
import { addDays, cn, formatDate, toISODate, weekdayShort } from "@utils/format";

const WEEK = ["LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM"];

function startOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

export default function AvailabilityPage() {
  const { actions } = useSession();
  const blockedDays = useStore((s) => s.blockedDays);
  const bookings = useStore((s) => s.bookings);
  const [month, setMonth] = useState(() => startOfMonth(new Date()));
  const [selectedBookingDay, setSelectedBookingDay] = useState(null);
  const [syncError, setSyncError] = useState("");

  const today = toISODate();

  // Sincroniza el calendario con el backend: los días bloqueados y las reservas
  // confirmadas son la fuente de verdad de qué está ocupado. En modo demo el
  // store local ya ES la fuente, así que no hace falta pedir nada.
  useEffect(() => {
    if (DEMO_MODE) return;
    let alive = true;
    Promise.allSettled([actions.getBookings?.(), actions.getBlockedDays?.()]).then((results) => {
      if (!alive) return;
      const failed = results.some((r) => r.status === "rejected");
      setSyncError(
        failed
          ? "No se pudieron actualizar las reservas con el backend: el calendario muestra los últimos datos cargados."
          : "",
      );
    });
    return () => {
      alive = false;
    };
  }, [actions]);

  // Mapear automáticamente todos los días ocupados por reservas activas
  const bookedDaysMap = useMemo(() => {
    const map = new Map();
    (bookings || []).forEach((b) => {
      if (b.status === "CANCELLED") return;
      if (!b.checkIn || !b.checkOut) return;
      let cur = b.checkIn;
      while (cur < b.checkOut) {
        map.set(cur, b);
        cur = addDays(cur, 1);
      }
    });
    return map;
  }, [bookings]);

  const blockedManual = useMemo(() => new Set(blockedDays || []), [blockedDays]);

  // Lista combinada de todos los días deshabilitados (bloqueos manuales + ocupados por reservas)
  const allDisabledList = useMemo(() => {
    const datesMap = new Map();

    // Bloqueos manuales
    (blockedDays || []).forEach((d) => {
      if (d >= today) {
        datesMap.set(d, { date: d, isBooking: false, booking: null });
      }
    });

    // Días ocupados por reservas
    bookedDaysMap.forEach((booking, date) => {
      if (date >= today) {
        datesMap.set(date, { date, isBooking: true, booking });
      }
    });

    return Array.from(datesMap.values()).sort((a, b) => (a.date > b.date ? 1 : -1));
  }, [blockedDays, bookedDaysMap, today]);

  const cells = useMemo(() => {
    const year = month.getFullYear();
    const m = month.getMonth();
    const daysInMonth = new Date(year, m + 1, 0).getDate();
    const firstWeekday = (new Date(year, m, 1).getDay() + 6) % 7; // lunes = 0
    const result = Array.from({ length: firstWeekday }, () => null);
    for (let day = 1; day <= daysInMonth; day += 1) {
      const iso = toISODate(new Date(year, m, day));
      result.push(iso);
    }
    return result;
  }, [month]);

  const monthLabel = new Intl.DateTimeFormat("es-AR", { month: "long", year: "numeric" }).format(month);

  const move = (delta) => setMonth((current) => new Date(current.getFullYear(), current.getMonth() + delta, 1));

  const handleDayClick = (iso) => {
    if (iso < today) return;

    // Si está ocupado por una reserva de huésped, mostrar información detallada
    if (bookedDaysMap.has(iso)) {
      setSelectedBookingDay({
        date: iso,
        booking: bookedDaysMap.get(iso),
      });
      return;
    }

    // Si es un día libre o bloqueo manual, alternar bloqueo
    actions.toggleBlockedDay(iso);
  };

  return (
    <div className="grid gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[clamp(24px,4vw,32px)] font-extrabold text-ink">Disponibilidad</h1>
          <p className="mt-1 text-sm text-muted">
            Deshabilita los días en los que no querés recibir huéspedes.
          </p>
        </div>
        <Badge tone="pink" className="px-3.5 py-1.5 text-xs font-extrabold uppercase tracking-wider">
          {allDisabledList.length} {allDisabledList.length === 1 ? "DÍA BLOQUEADO" : "DÍAS BLOQUEADOS"}
        </Badge>
      </header>

      {syncError && <Alert>{syncError}</Alert>}

      <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]">
        {/* Calendario interactivo */}
        <Card className="p-5 sm:p-7">
          <div className="mb-6 flex items-center justify-between">
            <h2 className="text-xl font-extrabold text-ink first-letter:uppercase">{monthLabel}</h2>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => move(-1)}
                aria-label="Mes anterior"
                className="grid size-9 cursor-pointer place-items-center rounded-xl border border-line-strong text-ink-soft transition hover:border-ink hover:text-ink"
              >
                <ChevronLeft size={18} />
              </button>
              <button
                type="button"
                onClick={() => move(1)}
                aria-label="Mes siguiente"
                className="grid size-9 cursor-pointer place-items-center rounded-xl border border-line-strong text-ink-soft transition hover:border-ink hover:text-ink"
              >
                <ChevronRight size={18} />
              </button>
            </div>
          </div>

          <div className="grid grid-cols-7 gap-2 text-center text-xs font-bold uppercase tracking-wider text-muted">
            {WEEK.map((d) => (
              <span key={d} className="pb-3">
                {d}
              </span>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-2">
            {cells.map((iso, index) => {
              if (!iso) return <span key={`blank-${index}`} />;
              const isToday = iso === today;
              const hasBooking = bookedDaysMap.has(iso);
              const isManualBlocked = blockedManual.has(iso);
              const isBlocked = hasBooking || isManualBlocked;
              const isPast = iso < today;

              const booking = hasBooking ? bookedDaysMap.get(iso) : null;
              const titleText = hasBooking
                ? `Ocupado: Reserva de ${booking.guestName} (${booking.id})`
                : isManualBlocked
                  ? `Bloqueado manualmente: ${formatDate(iso)}`
                  : isToday
                    ? `Hoy: ${formatDate(iso)}`
                    : `Disponible: ${formatDate(iso)}`;

              // Días pasados: números atenuados sin tarjeta
              if (isPast) {
                return (
                  <div
                    key={iso}
                    className="grid aspect-square place-items-center text-sm font-semibold text-muted/30 select-none"
                  >
                    {Number(iso.slice(-2))}
                  </div>
                );
              }

              // Hoy: tarjeta lima destacada
              if (isToday) {
                return (
                  <button
                    key={iso}
                    type="button"
                    onClick={() => handleDayClick(iso)}
                    title={titleText}
                    className="grid aspect-square cursor-pointer place-items-center rounded-2xl bg-lime text-base font-extrabold text-lime-ink shadow-sm transition hover:scale-[1.03] active:scale-[0.98]"
                  >
                    {Number(iso.slice(-2))}
                  </button>
                );
              }

              // Ocupado por reserva o bloqueado manualmente: el de reserva usa
              // un rosa más intenso y un punto, para distinguirlo de un bloqueo.
              if (isBlocked) {
                return (
                  <button
                    key={iso}
                    type="button"
                    onClick={() => handleDayClick(iso)}
                    title={titleText}
                    className={cn(
                      "relative grid aspect-square cursor-pointer place-items-center rounded-2xl text-base font-extrabold shadow-sm transition hover:scale-[1.03] hover:brightness-95 active:scale-[0.98]",
                      hasBooking ? "bg-pink-strong text-white" : "bg-pink text-ink",
                    )}
                  >
                    {Number(iso.slice(-2))}
                    {hasBooking && (
                      <span className="absolute bottom-1.5 size-1.5 rounded-full bg-white/85" />
                    )}
                  </button>
                );
              }

              // Disponible futuro: tarjeta con fondo sutil
              return (
                <button
                  key={iso}
                  type="button"
                  onClick={() => handleDayClick(iso)}
                  title={titleText}
                  className="grid aspect-square cursor-pointer place-items-center rounded-2xl bg-cloud dark:bg-[#1e1e1e] text-base font-bold text-ink dark:text-white/90 shadow-sm transition hover:border hover:border-teal hover:text-teal-strong hover:scale-[1.03] active:scale-[0.98]"
                >
                  {Number(iso.slice(-2))}
                </button>
              );
            })}
          </div>

          {/* Leyenda inferior */}
          <div className="mt-8 flex flex-wrap items-center gap-6 border-t border-line pt-4 text-xs font-semibold text-muted">
            <span className="flex items-center gap-2">
              <span className="size-3.5 rounded-md bg-pink shrink-0" />
              <span>Bloqueado</span>
            </span>
            <span className="flex items-center gap-2">
              <span className="relative size-3.5 rounded-md bg-pink-strong shrink-0">
                <span className="absolute bottom-0.5 left-1/2 size-1 -translate-x-1/2 rounded-full bg-white/85" />
              </span>
              <span>Ocupado por reserva</span>
            </span>
            <span className="flex items-center gap-2">
              <span className="size-3.5 rounded-md bg-lime shrink-0" />
              <span>Hoy</span>
            </span>
            <span className="flex items-center gap-2">
              <span className="size-3.5 rounded-md bg-cloud dark:bg-[#1e1e1e] border border-line-strong shrink-0" />
              <span>Disponible</span>
            </span>
          </div>
        </Card>

        {/* Panel lateral: Días deshabilitados */}
        <div className="grid content-start gap-4">
          <Card className="p-5">
            <h2 className="mb-4 flex items-center gap-2 text-base font-bold text-ink">
              <CalendarX size={18} className="text-pink-strong" /> Días deshabilitados
            </h2>

            {allDisabledList.length === 0 ? (
              <p className="text-sm text-muted">
                No hay días deshabilitados ni reservas para este período. Tocá cualquier fecha en el calendario para bloquearla.
              </p>
            ) : (
              <ul className="grid max-h-[460px] gap-2.5 overflow-y-auto pr-1">
                {allDisabledList.map((item) => {
                  const dayName = weekdayShort(item.date).replace(".", "").toLowerCase();
                  return (
                    <li
                      key={item.date}
                      className="flex items-center justify-between gap-2 rounded-2xl border border-line bg-paper dark:bg-[#181818] px-4 py-3 text-sm shadow-sm transition hover:border-line-strong"
                    >
                      <div className="min-w-0">
                        <span className="block font-bold text-ink">{formatDate(item.date)}</span>
                        {item.isBooking ? (
                          <span className="mt-0.5 inline-flex items-center gap-1 rounded-full bg-lime-soft px-2 py-0.5 text-[10px] font-extrabold text-lime-ink">
                            <UserCheck size={11} /> Reserva · {item.booking?.guestName}
                          </span>
                        ) : (
                          <span className="mt-0.5 inline-block text-[11px] font-medium text-muted">
                            Bloqueo manual
                          </span>
                        )}
                      </div>

                      <div className="flex shrink-0 items-center gap-2.5">
                        <span className="text-xs font-bold uppercase tracking-wider text-muted">
                          {dayName}
                        </span>
                        {!item.isBooking && (
                          <button
                            type="button"
                            onClick={() => actions.toggleBlockedDay(item.date)}
                            title="Desbloquear fecha"
                            className="cursor-pointer rounded-lg p-1 text-muted transition hover:bg-danger-soft hover:text-danger"
                          >
                            <Trash2 size={14} />
                          </button>
                        )}
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}
          </Card>

          {/* Cartel informativo inferior (estilo exacto a la imagen del usuario) */}
          <div className="flex items-start gap-3 rounded-2xl border border-teal/40 bg-teal-soft/15 dark:bg-teal-soft/10 p-4 text-xs font-medium text-teal-strong">
            <Info size={17} className="mt-0.5 shrink-0 text-teal" />
            <span>Los huéspedes no podrán seleccionar las fechas bloqueadas al reservar.</span>
          </div>
        </div>
      </div>

      {/* Modal informativo al hacer clic sobre una fecha ocupada por una reserva */}
      {selectedBookingDay && (
        <Modal
          open={Boolean(selectedBookingDay)}
          onClose={() => setSelectedBookingDay(null)}
          title="Día ocupado por reserva"
          subtitle={`Fecha: ${formatDate(selectedBookingDay.date)}`}
        >
          <div className="grid gap-4 text-sm">
            <div className="rounded-2xl border border-line bg-cloud p-4">
              <p className="text-xs uppercase font-extrabold tracking-wider text-muted">Huésped</p>
              <p className="text-lg font-bold text-ink">{selectedBookingDay.booking.guestName}</p>
              <p className="text-xs text-muted">
                {selectedBookingDay.booking.guestEmail} · {selectedBookingDay.booking.guestPhone}
              </p>

              <div className="mt-3 grid gap-1 border-t border-line pt-2 text-xs">
                <p>
                  <span className="font-semibold text-ink">Estadía:</span>{" "}
                  {formatDate(selectedBookingDay.booking.checkIn)} → {formatDate(selectedBookingDay.booking.checkOut)}
                </p>
                <p>
                  <span className="font-semibold text-ink">Huéspedes:</span> {selectedBookingDay.booking.guests} personas
                </p>
                <p>
                  <span className="font-semibold text-ink">Estado:</span>{" "}
                  <Badge tone={selectedBookingDay.booking.status === "CONFIRMED" ? "success" : "neutral"} size="sm">
                    {selectedBookingDay.booking.status === "CONFIRMED" ? "Confirmada" : "Pendiente"}
                  </Badge>
                </p>
              </div>
            </div>

            <p className="text-xs text-muted">
              Esta fecha está bloqueada automáticamente porque el huésped tiene asignado el alojamiento durante estos días. Para liberar el calendario, podés editar o cancelar la reserva en el módulo correspondiente.
            </p>

            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="secondary" onClick={() => setSelectedBookingDay(null)}>
                Cerrar
              </Button>
              <Link to="/panel/reservas">
                <Button type="button">Ir a Reservas</Button>
              </Link>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}