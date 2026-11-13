import { Building2, Users } from "lucide-react";
import { Card } from "@components/ui";

export function AdminHighlightCards({ lodgingsCount }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <Card className="flex items-center gap-4 p-5">
        <span className="grid size-11 place-items-center rounded-xl bg-lime-soft text-lime-ink"><Building2 size={20} /></span>
        <div>
          <p className="font-bold text-ink">Alta de alojamientos</p>
          <p className="text-xs text-muted">Crece {lodgingsCount > 6 ? "rápido" : "de forma constante"} esta semana</p>
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
  );
}
