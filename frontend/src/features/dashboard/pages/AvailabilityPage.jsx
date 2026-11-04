import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router";
import {
  ArrowUpRight,
  CalendarDays,
  CalendarX,
  ChevronLeft,
  ChevronRight,
  Info,
  Trash2,
  Users,
} from "lucide-react";
import { Alert, Badge, Card } from "@components/ui";
import { BookingDetailModal } from "../components/BookingDetailModal";
import { useBlockedDays, useBookings, useBrand } from "@lib/api/hooks";
import { useSession } from "@features/auth/context/SessionContext";
import { useTheme } from "@lib/theme/ThemeContext";
import { DEMO_MODE } from "@lib/config/env";
import { mixHex, normalizeHex, readableOn, visibleOn } from "@utils/color";
import { addDays, cn, formatDateShort, plural, toISODate, weekdayShort } from "@utils/format";

const WEEK = ["LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM"];

// Máximo 4 tonos del MISMO color de marca para diferenciar reservas en el
// calendario: la diferencia se da por OPACIDAD, nunca por tonos distintos.
const TONES = [0.26, 0.44, 0.62, 0.8];

// Fondos reales de cada tema (styles/globals.css: --color-paper).
const PAPER = { light: "#ffffff", dark: "#101518" };

function startOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function endOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0);
}

