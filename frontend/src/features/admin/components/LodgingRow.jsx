import { Badge } from "@components/ui";

export function LodgingRow({ lodging, updatingId, onToggleStatus }) {
  const isActive = lodging.status === "ACTIVE" || lodging.enabled === true;
  const isBusy = updatingId === lodging.id;
  return (
    <tr className="border-b border-line last:border-0 hover:bg-overlay/50 transition">
      <td className="py-3.5 pr-4 font-bold text-ink">{lodging.name}</td>
      <td className="py-3.5 pr-4 text-ink-soft">{lodging.owner}</td>
      <td className="py-3.5 pr-4 font-mono text-xs text-muted">{lodging.id}</td>
      <td className="py-3.5 pr-4 text-teal-strong">{lodging.ownerEmail}</td>
      <td className="py-3.5 pr-4 text-ink-soft">{lodging.rooms ?? "—"}</td>
      <td className="py-3.5 pr-4">
        <Badge tone={isActive ? "success" : "neutral"}>
          {isActive ? "Activo" : "En pausa"}
        </Badge>
      </td>
      <td className="py-3.5">
        <div className="flex gap-1.5">
          {!isActive ? (
            <button
              disabled={isBusy}
              onClick={() => onToggleStatus(lodging, "ACTIVE")}
              className="cursor-pointer rounded-lg border border-line-strong px-2.5 py-1.5 text-xs font-bold text-ink-soft transition hover:border-success hover:text-success disabled:opacity-50"
            >
              {isBusy ? "Activando…" : "Activar"}
            </button>
          ) : (
            <button
              disabled={isBusy}
              onClick={() => onToggleStatus(lodging, "PAUSED")}
              className="cursor-pointer rounded-lg border border-line-strong px-2.5 py-1.5 text-xs font-bold text-ink-soft transition hover:border-pink-strong hover:text-pink-strong disabled:opacity-50"
            >
              {isBusy ? "Pausando…" : "Pausar"}
            </button>
          )}
        </div>
      </td>
    </tr>
  );
}
