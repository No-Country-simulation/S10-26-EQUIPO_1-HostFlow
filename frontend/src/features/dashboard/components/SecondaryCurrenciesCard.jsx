import { Wallet } from "lucide-react";
import { Card, Switch } from "@components/ui";
import { CURRENCIES } from "@utils/format";

export function SecondaryCurrenciesCard({ payments, toggleWallet }) {
  return (
    <Card className="p-6">
      <div className="mb-4 flex items-center gap-2">
        <span className="grid size-10 place-items-center rounded-xl bg-teal-soft text-teal-strong">
          <Wallet size={20} />
        </span>
        <div>
          <h2 className="font-bold text-ink">Monedas secundarias</h2>
          <p className="text-xs text-muted">Ofrecé alternativas a tus huéspedes.</p>
        </div>
      </div>

      <div className="grid gap-3">
        {["USD", "EUR"].map((code) => {
          const currency = CURRENCIES[code];
          const active = payments.wallets.includes(code);
          return (
            <div
              key={code}
              className="flex min-w-0 items-center justify-between gap-2 rounded-xl border border-line px-4 py-3.5"
            >
              <span className="flex min-w-0 items-center gap-3">
                <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-cloud text-sm font-extrabold text-muted">
                  {currency.symbol}
                </span>
                <span className="min-w-0">
                  <span className="block truncate text-sm font-bold text-ink">{currency.label}</span>
                  <span className="block text-xs text-muted">{currency.code}</span>
                </span>
              </span>
              <Switch checked={active} onChange={() => toggleWallet(code)} label={active ? "Activa" : "Inactiva"} />
            </div>
          );
        })}
      </div>
    </Card>
  );
}
