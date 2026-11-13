import { Landmark } from "lucide-react";
import { Alert, Button, Card, Field } from "@components/ui";

export function BankDetailsCard({ savedBank, bankDetails, saveBankDetails }) {
  return (
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
          <Field label="Titular de la cuenta" name="holder" defaultValue={bankDetails.holder} placeholder="Ej. Cabañas del Arroyo" required />
          <Field label="Alias" name="alias" defaultValue={bankDetails.alias} placeholder="Ej. cabañas.arroyo" required />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Banco o entidad" name="bank" defaultValue={bankDetails.bank} placeholder="Ej. Banco Nación" required />
          <Field label="Tipo de cuenta" name="accountType" defaultValue={bankDetails.accountType} placeholder="Ej. Caja de ahorro" required />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Número de cuenta" name="accountNumber" defaultValue={bankDetails.accountNumber} placeholder="Número de cuenta" required />
          <Field label="CBU / CVU" name="cbu" defaultValue={bankDetails.cbu} placeholder="0000000000000000000000" required />
        </div>
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
  );
}
