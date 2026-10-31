import { useEffect, useMemo, useState } from "react";
import {
  Check,
  Coins,
  Copy,
  CreditCard,
  Eye,
  EyeOff,
  Key,
  Landmark,
  Plus,
  Settings,
  Trash2,
  Wallet,
} from "lucide-react";
import { Alert, Badge, Button, Card, Field, Switch } from "@components/ui";
import Modal from "@components/ui/Modal";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { MAX_PAYMENT_METHODS } from "@lib/state/store";
import { DEMO_MODE } from "@lib/config/env";
import { CURRENCIES, cn, copyText, formatMoney } from "@utils/format";

// Opciones predefinidas para agregar fácilmente un método
const PRESET_PROVIDERS = [
  {
    type: "WALLET",
    label: "Mercado Pago",
    defaultKeys: { alias: "hostflow.mp", cvu: "", publicKey: "", accessToken: "" },
    notes: "Transferir al Alias/CVU o pagar con dinero en cuenta de Mercado Pago.",
  },
  {
    type: "BANK",
    label: "Transferencia Bancaria",
    defaultKeys: { holder: "", cbu: "", alias: "", bank: "" },
    notes: "Enviar comprobante indicando el número de reserva.",
  },
  {
    type: "WALLET",
    label: "PayPal",
    defaultKeys: { email: "", clientId: "", paypalMe: "" },
    notes: "Aceptamos pagos internacionales en USD o EUR.",
  },
  {
    type: "CARD",
    label: "Stripe / Tarjeta",
    defaultKeys: { publishableKey: "", secretKey: "" },
    notes: "Procesamiento con tarjeta de crédito o débito.",
  },
  {
    type: "WALLET",
    label: "Ualá / Billetera digital",
    defaultKeys: { alias: "", cvu: "", holder: "" },
    notes: "Transferir al CVU o Alias y enviar el comprobante.",
  },
];

