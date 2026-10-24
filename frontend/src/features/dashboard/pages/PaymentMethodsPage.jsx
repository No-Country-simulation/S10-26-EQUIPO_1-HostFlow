import { useState } from "react";
import { Coins, CreditCard, Plus, Wallet } from "lucide-react";
import { Alert, Badge, Button, Card, Switch } from "@components/ui";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { CURRENCIES, WALLETS, cn, formatMoney } from "@utils/format";

export default function PaymentMethodsPage() {
  const { actions } = useSession();
  const payments = useStore((s) => s.payments);
  const lodging = useStore((s) => s.lodging);
  const [newWallet, setNewWallet] = useState("");
  const [saved, setSaved] = useState(false);

  const flashSaved = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 2200);
  };

  const setPrimary = (code) => {
    actions.setPrimaryCurrency(code);
    flashSaved();
  };

  const toggleWallet = (code) => {
    actions.toggleWallet(code);
    flashSaved();
  };

  const addWallet = (event) => {
    event.preventDefault();
    const label = newWallet.trim();
    if (!label) return;
    actions.addPaymentMethod(label, "WALLET");
    setNewWallet("");
    flashSaved();
  };

  return (
    <div className="grid gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[clamp(24px,4vw,32px)] font-extrabold">Métodos de pago</h1>
          <p className="mt-1 text-sm text-muted">Definí la moneda principal y las billeteras digitales que aceptás.</p>
        </div>
        {saved && <Alert tone="success">Configuración guardada.</Alert>}
      </header>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Moneda principal */}
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
                      ? "border-lime bg-lime-soft"
                      : "border-line bg-paper hover:border-line-strong",
                  )}
                >
                  <span className="flex items-center gap-3">
                    <span className={cn("grid size-9 place-items-center rounded-lg text-sm font-extrabold", active ? "bg-lime text-lime-ink" : "bg-cloud text-muted")}>
                      {currency.symbol}
                    </span>
                    <span>
                      <span className="block text-sm font-bold text-ink">{currency.label}</span>
                      <span className="block text-xs text-muted">{currency.code}</span>
                    </span>
                  </span>
                  <span className={cn("grid size-5 place-items-center rounded-full border-2", active ? "border-lime-ink bg-lime-ink text-lime" : "border-line-strong text-transparent")}>
                    ✓
                  </span>
                </button>
              );
            })}
          </div>

          <p className="mt-4 text-xs text-muted">
            Precio actual: <span className="font-bold text-ink">{formatMoney(lodging.pricePerNight, payments.primary)}</span> por noche.
          </p>
        </Card>

        {/* Monedas secundarias */}
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
                <div key={code} className="flex items-center justify-between rounded-xl border border-line px-4 py-3.5">
                  <span className="flex items-center gap-3">
                    <span className="grid size-9 place-items-center rounded-lg bg-cloud text-sm font-extrabold text-muted">{currency.symbol}</span>
                    <span>
                      <span className="block text-sm font-bold text-ink">{currency.label}</span>
                      <span className="block text-xs text-muted">{currency.code}</span>
                    </span>
                  </span>
                  <Switch checked={active} onChange={() => toggleWallet(code)} label={active ? "Activa" : "Inactiva"} />
                </div>
              );
            })}
          </div>
        </Card>
      </div>

      {/* Billeteras digitales */}
      <Card className="p-6">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <span className="grid size-10 place-items-center rounded-xl bg-pink-soft text-pink-strong">
              <CreditCard size={20} />
            </span>
            <div>
              <h2 className="font-bold text-ink">Billeteras digitales</h2>
              <p className="text-xs text-muted">Habilitá las plataformas que aceptás para cobrar.</p>
            </div>
          </div>
          <Badge tone="teal">{payments.methods.filter((m) => m.enabled).length} habilitadas</Badge>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          {payments.methods.map((method) => (
            <div
              key={method.id}
              className={cn(
                "flex items-center justify-between rounded-xl border px-4 py-3.5 transition-all",
                method.enabled ? "border-lime/70 bg-lime-soft/40" : "border-line bg-paper",
              )}
            >
              <div className="flex items-center gap-3">
                <span className={cn("grid size-9 place-items-center rounded-lg", method.enabled ? "bg-lime text-lime-ink" : "bg-cloud text-muted")}>
                  <CreditCard size={17} />
                </span>
                <span className="text-sm font-bold text-ink">{method.label}</span>
              </div>
              <Switch checked={method.enabled} onChange={() => { actions.togglePaymentMethod(method.id); flashSaved(); }} label={method.enabled ? "On" : "Off"} />
            </div>
          ))}
        </div>

        <form onSubmit={addWallet} className="mt-5 flex flex-col gap-2 sm:flex-row">
          <input
            value={newWallet}
            onChange={(e) => setNewWallet(e.target.value)}
            placeholder="Agregar otra billetera (ej. Binance Pay)"
            className="min-h-[46px] flex-1 appearance-none rounded-xl border border-line-strong bg-paper px-4 text-sm font-medium text-ink outline-none transition placeholder:text-muted focus:border-teal focus:shadow-[0_0_0_4px_var(--color-teal-soft)]"
          />
          <Button type="submit" variant="secondary" icon={Plus} className="shrink-0">
            Agregar
          </Button>
        </form>

        <p className="mt-4 text-xs text-muted">Sugeridas: {WALLETS.join(" · ")}</p>
      </Card>
    </div>
  );
}
