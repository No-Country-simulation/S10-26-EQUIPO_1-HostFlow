import { useMemo, useState } from "react";
import { CalendarDays, Search } from "lucide-react";
import { Badge, EmptyState } from "@components/ui";
import { BookingSummaryCard } from "../components/BookingSummaryCard";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { cn, toISODate } from "@lib/format";

const FILTERS = [
  { id: "all", label: "Todas" },
  { id: "upcoming", label: "Próximas" },
  { id: "pending", label: "Pendientes" },
  { id: "past", label: "Pasadas" },
];

export default function BookingsPage() {
  const { actions } = useSession();
  const bookings = useStore((s) => s.bookings);
  const [filter, setFilter] = useState("all");
  const [query, setQuery] = useState("");

  const today = toISODate();

  const list = useMemo(() => {
    const q = query.trim().toLowerCase();
    return bookings
      .filter((b) => {
        if (filter === "upcoming") return b.checkIn >= today && b.status !== "CANCELLED";
        if (filter === "pending") return b.status === "PENDING";
        if (filter === "past") return b.checkIn < today || b.status === "COMPLETED";
        return true;
      })
      .filter((b) => !q || `${b.guestName} ${b.guestEmail}`.toLowerCase().includes(q))
      .sort((a, b) => b.checkIn.localeCompare(a.checkIn));
  }, [bookings, filter, query, today]);

  return (
    <div className="grid gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[clamp(24px,4vw,32px)] font-extrabold">Reservas</h1>
          <p className="mt-1 text-sm text-muted">Gestioná las llegadas y el estado de cada reserva.</p>
        </div>
        <Badge tone="teal">{list.length} resultado{list.length === 1 ? "" : "s"}</Badge>
      </header>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap gap-2">
          {FILTERS.map((item) => (
            <button
              key={item.id}
              onClick={() => setFilter(item.id)}
              className={cn(
                "cursor-pointer rounded-full px-4 py-2 text-xs font-bold transition-all",
                filter === item.id
                  ? "bg-ink text-paper"
                  : "border border-line bg-paper text-ink-soft hover:border-line-strong hover:text-ink",
              )}
            >
              {item.label}
            </button>
          ))}
        </div>
        <div className="relative sm:w-64">
          <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Buscar huésped…"
            className="min-h-[44px] w-full appearance-none rounded-xl border border-line-strong bg-paper pl-10 pr-3 text-sm font-medium text-ink outline-none transition placeholder:text-muted focus:border-teal focus:shadow-[0_0_0_4px_var(--color-teal-soft)]"
          />
        </div>
      </div>

      {list.length === 0 ? (
        <EmptyState
          icon={CalendarDays}
          title="No hay reservas"
          text="Cuando los huéspedes reserven, aparecerán acá."
        />
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          {list.map((booking) => (
            <BookingSummaryCard
              key={booking.id}
              booking={booking}
              onConfirm={(id) => actions.setBookingStatus(id, "CONFIRMED")}
              onCancel={(id) => actions.setBookingStatus(id, "CANCELLED")}
            />
          ))}
        </div>
      )}
    </div>
  );
}