export default function PaymentMethodsPage() {
  const { actions } = useSession();
  const payments = useStore((s) => s.payments);
  const lodging = useStore((s) => s.lodging);
  const bankDetails = payments.bankDetails || {};
  const methods = Array.isArray(payments.methods) ? payments.methods : [];

  const [saved, setSaved] = useState(false);
  const [savedBank, setSavedBank] = useState(false);
  const [copiedId, setCopiedId] = useState(null);
  const [editingMethod, setEditingMethod] = useState(null);
  const [isNewMethod, setIsNewMethod] = useState(false);
  const [showSecret, setShowSecret] = useState(false);
  const [modalError, setModalError] = useState("");
  const [actionError, setActionError] = useState("");

  // Sincroniza los métodos de pago con el backend al abrir la pantalla. En
  // modo demo el store local ya es la fuente de verdad.
  useEffect(() => {
    if (DEMO_MODE) return;
    let alive = true;
    Promise.resolve()
      .then(() => actions.getPayments?.())
      .catch((err) => {
        if (alive) setActionError(err?.message || "No se pudieron cargar los métodos de pago desde el backend.");
      });
    return () => {
      alive = false;
    };
  }, [actions]);

  // Ejecuta una acción de pago y, si el backend responde un error real, lo
  // muestra en pantalla en lugar de dejar una promesa sin manejar.
  // Devuelve true si la operación terminó bien.
  const runAction = async (action) => {
    setActionError("");
    try {
      await action();
      flashSaved();
      return true;
    } catch (err) {
      setActionError(err?.message || "No se pudo guardar la configuración de pagos.");
      return false;
    }
  };

  const flashSaved = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 2200);
  };

  const setPrimary = (code) => runAction(() => actions.setPrimaryCurrency(code));

  const toggleWallet = (code) => runAction(() => actions.toggleWallet(code));

  const toggleMethod = (id) => runAction(() => actions.togglePaymentMethod(id));

  const saveBankDetails = async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const ok = await runAction(() =>
      actions.updateBankDetails({
        holder: String(form.get("holder") || "").trim(),
        alias: String(form.get("alias") || "").trim(),
        cbu: String(form.get("cbu") || "").trim(),
        notes: String(form.get("notes") || "").trim(),
      }),
    );
    // Solo se confirma si la operación terminó bien.
    if (!ok) return;
    setSavedBank(true);
    setTimeout(() => setSavedBank(false), 2200);
  };

  const copyCredentials = async (method) => {
    const keys = method.keys || {};
    const lines = [`Método: ${method.label}`];
    if (keys.holder) lines.push(`Titular: ${keys.holder}`);
    if (keys.alias) lines.push(`Alias: ${keys.alias}`);
    if (keys.cbu) lines.push(`CBU: ${keys.cbu}`);
    if (keys.cvu) lines.push(`CVU: ${keys.cvu}`);
    if (keys.bank) lines.push(`Banco: ${keys.bank}`);
    if (keys.email) lines.push(`Correo: ${keys.email}`);
    if (keys.paypalMe) lines.push(`PayPal.Me: ${keys.paypalMe}`);
    if (keys.publicKey) lines.push(`Llave pública / Client ID: ${keys.publicKey}`);
    if (method.notes) lines.push(`Indicaciones: ${method.notes}`);

    // Si el navegador bloquea el portapapeles no se marca como copiado.
    if (!(await copyText(lines.join("\n")))) return;
    setCopiedId(method.id);
    setTimeout(() => setCopiedId(null), 2500);
  };

  const openAddModal = (preset = null) => {
    if (methods.length >= MAX_PAYMENT_METHODS) return;
    setIsNewMethod(true);
    setShowSecret(false);
    setModalError("");
    if (preset) {
      setEditingMethod({
        id: "",
        label: preset.label,
        type: preset.type,
        enabled: true,
        keys: { ...preset.defaultKeys },
        notes: preset.notes,
      });
    } else {
      setEditingMethod({
        id: "",
        label: "Nuevo método de pago",
        type: "WALLET",
        enabled: true,
        keys: { alias: "", cvu: "", publicKey: "", accessToken: "" },
        notes: "",
      });
    }
  };

  const openEditModal = (method) => {
    setIsNewMethod(false);
    setShowSecret(false);
    setModalError("");
    setEditingMethod({
      ...method,
      keys: { ...(method.keys || {}) },
      notes: method.notes || "",
    });
  };

  const saveMethodModal = async (event) => {
    event.preventDefault();
    if (!editingMethod) return;

    const form = new FormData(event.currentTarget);
    const label = String(form.get("label") || "").trim() || editingMethod.label;
    const type = String(form.get("type") || editingMethod.type);
    const notes = String(form.get("notes") || "").trim();

    const keys = {
      holder: String(form.get("holder") || "").trim(),
      alias: String(form.get("alias") || "").trim(),
      cbu: String(form.get("cbu") || "").trim(),
      cvu: String(form.get("cvu") || "").trim(),
      bank: String(form.get("bank") || "").trim(),
      email: String(form.get("email") || "").trim(),
      paypalMe: String(form.get("paypalMe") || "").trim(),
      publicKey: String(form.get("publicKey") || "").trim(),
      accessToken: String(form.get("accessToken") || "").trim(),
    };

    // Quitar claves vacías
    Object.keys(keys).forEach((k) => {
      if (!keys[k]) delete keys[k];
    });

    // Límite estricto: se vuelve a chequear acá porque el modal pudo quedar
    // abierto mientras otro usuario agregaba un método.
    if (isNewMethod && methods.length >= MAX_PAYMENT_METHODS) {
      setModalError(`Ya tenés los ${MAX_PAYMENT_METHODS} métodos permitidos. Eliminá uno para agregar otro.`);
      return;
    }

    try {
      if (isNewMethod) {
        await actions.addPaymentMethod({ label, type, keys, notes });
      } else {
        await actions.updatePaymentMethod(editingMethod.id, { label, type, keys, notes });
      }
    } catch (err) {
      setModalError(err?.message || "No se pudo guardar el método de pago. Intentá nuevamente.");
      return;
    }

    setModalError("");
    setEditingMethod(null);
    flashSaved();
  };

  const removeMethod = (id) => runAction(() => actions.removePaymentMethod(id));

  const canAdd = methods.length < MAX_PAYMENT_METHODS;

  return (
    <div className="grid gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[clamp(24px,4vw,32px)] font-extrabold">Métodos de pago</h1>
          <p className="mt-1 text-sm text-muted">
            Configurá hasta {MAX_PAYMENT_METHODS} métodos con sus llaves y credenciales para cobrar a tus huéspedes.
          </p>
        </div>
        <div className="flex items-center gap-2">
          {saved && <Alert tone="success">Configuración guardada.</Alert>}
          <Badge tone={canAdd ? "lime" : "neutral"}>
            {methods.length} / {MAX_PAYMENT_METHODS} métodos configurados
          </Badge>
        </div>
      </header>

      {actionError && <Alert tone="danger">{actionError}</Alert>}

      {/* Listado de Métodos de Pago y Configuración de Llaves */}
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

      {/* Moneda principal y secundarias */}
      <div className="grid gap-6 lg:grid-cols-2">
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
      </div>

      {/* Datos para transferencia estándar */}
      <Card className="p-6">
        <div className="sticky top-[65px] z-20 -mx-6 -mt-6 mb-5 flex flex-wrap items-center justify-between gap-3 border-b border-line bg-paper/95 px-6 pb-3 pt-2 backdrop-blur">
          <div className="flex items-center gap-2">
            <span className="grid size-10 place-items-center rounded-xl bg-teal-soft text-teal-strong">
              <Landmark size={20} />
            </span>
            <div>
              <h2 className="font-bold text-ink">Datos para transferencia bancaria general</h2>
              <p className="text-xs text-muted">
                La plataforma no procesa cobros directos: el huésped transfiere a estos datos y vos marcás la reserva como pagada.
              </p>
            </div>
          </div>
          {savedBank && <Alert tone="success">Datos bancarios actualizados.</Alert>}
        </div>

        <form onSubmit={saveBankDetails} className="grid gap-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Titular de la cuenta" name="holder" defaultValue={bankDetails.holder} placeholder="Ej. Cabañas del Arroyo" />
            <Field label="Alias" name="alias" defaultValue={bankDetails.alias} placeholder="Ej. cabañas.arroyo" />
          </div>
          <Field label="CBU / CVU" name="cbu" defaultValue={bankDetails.cbu} placeholder="0000000000000000000000" />
          <Field
            as="textarea"
            label="Indicaciones para el huésped (opcional)"
            name="notes"
            defaultValue={bankDetails.notes}
            placeholder="Ej. Envíanos el comprobante por WhatsApp al +54 9 11 5555 0101."
          />
          <Button type="submit" className="mt-1 w-full sm:w-auto sm:justify-self-end">
            Guardar datos bancarios
          </Button>
        </form>
      </Card>

      {/* Modal de Configuración de Llaves y Credenciales */}
      {editingMethod && (
        <Modal
          open={Boolean(editingMethod)}
          onClose={() => setEditingMethod(null)}
          title={isNewMethod ? "Nuevo método de pago" : `Configurar llaves · ${editingMethod.label}`}
          subtitle="Ajustá los identificadores, credenciales y llaves que se mostrarán al huésped."
        >
          <form onSubmit={saveMethodModal} className="grid gap-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <Field
                label="Nombre del método"
                name="label"
                defaultValue={editingMethod.label}
                placeholder="Ej. Mercado Pago, PayPal..."
                required
              />
              <Field as="select" label="Tipo" name="type" defaultValue={editingMethod.type}>
                <option value="WALLET">Billetera Digital</option>
                <option value="BANK">Transferencia Bancaria</option>
                <option value="CARD">Tarjeta de crédito / débito</option>
              </Field>
            </div>

            <div className="rounded-xl border border-line bg-cloud/50 p-4">
              <h3 className="mb-3 text-xs font-bold uppercase tracking-wider text-muted">
                Credenciales y llaves de integración
              </h3>

              <div className="grid gap-3 sm:grid-cols-2">
                <Field
                  label="Titular / Razón social"
                  name="holder"
                  defaultValue={editingMethod.keys?.holder || ""}
                  placeholder="Ej. Martina Silva"
                />
                <Field
                  label="Alias"
                  name="alias"
                  defaultValue={editingMethod.keys?.alias || ""}
                  placeholder="Ej. mi.alojamiento.mp"
                />
              </div>

              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                <Field
                  label="CBU o CVU"
                  name="cbu"
                  defaultValue={editingMethod.keys?.cbu || editingMethod.keys?.cvu || ""}
                  placeholder="0000003100012345678901"
                />
                <Field
                  label="Banco o Entidad"
                  name="bank"
                  defaultValue={editingMethod.keys?.bank || ""}
                  placeholder="Ej. Banco Santander / Mercado Pago"
                />
              </div>

              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                <Field
                  label="Correo / Usuario asociado"
                  name="email"
                  defaultValue={editingMethod.keys?.email || ""}
                  placeholder="Ej. pagos@mihotel.com"
                />
                <Field
                  label="Enlace de cobro / PayPal.Me"
                  name="paypalMe"
                  defaultValue={editingMethod.keys?.paypalMe || ""}
                  placeholder="Ej. paypal.me/mihotel"
                />
              </div>

              <div className="mt-3">
                <Field
                  label="Llave pública / Client ID"
                  name="publicKey"
                  defaultValue={editingMethod.keys?.publicKey || editingMethod.keys?.clientId || ""}
                  placeholder="Ej. APP_USR-839210-9182 o pk_live_..."
                />
              </div>

              <div className="relative mt-3">
                <Field
                  label="Llave secreta / Access Token (privada)"
                  name="accessToken"
                  type={showSecret ? "text" : "password"}
                  defaultValue={editingMethod.keys?.accessToken || editingMethod.keys?.secretKey || ""}
                  placeholder="TEST-48291048291..."
                />
                <button
                  type="button"
                  onClick={() => setShowSecret(!showSecret)}
                  className="absolute right-3.5 top-[38px] text-muted hover:text-ink cursor-pointer"
                  title={showSecret ? "Ocultar" : "Mostrar"}
                >
                  {showSecret ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <Field
              as="textarea"
              label="Indicaciones o instrucciones para el huésped"
              name="notes"
              defaultValue={editingMethod.notes || ""}
              placeholder="Ej. Realizar el pago y enviar el comprobante a nuestro WhatsApp para confirmar."
            />

            {modalError && <Alert>{modalError}</Alert>}

            <div className="mt-2 flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={() => setEditingMethod(null)}>
                Cancelar
              </Button>
              <Button type="submit">Guardar llaves</Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}