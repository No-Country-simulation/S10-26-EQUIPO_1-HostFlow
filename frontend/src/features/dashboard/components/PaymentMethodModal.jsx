import { Eye, EyeOff } from "lucide-react";
import { Alert, Button, Field, Modal } from "@components/ui";

export function PaymentMethodModal({
  editingMethod,
  isNewMethod,
  showSecret,
  setShowSecret,
  saveMethodModal,
  setEditingMethod,
  modalError,
}) {
  return (
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
              label="Tipo de cuenta"
              name="accountType"
              defaultValue={editingMethod.keys?.accountType || ""}
              placeholder="Ej. Caja de ahorro"
            />
            <Field
              label="Número de cuenta"
              name="accountNumber"
              defaultValue={editingMethod.keys?.accountNumber || ""}
              placeholder="Número de cuenta"
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
              label="Teléfono de billetera"
              name="phone"
              defaultValue={editingMethod.keys?.phone || ""}
              placeholder="Ej. +54 9 11 1234 5678"
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
  );
}
