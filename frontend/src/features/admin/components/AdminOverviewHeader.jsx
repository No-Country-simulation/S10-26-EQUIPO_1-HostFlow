import { RefreshCw, ShieldCheck } from "lucide-react";
import { Badge, Button } from "@components/ui";
import { DEMO_MODE } from "@lib/config/env";

export function AdminOverviewHeader({ loading, onLoadData, lodgingsCount }) {
  return (
    <header
      className="relative overflow-hidden rounded-[26px] p-6 text-white sm:p-8"
      style={{ background: "linear-gradient(135deg, #101518 0%, #1b2724 55%, #0b6b6b 130%)" }}
    >
      <div className="absolute inset-0 dot-grid opacity-15 [mask-image:radial-gradient(ellipse_at_right,black,transparent_70%)]" />
      <div className="relative flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <Badge tone="lime" className="border-0">
              <ShieldCheck size={12} /> Control interno · HostFlow
            </Badge>
            <Badge tone={DEMO_MODE ? "neutral" : "teal"} className="border-0 bg-white/20 text-white">
              {DEMO_MODE ? "Modo Demo" : "Backend conectado · /owners · /admin/stats"}
            </Badge>
          </div>
          <h1 className="text-[clamp(24px,4vw,34px)] font-extrabold">Panel de administración</h1>
          <p className="mt-1.5 text-sm text-white/70">
            Gestión centralizada de dueños y alojamientos registrados en HostFlow.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="sm"
            icon={RefreshCw}
            onClick={onLoadData}
            disabled={loading}
            className="bg-white/10 text-white hover:bg-white/20 border-white/20"
          >
            {loading ? "Sincronizando…" : "Actualizar"}
          </Button>
          <span className="rounded-xl bg-white/10 px-4 py-2.5 text-sm font-bold text-white/90">
            {lodgingsCount} alojamientos
          </span>
        </div>
      </div>
    </header>
  );
}
