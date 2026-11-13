import { useEffect, useState } from "react";
import { Alert, Badge } from "@components/ui";
import { PaymentMethodsCard } from "../components/PaymentMethodsCard";
import { PrimaryCurrencyCard } from "../components/PrimaryCurrencyCard";
import { SecondaryCurrenciesCard } from "../components/SecondaryCurrenciesCard";
import { BankDetailsCard } from "../components/BankDetailsCard";
import { PaymentMethodModal } from "../components/PaymentMethodModal";
import { useLodging, usePayments } from "@lib/api/hooks";
import { useSession } from "@features/auth";
import { MAX_PAYMENT_METHODS } from "@lib/domain/payments";
import { DEMO_MODE } from "@lib/config/env";
import { copyText } from "@utils/format";

export default function PaymentMethodsPage() {
  const { actions } = useSession();
  const payments = usePayments();
  const lodging = useLodging();
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
      <PaymentMethodsCard
        methods={methods}
        canAdd={canAdd}
        openAddModal={openAddModal}
        copiedId={copiedId}
        toggleMethod={toggleMethod}
        openEditModal={openEditModal}
        copyCredentials={copyCredentials}
        removeMethod={removeMethod}
      />

      {/* Moneda principal y secundarias */}
      <div className="grid gap-6 lg:grid-cols-2">
        <PrimaryCurrencyCard payments={payments} setPrimary={setPrimary} lodging={lodging} />

        {/* Monedas secundarias */}
        <SecondaryCurrenciesCard payments={payments} toggleWallet={toggleWallet} />
      </div>

      {/* Datos para transferencia estándar */}
      <BankDetailsCard savedBank={savedBank} bankDetails={bankDetails} saveBankDetails={saveBankDetails} />

      {/* Modal de Configuración de Llaves y Credenciales */}
      {editingMethod && (
        <PaymentMethodModal
          editingMethod={editingMethod}
          isNewMethod={isNewMethod}
          showSecret={showSecret}
          setShowSecret={setShowSecret}
          saveMethodModal={saveMethodModal}
          setEditingMethod={setEditingMethod}
          modalError={modalError}
        />
      )}
    </div>
  );
}
