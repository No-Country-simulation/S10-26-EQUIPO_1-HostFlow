import { Building2, CalendarDays, CheckCircle2, Users } from "lucide-react";
import { Stat } from "@components/ui";

export function AdminStatsSection({ metrics }) {
  const { lodgingsCount, active, paused, totalBookings, uniqueHosts } = metrics;
  return (
    <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <Stat label="Alojamientos registrados" value={lodgingsCount} tone="lime" icon={Building2} />
      <Stat label="Activos" value={active} hint={`${paused} en pausa`} tone="teal" icon={CheckCircle2} />
      <Stat
        label="Reservas procesadas"
        value={totalBookings ?? "—"}
        hint={totalBookings == null ? "Sin dato en el backend" : undefined}
        tone="pink"
        icon={CalendarDays}
      />
      <Stat label="Anfitriones únicos" value={uniqueHosts} tone="lime" icon={Users} />
    </section>
  );
}
