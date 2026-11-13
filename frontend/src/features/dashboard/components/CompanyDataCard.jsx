import { Building2 } from "lucide-react";
import { Button, Card, Field } from "@components/ui";

export function CompanyDataCard({ lodging, submit }) {
  return (
    <Card className="p-6">
      <div className="sticky top-[65px] z-20 -mx-6 -mt-6 mb-4 flex items-center gap-2 border-b border-line bg-paper/95 px-6 pb-3 pt-2 backdrop-blur">
        <span className="grid size-10 place-items-center rounded-xl bg-lime-soft text-lime-ink">
          <Building2 size={20} />
        </span>
        <div>
          <h2 className="font-bold text-ink">Datos de la empresa</h2>
          <p className="text-xs text-muted">Información que ven tus huéspedes.</p>
        </div>
      </div>

      <form onSubmit={submit} className="grid gap-4">
        <Field label="Nombre de la empresa" name="name" defaultValue={lodging.name} />
        <Field label="Eslogan" name="tagline" defaultValue={lodging.tagline} placeholder="Ej. Descanso frente al río" />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Ubicación" name="location" defaultValue={lodging.location} />
          <Field label="Teléfono" name="phone" defaultValue={lodging.phone} />
        </div>
        <Button type="submit" className="mt-1 w-full sm:w-auto sm:justify-self-end">Guardar datos</Button>
      </form>
    </Card>
  );
}
