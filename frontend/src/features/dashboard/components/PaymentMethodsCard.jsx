import { Check, Copy, CreditCard, Key, Landmark, Plus, Trash2 } from "lucide-react";
import { Button, Card, Switch } from "@components/ui";
import { PRESET_PROVIDERS } from "../lib/presetProviders";
import { MAX_PAYMENT_METHODS } from "@lib/domain/payments";
import { cn } from "@utils/format";

export function PaymentMethodsCard({
  methods,
  canAdd,
  openAddModal,
  copiedId,
  toggleMethod,
  openEditModal,
  copyCredentials,
  removeMethod,
}) {
  return (
    <Card className="p-6">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <span className="grid size-10 place-items-center rounded-xl bg-pink-soft text-pink-strong">
            <CreditCard size={20} />
          </span>
          <div>
            <h2 className="font-bold text-ink">Métodos de pago habilitados</h2>
            <p className="text-xs text-muted">
              Limitado a {MAX_PAYMENT_METHODS} métodos. Configurá sus credenciales y compartilas en el onboarding del huésped.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant={canAdd ? "primary" : "secondary"}
            disabled={!canAdd}
            icon={Plus}
            onClick={() => openAddModal()}
          >
            {canAdd ? "Agregar método" : "Máximo alcanzado"}
          </Button>
        </div>
      </div>

      {/* Métodos configurados */}
      <div className="grid gap-4 md:grid-cols-2">
        {methods.map((method) => {
          const keys = method.keys || {};
          const isCopied = copiedId === method.id;
          return (
            <div
              key={method.id}
              className={cn(
                "relative flex flex-col justify-between rounded-2xl border p-4 transition-all",
                method.enabled
                  ? "border-teal/50 bg-teal-soft/10 dark:bg-teal-soft/5 shadow-sm"
                  : "border-line bg-paper opacity-80",
              )}
            >
              <div>
                <div className="flex items-center justify-between gap-2 border-b border-line pb-3">
                  <div className="flex items-center gap-2.5">
                    <span
                      className={cn(
                        "grid size-9 shrink-0 place-items-center rounded-xl text-sm font-extrabold",
                        method.enabled ? "bg-teal text-white" : "bg-cloud text-muted",
                      )}
                    >
                      {method.type === "BANK" ? <Landmark size={18} /> : <CreditCard size={18} />}
                    </span>
                    <div>
                      <span className="block text-sm font-bold text-ink">{method.label}</span>
                      <span className="block text-[11px] font-semibold text-muted">
                        {method.type === "BANK"
                          ? "Transferencia bancaria"
                          : method.type === "CARD"
                            ? "Tarjeta de crédito/débito"
                            : "Billetera digital"}
                      </span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Switch
                      checked={method.enabled}
                      onChange={() => toggleMethod(method.id)}
                      label={method.enabled ? "Activo" : "Pausado"}
                    />
                  </div>
                </div>

                {/* Resumen de llaves / credenciales */}
                <div className="mt-3 grid gap-1.5 rounded-xl bg-cloud/80 p-3 text-xs">
                  {keys.holder && (
                    <p>
                      <span className="font-semibold text-ink">Titular:</span> {keys.holder}
                    </p>
                  )}
                  {keys.alias && (
                    <p>
                      <span className="font-semibold text-ink">Alias:</span>{" "}
                      <code className="font-mono text-teal-strong">{keys.alias}</code>
                    </p>
                  )}
                  {keys.cbu && (
                    <p>
                      <span className="font-semibold text-ink">CBU:</span>{" "}
                      <code className="font-mono text-teal-strong">{keys.cbu}</code>
                    </p>
                  )}
                  {keys.cvu && (
                    <p>
                      <span className="font-semibold text-ink">CVU:</span>{" "}
                      <code className="font-mono text-teal-strong">{keys.cvu}</code>
                    </p>
                  )}
                  {keys.email && (
                    <p>
                      <span className="font-semibold text-ink">Correo:</span> {keys.email}
                    </p>
                  )}
                  {keys.paypalMe && (
                    <p>
                      <span className="font-semibold text-ink">PayPal.Me:</span> {keys.paypalMe}
                    </p>
                  )}
                  {keys.publicKey && (
                    <p>
                      <span className="font-semibold text-ink">Llave pública:</span>{" "}
                      <code className="font-mono text-muted">
                        {keys.publicKey.length > 18 ? `${keys.publicKey.slice(0, 14)}…` : keys.publicKey}
                      </code>
                    </p>
                  )}
                  {keys.accessToken && (
                    <p>
                      <span className="font-semibold text-ink">Access Token:</span>{" "}
                      <span className="font-mono text-muted">••••••••••••••••</span>
                    </p>
                  )}
                  {method.notes && (
                    <p className="mt-1 border-t border-line/60 pt-1 text-[11px] text-muted italic">
                      {method.notes}
                    </p>
                  )}
                  {Object.keys(keys).length === 0 && !method.notes && (
                    <p className="text-muted italic">Sin credenciales cargadas. Tocá en configurar llaves.</p>
                  )}
                </div>
              </div>

              {/* Acciones del método */}
              <div className="mt-4 flex flex-wrap items-center justify-between gap-2 border-t border-line pt-3">
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => openEditModal(method)}
                    className="inline-flex cursor-pointer items-center gap-1.5 rounded-lg border border-line-strong px-2.5 py-1.5 text-xs font-bold text-ink transition hover:border-teal hover:text-teal-strong"
                  >
                    <Key size={13} /> Configurar llaves
                  </button>
                  <button
                    type="button"
                    onClick={() => copyCredentials(method)}
                    className="inline-flex cursor-pointer items-center gap-1.5 rounded-lg border border-line-strong px-2.5 py-1.5 text-xs font-bold text-ink-soft transition hover:border-line hover:text-ink"
                  >
                    {isCopied ? (
                      <>
                        <Check size={13} className="text-success" /> Copiado
                      </>
                    ) : (
                      <>
                        <Copy size={13} /> Copiar credenciales
                      </>
                    )}
                  </button>
                </div>

                {methods.length > 1 && (
                  <button
                    type="button"
                    title="Eliminar método"
                    onClick={() => removeMethod(method.id)}
                    className="cursor-pointer rounded-lg p-1.5 text-muted transition hover:bg-danger-soft hover:text-danger"
                  >
                    <Trash2 size={15} />
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Sugerencias para agregar si no se alcanzó el máximo */}
      {canAdd && (
        <div className="mt-6 rounded-2xl bg-cloud p-4">
          <p className="text-xs font-bold text-ink">Agregar plantilla rápida:</p>
          <div className="mt-2.5 flex flex-wrap gap-2">
            {PRESET_PROVIDERS.filter((p) => !methods.some((m) => m.label.toLowerCase() === p.label.toLowerCase())).map(
              (preset) => (
                <button
                  key={preset.label}
                  type="button"
                  onClick={() => openAddModal(preset)}
                  className="inline-flex cursor-pointer items-center gap-1.5 rounded-xl border border-line-strong bg-paper px-3 py-1.5 text-xs font-bold text-ink transition hover:border-teal hover:text-teal-strong"
                >
                  <Plus size={13} /> {preset.label}
                </button>
              ),
            )}
          </div>
        </div>
      )}
    </Card>
  );
}
