import { Building2, Search } from "lucide-react";
import { Card, EmptyState } from "@components/ui";
import { LodgingRow } from "./LodgingRow";

export function LodgingsPanel({ lodgings, query, onQueryChange, updatingId, onToggleStatus }) {
  return (
    <Card className="p-6">
      <div className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="font-bold text-ink">Listado de alojamientos</h2>
          <p className="text-xs text-muted">
            Consultando endpoints reales <code className="font-mono text-teal-strong">GET /owners</code>,{" "}
            <code className="font-mono text-teal-strong">PATCH /owners/:id/status</code> y{" "}
            <code className="font-mono text-teal-strong">GET /admin/stats</code>.
          </p>
        </div>
        <div className="relative sm:w-80">
          <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted" />
          <input
            value={query}
            onChange={(e) => onQueryChange(e.target.value)}
            placeholder="Dueño, ID o correo…"
            className="min-h-[46px] w-full appearance-none rounded-xl border border-line-strong bg-paper pl-10 pr-3 text-sm font-medium text-ink outline-none transition placeholder:text-muted focus:border-teal focus:shadow-[0_0_0_4px_var(--color-teal-soft)]"
          />
        </div>
      </div>

      {lodgings.length === 0 ? (
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
              {lodgings.map((lodging) => (
                <LodgingRow
                  key={lodging.id}
                  lodging={lodging}
                  updatingId={updatingId}
                  onToggleStatus={onToggleStatus}
                />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}
