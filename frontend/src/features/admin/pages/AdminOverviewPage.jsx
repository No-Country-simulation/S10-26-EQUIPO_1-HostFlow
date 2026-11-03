import { useMemo, useState } from "react";
import { Building2, CalendarDays, CheckCircle2, Search, ShieldCheck, Users } from "lucide-react";
import { Badge, Card, EmptyState, Stat } from "@components/ui";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";

export default function AdminOverviewPage() {
  const { actions } = useSession();
  const lodgings = useStore((s) => s.admin.lodgings);
  const [query, setQuery] = useState("");

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return lodgings;
    return lodgings.filter((l) =>
      `${l.owner} ${l.ownerEmail} ${l.id} ${l.name}`.toLowerCase().includes(q),
    );
  }, [lodgings, query]);

  const active = lodgings.filter((l) => l.status === "ACTIVE").length;
  const paused = lodgings.filter((l) => l.status === "PAUSED").length;
  const totalBookings = lodgings.length * 42; // métrica ilustrativa del backend

  return (
    <div className="grid gap-6">
      <header
        className="relative overflow-hidden rounded-[26px] p-6 text-white sm:p-8"
        style={{ background: "linear-gradient(135deg, #111111 0%, #1f2d2b 55%, #0b6b6b 130%)" }}
      >
        <div className="absolute inset-0 dot-grid opacity-15 [mask-image:radial-gradient(ellipse_at_right,black,transparent_70%)]" />
        <div className="relative flex flex-wrap items-center justify-between gap-4">
          <div>
            <Badge tone="lime" className="mb-3 border-0">
              <ShieldCheck size={12} /> Control interno · HostFlow
            </Badge>
            <h1 className="text-[clamp(24px,4vw,34px)] font-extrabold">Panel de administración</h1>
            <p className="mt-1.5 text-sm text-white/70">Solo visible para administradores.</p>
          </div>
          <span className="rounded-xl bg-white/10 px-4 py-2.5 text-sm font-bold text-white/90">
            {lodgings.length} alojamientos
          </span>
        </div>
      </header>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Stat label="Alojamientos registrados" value={lodgings.length} tone="lime" icon={Building2} />
        <Stat label="Activos" value={active} hint={`${paused} en pausa`} tone="teal" icon={CheckCircle2} />
        <Stat label="Reservas procesadas" value={totalBookings} tone="pink" icon={CalendarDays} />
        <Stat label="Anfitriones únicos" value={new Set(lodgings.map((l) => l.ownerEmail)).size} tone="lime" icon={Users} />
      </section>

      <Card className="p-6">
        <div className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="font-bold text-ink">Listado de alojamientos</h2>
            <p className="text-xs text-muted">Buscá por dueño, número ID o correo.</p>
          </div>
          <div className="relative sm:w-80">
            <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Dueño, ID o correo…"
              className="min-h-[46px] w-full appearance-none rounded-xl border border-line-strong bg-paper pl-10 pr-3 text-sm font-medium text-ink outline-none transition placeholder:text-muted focus:border-teal focus:shadow-[0_0_0_4px_var(--color-teal-soft)]"
            />
          </div>
        </div>

        {filtered.length === 0 ? (
          <EmptyState icon={Building2} title="Sin resultados" text={`Ningún alojamiento coincide con "${query}".`} />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] text-left text-sm">
              <thead>
                <tr className="border-b border-line text-xs uppercase tracking-wide text-muted">
                  <th className="pb-3 pr-4 font-bold">Alojamiento</th>
                  <th className="pb-3 pr-4 font-bold">Dueño</th>
                  <th className="pb-3 pr-4 font-bold">ID</th>
                  <th className="pb-3 pr-4 font-bold">Correo</th>
                  <th className="pb-3 pr-4 font-bold">Hab.</th>
                  <th className="pb-3 pr-4 font-bold">Estado</th>
                  <th className="pb-3 font-bold">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((lodging) => (
                  <tr key={lodging.id} className="border-b border-line last:border-0">
                    <td className="py-3.5 pr-4 font-bold text-ink">{lodging.name}</td>
                    <td className="py-3.5 pr-4 text-ink-soft">{lodging.owner}</td>
                    <td className="py-3.5 pr-4 font-mono text-xs text-muted">{lodging.id}</td>
                    <td className="py-3.5 pr-4 text-teal-strong">{lodging.ownerEmail}</td>
                    <td className="py-3.5 pr-4 text-ink-soft">{lodging.rooms}</td>
                    <td className="py-3.5 pr-4">
                      <Badge tone={lodging.status === "ACTIVE" ? "success" : "neutral"}>
                        {lodging.status === "ACTIVE" ? "Activo" : "En pausa"}
                      </Badge>
                    </td>
                    <td className="py-3.5">
                      <div className="flex gap-1.5">
                        {lodging.status !== "ACTIVE" && (
                          <button
                            onClick={() => actions.setAdminLodgingStatus(lodging.id, "ACTIVE")}
                            className="cursor-pointer rounded-lg border border-line-strong px-2.5 py-1.5 text-xs font-bold text-ink-soft transition hover:border-success hover:text-success"
                          >
                            Activar
                          </button>
                        )}
                        {lodging.status === "ACTIVE" && (
                          <button
                            onClick={() => actions.setAdminLodgingStatus(lodging.id, "PAUSED")}
                            className="cursor-pointer rounded-lg border border-line-strong px-2.5 py-1.5 text-xs font-bold text-ink-soft transition hover:border-pink-strong hover:text-pink-strong"
                          >
                            Pausar
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <div className="grid gap-4 sm:grid-cols-2">
        <Card className="flex items-center gap-4 p-5">
          <span className="grid size-11 place-items-center rounded-xl bg-lime-soft text-lime-ink"><Building2 size={20} /></span>
          <div>
            <p className="font-bold text-ink">Alta de alojamientos</p>
            <p className="text-xs text-muted">Crece {lodgings.length > 6 ? "rápido" : "de forma constante"} esta semana</p>
          </div>
        </Card>
        <Card className="flex items-center gap-4 p-5">
          <span className="grid size-11 place-items-center rounded-xl bg-teal-soft text-teal-strong"><Users size={20} /></span>
          <div>
            <p className="font-bold text-ink">Anfitriones activos</p>
            <p className="text-xs text-muted">Con al menos una reserva</p>
          </div>
        </Card>
      </div>

      <p className="flex items-center gap-2 text-xs text-muted">
        <CalendarDays size={13} /> Datos de demostración · el backend real expone estas métricas en <code className="font-mono">/api/admin/stats</code>.
      </p>
    </div>
  );
}
