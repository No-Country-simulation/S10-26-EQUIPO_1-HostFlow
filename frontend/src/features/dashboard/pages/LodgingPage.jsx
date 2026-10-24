import { useRef, useState } from "react";
import { BedDouble, ImagePlus, MapPin, Plus, Trash2 } from "lucide-react";
import { Alert, Badge, Button, Card, Field } from "@components/ui";
import { useStore } from "@/data/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { CURRENCIES, formatMoney, plural } from "@lib/format";

export default function LodgingPage() {
  const { actions } = useSession();
  const lodging = useStore((s) => s.lodging);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState("");
  const fileRef = useRef(null);

  const submit = (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const name = String(form.get("name") || "").trim();
    const rooms = Number(form.get("rooms"));
    const bathrooms = Number(form.get("bathrooms"));
    const pricePerNight = Number(form.get("pricePerNight"));
    const currency = String(form.get("currency") || "ARS");

    if (!name) return setError("Ingresá el nombre del alojamiento.");
    if (!rooms || rooms < 1) return setError("El número de habitaciones debe ser al menos 1.");
    if (bathrooms < 0) return setError("Ingresá una cantidad de baños válida.");
    if (!pricePerNight || pricePerNight <= 0) return setError("Ingresá un precio por noche válido.");

    setError("");
    actions.updateLodging({ name, rooms, bathrooms, pricePerNight, currency });
    setSaved(true);
    setTimeout(() => setSaved(false), 2600);
  };

  const pickImages = (event) => {
    const files = Array.from(event.target.files || []);
    files.forEach((file) => {
      const reader = new FileReader();
      reader.onload = () => actions.addImage(String(reader.result));
      reader.readAsDataURL(file);
    });
    event.target.value = "";
  };

  return (
    <div className="grid gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[clamp(24px,4vw,32px)] font-extrabold">Datos del alojamiento</h1>
          <p className="mt-1 text-sm text-muted">Nombre, capacidad, precio e imágenes de tu propiedad.</p>
        </div>
        {saved ? (
          <Alert tone="success">Cambios guardados correctamente.</Alert>
        ) : (
          <Badge tone="lime">{formatMoney(lodging.pricePerNight, lodging.currency)} / noche</Badge>
        )}
      </header>

      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        {/* Formulario */}
        <Card className="p-6">
          <form onSubmit={submit} className="grid gap-4" noValidate>
            <Field label="Nombre del alojamiento" name="name" defaultValue={lodging.name} placeholder="Ej. Cabañas del Arroyo" />
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Número de habitaciones" name="rooms" type="number" min="1" defaultValue={lodging.rooms} />
              <Field label="Número de baños" name="bathrooms" type="number" min="0" defaultValue={lodging.bathrooms} />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Precio por noche" name="pricePerNight" type="number" min="0" step="1000" defaultValue={lodging.pricePerNight} />
              <Field as="select" label="Moneda principal" name="currency" defaultValue={lodging.currency}>
                {Object.values(CURRENCIES).map((c) => (
                  <option key={c.code} value={c.code}>
                    {c.label} ({c.code})
                  </option>
                ))}
              </Field>
            </div>

            {error && <Alert>{error}</Alert>}

            <Button type="submit" className="mt-1 w-full sm:w-auto">
              Guardar cambios
            </Button>
          </form>
        </Card>

        {/* Imágenes */}
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="flex items-center gap-2 font-bold text-ink">
              <ImagePlus size={18} className="text-teal-strong" /> Imágenes
            </h2>
            <Badge tone="neutral">{plural(lodging.images.length, "imagen", "imágenes")}</Badge>
          </div>

          <div className="grid grid-cols-2 gap-3">
            {lodging.images.map((src, index) => (
              <div key={index} className="group relative aspect-[4/3] overflow-hidden rounded-xl border border-line">
                <img src={src} alt={`Imagen ${index + 1}`} className="size-full object-cover transition-transform duration-300 group-hover:scale-105" />
                <button
                  type="button"
                  onClick={() => actions.removeImage(index)}
                  aria-label="Eliminar imagen"
                  className="absolute right-2 top-2 grid size-8 cursor-pointer place-items-center rounded-lg bg-black/60 text-white opacity-0 transition group-hover:opacity-100 hover:bg-danger"
                >
                  <Trash2 size={15} />
                </button>
              </div>
            ))}

            <button
              type="button"
              onClick={() => fileRef.current?.click()}
              className="grid aspect-[4/3] cursor-pointer place-items-center gap-2 rounded-xl border border-dashed border-line-strong bg-cloud text-sm font-semibold text-ink-soft transition hover:border-teal hover:text-teal-strong"
            >
              <Plus size={18} /> Agregar
            </button>
          </div>

          <input ref={fileRef} type="file" accept="image/*" multiple hidden onChange={pickImages} />
          <p className="mt-3 text-xs text-muted">Subí hasta 8 imágenes. La primera se usa como portada.</p>
        </Card>
      </div>

      {/* Vista previa */}
      <Card className="overflow-hidden">
        <div
          className="grid place-items-center gap-3 px-6 py-14 text-center text-white"
          style={{ background: `linear-gradient(135deg, ${lodging.brand.primary}, #0b6b6b)` }}
        >
          <span className="grid size-16 place-items-center rounded-2xl bg-white/15">
            <BedDouble size={30} />
          </span>
          <h2 className="text-2xl font-extrabold">{lodging.name}</h2>
          <p className="flex items-center gap-1.5 text-sm text-white/80">
            <MapPin size={14} /> {lodging.location}
          </p>
          <p className="mt-2 text-lg font-bold">{formatMoney(lodging.pricePerNight, lodging.currency)} / noche</p>
        </div>
        <div className="flex flex-wrap gap-6 border-t border-line px-6 py-4 text-sm">
          <span className="font-semibold text-ink-soft">{plural(lodging.rooms, "habitación", "habitaciones")}</span>
          <span className="font-semibold text-ink-soft">{plural(lodging.bathrooms, "baño", "baños")}</span>
          <span className="font-semibold text-teal-strong">{plural(lodging.images.length, "imagen", "imágenes")}</span>
        </div>
      </Card>
    </div>
  );
}
