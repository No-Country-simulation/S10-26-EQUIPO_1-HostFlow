import { useMemo, useState } from "react";
import { CalendarX, ChevronLeft, ChevronRight, Info } from "lucide-react";
import { Alert, Badge, Card } from "../../components/ui";
import { useStore } from "../../data/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { cn, formatDate, toISODate, weekdayShort } from "../../lib/format";

const WEEK = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"];

function startOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

export default function Availability() {
  const { actions } = useSession();
  const blockedDays = useStore((s) => s.blockedDays);
  const [month, setMonth] = useState(() => startOfMonth(new Date()));

  const today = toISODate();

  const blocked = useMemo(() => new Set(blockedDays), [blockedDays]);

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

  return (
    <div className="grid gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[clamp(24px,4vw,32px)] font-extrabold">Disponibilidad</h1>
          <p className="mt-1 text-sm text-muted">Deshabilitá los días en los que no querés recibir huéspedes.</p>
        </div>
        <Badge tone="pink">{blockedDays.length} día{blockedDays.length === 1 ? "" : "s"} bloqueado{blockedDays.length === 1 ? "" : "s"}</Badge>
      </header>

      <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <Card className="p-6">
          <div className="mb-5 flex items-center justify-between">
            <h2 className="text-lg font-bold text-ink first-letter:uppercase">{monthLabel}</h2>
            <div className="flex gap-1.5">
              <button onClick={() => move(-1)} aria-label="Mes anterior" className="grid size-9 cursor-pointer place-items-center rounded-lg border border-line text-ink-soft transition hover:bg-overlay hover:text-ink">
                <ChevronLeft size={18} />
              </button>
              <button onClick={() => move(1)} aria-label="Mes siguiente" className="grid size-9 cursor-pointer place-items-center rounded-lg border border-line text-ink-soft transition hover:bg-overlay hover:text-ink">
                <ChevronRight size={18} />
              </button>
            </div>
          </div>

          <div className="grid grid-cols-7 gap-1.5 text-center text-[11px] font-bold uppercase tracking-wide text-muted">
            {WEEK.map((d) => (
              <span key={d} className="pb-2">{d}</span>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-1.5">
            {cells.map((iso, index) => {
              if (!iso) return <span key={`blank-${index}`} />;
              const isToday = iso === today;
              const isBlocked = blocked.has(iso);
              const isPast = iso < today;
              return (
                <button
                  key={iso}
                  type="button"
                  disabled={isPast}
                  onClick={() => actions.toggleBlockedDay(iso)}
                  title={formatDate(iso)}
                  className={cn(
                    "grid aspect-square cursor-pointer place-items-center rounded-xl text-sm font-semibold transition-all",
                    isBlocked
                      ? "bg-pink text-white shadow-pop hover:bg-pink-strong"
                      : isPast
                        ? "cursor-not-allowed text-muted/40"
                        : isToday
                          ? "border-2 border-lime bg-lime-soft text-lime-ink"
                          : "bg-cloud text-ink-soft hover:border hover:border-teal hover:text-teal-strong",
                  )}
                >
                  {Number(iso.slice(-2))}
                </button>
              );
            })}
          </div>

          <div className="mt-5 flex flex-wrap items-center gap-4 text-xs text-muted">
            <span className="flex items-center gap-1.5"><span className="size-3 rounded bg-pink" /> Bloqueado</span>
            <span className="flex items-center gap-1.5"><span className="size-3 rounded border-2 border-lime bg-lime-soft" /> Hoy</span>
            <span className="flex items-center gap-1.5"><span className="size-3 rounded bg-cloud" /> Disponible</span>
          </div>
        </Card>

        <div className="grid content-start gap-4">
          <Card className="p-5">
            <h2 className="mb-3 flex items-center gap-2 font-bold text-ink">
              <CalendarX size={18} className="text-pink-strong" /> Días deshabilitados
            </h2>
            {blockedDays.length === 0 ? (
              <p className="text-sm text-muted">No deshabilitaste ningún día. Tocá un día del calendario para bloquearlo.</p>
            ) : (
              <ul className="grid gap-2">
                {[...blockedDays].sort().map((date) => (
                  <li key={date} className="flex items-center justify-between rounded-xl border border-line bg-cloud px-3.5 py-2.5 text-sm">
                    <span className="font-semibold text-ink">{formatDate(date)}</span>
                    <span className="text-xs text-muted">{weekdayShort(date)}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Alert tone="info">
            <span className="flex gap-2">
              <Info size={16} className="mt-0.5 shrink-0" />
              Los huéspedes no podrán seleccionar las fechas bloqueadas al reservar.
            </span>
          </Alert>
        </div>
      </div>
    </div>
  );
}