export default function AvailabilityPage() {
  const { actions } = useSession();
  const { isDark } = useTheme();
  const blockedDays = useBlockedDays();
  const bookings = useBookings();
  const brand = useBrand();
  const [month, setMonth] = useState(() => startOfMonth(new Date()));
  const [detailId, setDetailId] = useState(null);
  // Reserva resaltada al pasar el cursor por su tarjeta del panel lateral:
  // sus días en el calendario reciben un anillo para ubicarlas de un vistazo.
  const [highlightId, setHighlightId] = useState(null);
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

  // Reservas activas en orden estable (llegada y luego id): de ese orden sale
  // el tono de cada una dentro del tope de 4.
  const activeBookings = useMemo(
    () =>
      (bookings || [])
        .filter((b) => b.status !== "CANCELLED" && b.checkIn && b.checkOut)
        .sort((a, b) => a.checkIn.localeCompare(b.checkIn) || String(a.id).localeCompare(String(b.id))),
    [bookings],
  );

  // Tono (0-1 de opacidad del color de marca) por reserva.
  const toneByBooking = useMemo(() => {
    const map = new Map();
    activeBookings.forEach((booking, index) => {
      map.set(String(booking.id), TONES[index % TONES.length]);
    });
    return map;
  }, [activeBookings]);

  // Color de marca del alojamiento: el "principal" (Marca) es el que pinta las
  // reservas; en modo oscuro se aclara hasta distinguirse del fondo, igual que
  // hace LodgingTheme con la identidad del alojamiento.
  const brandPrimary = normalizeHex(brand?.primary, "#0fa3a3");
  const bookingColor = isDark ? visibleOn(brandPrimary, PAPER.dark, 2.9) : brandPrimary;

  // Pintura de una reserva: mismo color, distinta opacidad, mezclado sobre el
  // papel del tema. El texto se elige por contraste real sobre ese resultado.
  const skinFor = (booking) => {
    const tone = toneByBooking.get(String(booking.id)) ?? TONES[0];
    const bg = mixHex(PAPER[isDark ? "dark" : "light"], bookingColor, tone);
    return { backgroundColor: bg, color: readableOn(bg) };
  };

  // Mapear automáticamente todos los días ocupados por reservas activas
  const bookedDaysMap = useMemo(() => {
    const map = new Map();
    activeBookings.forEach((b) => {
      let cur = b.checkIn;
      while (cur < b.checkOut) {
        map.set(cur, b);
        cur = addDays(cur, 1);
      }
    });
    return map;
  }, [activeBookings]);

  const blockedManual = useMemo(() => new Set(blockedDays || []), [blockedDays]);

  // Días bloqueados a mano (los de reserva se ven en el panel lateral).
  const manualBlocked = useMemo(
    () => (blockedDays || []).filter((d) => d >= today).sort((a, b) => (a > b ? 1 : -1)),
    [blockedDays, today],
  );

  // Reservas que tocan el mes mostrado: es lo que lista el panel lateral.
  const monthBookings = useMemo(() => {
    const first = toISODate(startOfMonth(month));
    const last = toISODate(endOfMonth(month));
    return activeBookings.filter((b) => b.checkIn <= last && b.checkOut > first);
  }, [activeBookings, month]);

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
    // Día ocupado por reserva → detalle completo de la reserva.
    if (bookedDaysMap.has(iso)) {
      setDetailId(bookedDaysMap.get(iso).id);
      return;
    }
    // Día libre o bloqueado a mano → alternar bloqueo.
    actions.toggleBlockedDay(iso);
  };

  // Detalle vivo: si la reserva cambia (estado, pago) el modal abierto la ve.
  const detail = useMemo(() => (bookings || []).find((b) => b.id === detailId) || null, [bookings, detailId]);

  // Muestra de los 4 tonos para la leyenda.
  const legendTones = TONES.map((tone) => {
    const bg = mixHex(PAPER[isDark ? "dark" : "light"], bookingColor, tone);
    return bg;
  });

  return (
    <div className="grid gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[clamp(24px,4vw,32px)] font-extrabold text-ink">Disponibilidad</h1>
          <p className="mt-1 text-sm text-muted">
            Deshabilitá los días libres y seguí las reservas de un vistazo.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2.5">
          <Badge tone="teal" className="px-3.5 py-1.5 text-xs font-extrabold uppercase tracking-wider">
            {monthBookings.length} {monthBookings.length === 1 ? "RESERVA" : "RESERVAS"}
          </Badge>
          <Badge tone="pink" className="px-3.5 py-1.5 text-xs font-extrabold uppercase tracking-wider">
            {manualBlocked.length} {manualBlocked.length === 1 ? "DÍA BLOQUEADO" : "DÍAS BLOQUEADOS"}
          </Badge>
        </div>
      </header>

      {syncError && <Alert>{syncError}</Alert>}

      <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]">
        {/* Calendario interactivo */}
        <Card className="p-5 sm:p-7">
          <div className="mb-6 flex items-center justify-between gap-3">
            <div className="min-w-0">
              <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-muted">Calendario</p>
              <h2 className="truncate text-xl font-extrabold text-ink first-letter:uppercase">{monthLabel}</h2>
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => move(-1)}
                aria-label="Mes anterior"
                className="grid size-9 cursor-pointer place-items-center rounded-xl border border-line-strong text-ink-soft transition hover:-translate-y-px hover:border-ink hover:bg-overlay hover:text-ink"
              >
                <ChevronLeft size={18} />
              </button>
              <button
                type="button"
                onClick={() => move(1)}
                aria-label="Mes siguiente"
                className="grid size-9 cursor-pointer place-items-center rounded-xl border border-line-strong text-ink-soft transition hover:-translate-y-px hover:border-ink hover:bg-overlay hover:text-ink"
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

          {/* key en el mes: al cambiar de mes la grilla entra con un fundido */}
          <div key={month.toISOString()} className="grid grid-cols-7 gap-2 animate-fade">
            {cells.map((iso, index) => {
              if (!iso) return <span key={`blank-${index}`} />;
              const isToday = iso === today;
              const hasBooking = bookedDaysMap.has(iso);
              const isManualBlocked = blockedManual.has(iso);
              const isBlocked = hasBooking || isManualBlocked;
              const isPast = iso < today;

              const booking = hasBooking ? bookedDaysMap.get(iso) : null;
              const highlighted = Boolean(booking && highlightId && String(booking.id) === String(highlightId));
              const titleText = hasBooking
                ? `Ocupado: Reserva de ${booking.guestName} (${booking.id})`
                : isManualBlocked
                  ? `Bloqueado manualmente: ${formatDateShort(iso)}`
                  : isToday
                    ? `Hoy`
                    : `Disponible`;

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

              const cellClass = cn(
                "relative grid aspect-square cursor-pointer place-items-center rounded-2xl text-base shadow-sm transition-all duration-200 hover:-translate-y-0.5 active:scale-[0.97]",
                highlighted && "ring-2 ring-ink ring-offset-2 ring-offset-paper scale-[1.06] z-10",
              );

              // Hoy: tarjeta lima destacada
              if (isToday) {
                return (
                  <button
                    key={iso}
                    type="button"
                    onClick={() => handleDayClick(iso)}
                    title={titleText}
                    className={cn(
                      cellClass,
                      "bg-lime font-extrabold text-lime-ink hover:bg-lime-strong",
                    )}
                  >
                    {Number(iso.slice(-2))}
                    <span className="absolute bottom-1.5 size-1.5 rounded-full bg-lime-ink/60" />
                  </button>
                );
              }

              // Ocupado por reserva: color de marca en uno de sus 4 tonos.
              if (hasBooking) {
                return (
                  <button
                    key={iso}
                    type="button"
                    onClick={() => handleDayClick(iso)}
                    title={titleText}
                    style={skinFor(booking)}
                    className={cn(cellClass, "font-extrabold hover:brightness-95")}
                  >
                    {Number(iso.slice(-2))}
                    <span
                      className="absolute bottom-1.5 size-1.5 rounded-full"
                      style={{ backgroundColor: "currentColor", opacity: 0.55 }}
                    />
                  </button>
                );
              }

              // Bloqueado manualmente: se distingue de las reservas.
              if (isManualBlocked) {
                return (
                  <button
                    key={iso}
                    type="button"
                    onClick={() => handleDayClick(iso)}
                    title={titleText}
                    className={cn(
                      cellClass,
                      "border border-dashed border-pink-strong bg-pink/45 font-extrabold text-ink hover:brightness-95",
                    )}
                  >
                    {Number(iso.slice(-2))}
                    <span className="absolute bottom-1.5 size-1.5 rounded-full bg-pink-strong/80" />
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
                  className={cn(
                    cellClass,
                    "bg-cloud font-bold text-ink dark:bg-[#1e1e1e] dark:text-white/90 hover:border hover:border-teal hover:text-teal-strong",
                  )}
                >
                  {Number(iso.slice(-2))}
                </button>
              );
            })}
          </div>

          {/* Leyenda inferior */}
          <div className="mt-8 flex flex-wrap items-center gap-x-6 gap-y-3 border-t border-line pt-4 text-xs font-semibold text-muted">
            <span className="flex items-center gap-2">
              <span className="flex overflow-hidden rounded-md ring-1 ring-black/10" aria-hidden="true">
                {legendTones.map((bg, index) => (
                  <span key={index} className="size-3.5" style={{ backgroundColor: bg }} />
                ))}
              </span>
              <span>Reserva · 4 tonos de tu color</span>
            </span>
            <span className="flex items-center gap-2">
              <span className="size-3.5 rounded-md border border-dashed border-pink-strong bg-pink/45 shrink-0" />
              <span>Bloqueado</span>
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

        {/* Panel lateral: una tarjeta POR RESERVA (no por día) */}
        <div className="grid content-start gap-4">
          <Card className="p-5">
            <div className="mb-4 flex items-center justify-between gap-2">
              <h2 className="flex items-center gap-2 text-base font-bold text-ink">
                <CalendarDays size={18} className="text-teal-strong" /> Reservas del mes
              </h2>
              <Badge tone="neutral">{monthBookings.length}</Badge>
            </div>

            {monthBookings.length === 0 ? (
              <div className="grid place-items-center gap-2 rounded-2xl border border-dashed border-line-strong px-4 py-8 text-center">
                <CalendarDays size={22} className="text-muted" aria-hidden="true" />
                <p className="text-sm font-bold text-ink">Sin reservas este mes</p>
                <p className="text-xs text-muted">
                  Cambiá de mes con las flechas o creá una desde Reservas.
                </p>
              </div>
            ) : (
              <ul className="grid max-h-[460px] gap-2.5 overflow-y-auto pr-1">
                {monthBookings.map((booking) => {
                  const skin = skinFor(booking);
                  const nights = Math.max(
                    1,
                    Math.round(
                      (new Date(booking.checkOut) - new Date(booking.checkIn)) / 86_400_000,
                    ),
                  );
                  return (
                    <li
                      key={booking.id}
                      onMouseEnter={() => setHighlightId(booking.id)}
                      onMouseLeave={() => setHighlightId(null)}
                      className="rounded-2xl border border-line bg-paper p-3.5 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-line-strong hover:shadow-card"
                    >
                      <div className="flex min-w-0 items-start gap-3">
                        <span
                          className="mt-1 size-3.5 shrink-0 rounded-md ring-1 ring-black/10"
                          style={{ backgroundColor: skin.backgroundColor }}
                          aria-hidden="true"
                        />
                        <div className="min-w-0 flex-1">
                          <p className="truncate font-bold text-ink">{booking.guestName}</p>
                          <p className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted">
                            <span className="inline-flex items-center gap-1.5">
                              <Users size={13} aria-hidden="true" /> {plural(booking.guests, "huésped", "huéspedes")}
                            </span>
                            <span className="inline-flex items-center gap-1.5">
                              <CalendarDays size={13} aria-hidden="true" /> {plural(nights, "noche", "noches")}
                            </span>
                          </p>
                          <p className="mt-1.5 text-xs font-semibold text-ink-soft">
                            Llegada: {weekdayShort(booking.checkIn).replace(".", "")} {formatDateShort(booking.checkIn)}
                            <span className="text-muted"> · sale {formatDateShort(booking.checkOut)}</span>
                          </p>
                        </div>
                        <Badge tone={booking.status === "CONFIRMED" ? "success" : "pink"} className="shrink-0">
                          {booking.status === "CONFIRMED" ? "Confirmada" : "Pendiente"}
                        </Badge>
                      </div>

                      <div className="mt-3 flex items-center justify-between gap-2 border-t border-line pt-2.5">
                        <span className="text-[11px] font-semibold uppercase tracking-wider text-muted">
                          {booking.id}
                        </span>
                        <button
                          type="button"
                          onClick={() => setDetailId(booking.id)}
                          className="inline-flex cursor-pointer items-center gap-1.5 rounded-lg bg-ink px-3 py-1.5 text-xs font-bold text-paper transition hover:-translate-y-px hover:opacity-90"
                        >
                          Ver detalles <ArrowUpRight size={13} aria-hidden="true" />
                        </button>
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}

            <Link
              to="/panel/reservas"
              className="mt-4 inline-flex items-center gap-1.5 text-xs font-bold text-teal-strong transition-colors hover:text-teal"
            >
              Gestionar todas las reservas <ArrowUpRight size={14} aria-hidden="true" />
            </Link>
          </Card>

          {/* Días bloqueados a mano (siguen editables desde el calendario) */}
          <Card className="p-5">
            <h2 className="mb-3 flex items-center gap-2 text-base font-bold text-ink">
              <CalendarX size={18} className="text-pink-strong" /> Días bloqueados
            </h2>
            {manualBlocked.length === 0 ? (
              <p className="text-sm text-muted">
                No hay días bloqueados. Tocá una fecha libre del calendario para bloquearla.
              </p>
            ) : (
              <ul className="grid max-h-44 gap-2 overflow-y-auto pr-1">
                {manualBlocked.map((date) => (
                  <li
                    key={date}
                    className="flex items-center justify-between gap-2 rounded-xl border border-line bg-cloud px-3 py-2 text-sm transition hover:border-line-strong"
                  >
                    <span className="min-w-0 truncate font-semibold text-ink">
                      {weekdayShort(date).replace(".", "")} · {formatDateShort(date)}
                    </span>
                    <button
                      type="button"
                      onClick={() => actions.toggleBlockedDay(date)}
                      title="Desbloquear fecha"
                      className="cursor-pointer rounded-lg p-1 text-muted transition hover:bg-danger-soft hover:text-danger"
                    >
                      <Trash2 size={14} aria-hidden="true" />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          {/* Cartel informativo inferior */}
          <div className="flex items-start gap-3 rounded-2xl border border-teal/40 bg-teal-soft/15 dark:bg-teal-soft/10 p-4 text-xs font-medium text-teal-strong">
            <Info size={17} className="mt-0.5 shrink-0 text-teal" />
            <span>Los huéspedes no podrán seleccionar las fechas bloqueadas al reservar.</span>
          </div>
        </div>
      </div>

      {/* Detalle completo de la reserva (mismo modal que Reservas) */}
      <BookingDetailModal booking={detail} open={Boolean(detail)} onClose={() => setDetailId(null)} />
    </div>
  );
}
