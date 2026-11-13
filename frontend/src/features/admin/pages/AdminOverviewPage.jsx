import { useCallback, useEffect, useMemo, useState } from "react";
import { CalendarDays } from "lucide-react";
import { Alert } from "@components/ui";
import { useAdminLodgings } from "@lib/api/hooks";
import { useSession } from "@features/auth";
import { DEMO_MODE } from "@lib/config/env";
import { AdminOverviewHeader } from "../components/AdminOverviewHeader";
import { AdminStatsSection } from "../components/AdminStatsSection";
import { LodgingsPanel } from "../components/LodgingsPanel";
import { AdminHighlightCards } from "../components/AdminHighlightCards";
import { deriveAdminStats } from "../lib/adminStats";
import { filterLodgings } from "../lib/filterLodgings";

export default function AdminOverviewPage() {
  const { actions } = useSession();
  const storeLodgings = useAdminLodgings();
  const [lodgings, setLodgings] = useState(storeLodgings);
  const [stats, setStats] = useState(null);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [updatingId, setUpdatingId] = useState(null);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  const loadData = useCallback(async () => {
    if (DEMO_MODE) {
      setLodgings(storeLodgings);
      if (actions.getAdminStats) setStats(await actions.getAdminStats());
      return;
    }
    setLoading(true);
    setError("");
    try {
      // Métricas agregadas del backend (/admin/stats); si ese endpoint todavía
      // no existe, la capa de servicios las calcula con el listado de dueños.
      if (actions.getAdminStats) setStats(await actions.getAdminStats());

      if (actions.getAdminLodgings) {
        const data = await actions.getAdminLodgings();
        if (Array.isArray(data) && data.length > 0) {
          setLodgings(data);
          return;
        }
      }
    } catch (err) {
      setError(err?.message || "No se pudo sincronizar la lista de alojamientos con el backend.");
    } finally {
      setLoading(false);
    }
    setLodgings(storeLodgings);
  }, [actions, storeLodgings]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Si cambia el store de admin en demo mode, mantener sincronizado si no se cargó otra cosa
  useEffect(() => {
    if (DEMO_MODE) {
      setLodgings(storeLodgings);
    }
  }, [storeLodgings]);

  const handleToggleStatus = async (lodging, targetStatus) => {
    setUpdatingId(lodging.id);
    setNotice("");
    setError("");
    try {
      if (actions.setAdminLodgingStatus) {
        await actions.setAdminLodgingStatus(lodging.id, targetStatus);
      }
      setLodgings((current) =>
        current.map((item) =>
          item.id === lodging.id ? { ...item, status: targetStatus, enabled: targetStatus === "ACTIVE" } : item,
        ),
      );
      setNotice(`Alojamiento "${lodging.name}" ${targetStatus === "ACTIVE" ? "activado" : "pausado"} correctamente.`);
      setTimeout(() => setNotice(""), 3500);
    } catch (err) {
      setError(err?.message || "Error al actualizar el estado en el backend.");
    } finally {
      setUpdatingId(null);
    }
  };

  const filtered = useMemo(() => filterLodgings(lodgings, query), [lodgings, query]);

  const metrics = deriveAdminStats(stats, lodgings);

  return (
    <div className="grid gap-6">
      <AdminOverviewHeader loading={loading} onLoadData={loadData} lodgingsCount={lodgings.length} />

      {notice && <Alert tone="success">{notice}</Alert>}
      {error && <Alert tone="danger">{error}</Alert>}

      <AdminStatsSection metrics={metrics} />

      <LodgingsPanel
        lodgings={filtered}
        query={query}
        onQueryChange={setQuery}
        updatingId={updatingId}
        onToggleStatus={handleToggleStatus}
      />

      <AdminHighlightCards lodgingsCount={lodgings.length} />

      <p className="flex items-center gap-2 text-xs text-muted">
        <CalendarDays size={13} /> Panel de administración conectado a endpoints <code className="font-mono">/owners</code> de HostFlow API.
      </p>
    </div>
  );
}
