import { Coins } from "lucide-react";
import { Card } from "@components/ui";
import { CURRENCIES, cn, formatMoney } from "@utils/format";

export function PrimaryCurrencyCard({ payments, setPrimary, lodging }) {
  return (
    <Card className="p-6">
      <div className="mb-4 flex items-center gap-2">
        <span className="grid size-10 place-items-center rounded-xl bg-lime-soft text-lime-ink">
          <Coins size={20} />
        </span>
        <div>
          <h2 className="font-bold text-ink">Moneda principal</h2>
          <p className="text-xs text-muted">Es la que usan tus huéspedes por defecto.</p>
        </div>
      </div>

      <div className="grid gap-2">
        {Object.values(CURRENCIES).map((currency) => {
          const active = payments.primary === currency.code;
          return (
            <button
              key={currency.code}
              type="button"
              onClick={() => setPrimary(currency.code)}
              className={cn(
                "flex items-center justify-between rounded-xl border px-4 py-3.5 text-left transition-all",
                active
                  ? "border-lime bg-lime-soft dark:bg-lime/12"
                  : "border-line bg-paper hover:border-line-strong",
              )}
            >
              <span className="flex items-center gap-3">
                <span
                  className={cn(
                    "grid size-9 place-items-center rounded-lg text-sm font-extrabold",
                    active ? "bg-lime text-lime-ink" : "bg-cloud text-muted",
                  )}
                >
                  {currency.symbol}
                </span>
                <span>
                  <span className="block text-sm font-bold text-ink">{currency.label}</span>
                  <span className="block text-xs text-muted">{currency.code}</span>
                </span>
              </span>
              <span
                className={cn(
                  "grid size-5 place-items-center rounded-full border-2",
                  active
                    ? "border-lime-ink bg-lime-ink text-lime"
                    : "border-line-strong text-transparent",
                )}
              >
                ✓
              </span>
            </button>
          );
        })}
      </div>

      <p className="mt-4 text-xs text-muted">
        Precio actual:{" "}
        <span className="font-bold text-ink">{formatMoney(lodging.pricePerNight, payments.primary)}</span> por
        noche.
      </p>
    </Card>
  );
}
