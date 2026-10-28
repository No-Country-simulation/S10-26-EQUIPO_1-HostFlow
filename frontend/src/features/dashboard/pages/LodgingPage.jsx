import { useRef, useState } from "react";
import { BedDouble, ImagePlus, MapPin, Plus, Trash2 } from "lucide-react";
import { Alert, Badge, Button, Card, Field } from "@components/ui";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { MAX_IMAGES } from "@lib/state/store";
import { readableOn } from "@utils/color";
import { CURRENCIES, formatMoney, plural } from "@utils/format";

// Validación de peso de las fotos: hasta 2 MB por archivo, sin mínimo (una foto
// chica se acepta igual). Solo se rechaza lo que supera el máximo o un archivo
// vacío, con un mensaje claro antes de leerlo.
const MAX_IMAGE_BYTES = 2 * 1024 * 1024;

const formatSize = (bytes) =>
  bytes >= 1024 * 1024 ? `${(bytes / (1024 * 1024)).toFixed(1)} MB` : `${Math.round(bytes / 1024)} KB`;

export default function LodgingPage() {
  const { actions } = useSession();
  const lodging = useStore((s) => s.lodging);
  const images = Array.isArray(lodging.images) ? lodging.images : [];
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState("");
  const [imageErrors, setImageErrors] = useState([]);
  const [imageNotice, setImageNotice] = useState("");
  const fileRef = useRef(null);

  const submit = (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const name = String(form.get("name") || "").trim();
    const rooms = Number(form.get("rooms"));
    const bathrooms = Number(form.get("bathrooms"));
    const capacity = Number(form.get("capacity"));
    const pricePerNight = Number(form.get("pricePerNight"));
    const currency = String(form.get("currency") || "ARS");

    if (!name) return setError("Ingresá el nombre del alojamiento.");
    if (!rooms || rooms < 1) return setError("El número de habitaciones debe ser al menos 1.");
    if (bathrooms < 0) return setError("Ingresá una cantidad de baños válida.");
    if (!capacity || capacity < 1) return setError("Ingresá la capacidad en huéspedes (al menos 1).");
    if (!pricePerNight || pricePerNight <= 0) return setError("Ingresá un precio por noche válido.");

    setError("");
    actions.updateLodging({ name, rooms, bathrooms, capacity, pricePerNight, currency });
    setSaved(true);
    setTimeout(() => setSaved(false), 2600);
  };

  // Subida de 0 a 4 fotos con validación de peso (hasta 2 MB cada una).
  const pickImages = (event) => {
    const files = Array.from(event.target.files || []);
    event.target.value = "";
    if (files.length === 0) return;

    const free = MAX_IMAGES - images.length;
    if (free <= 0) {
      setImageNotice("");
      setImageErrors([`Ya tenés las ${MAX_IMAGES} fotos permitidas. Eliminá una para subir otra.`]);
      return;
    }

    const accepted = [];
    const rejected = [];

    files.slice(0, free).forEach((file) => {
      if (!file.size) {
        rejected.push(`${file.name}: el archivo está vacío o dañado.`);
      } else if (file.size > MAX_IMAGE_BYTES) {
        rejected.push(`${file.name}: pesa ${formatSize(file.size)} y el máximo es 2 MB.`);
      } else {
        accepted.push(file);
      }
    });

    if (files.length > free) {
      rejected.push(`Solo podés tener ${MAX_IMAGES} fotos: se subieron las primeras ${free}.`);
    }

    accepted.forEach((file) => {
      const reader = new FileReader();
      reader.onload = () => actions.addImage(String(reader.result));
      reader.readAsDataURL(file);
    });

    setImageErrors(rejected);
    setImageNotice(
      accepted.length ? `${plural(accepted.length, "foto agregada", "fotos agregadas")}.` : "",
    );
    if (accepted.length) setTimeout(() => setImageNotice(""), 2600);
  };

  const cover = images[0];
  const heroInk = readableOn(lodging.brand?.primary || "#0fa3a3");

  return (
    <div className="grid gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[clamp(24px,4vw,32px)] font-extrabold">Datos del alojamiento</h1>
          <p className="mt-1 text-sm text-muted">Nombre, capacidad, precio y fotos de tu propiedad.</p>
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
            <Field
              label="Capacidad (huéspedes)"
              name="capacity"
              type="number"
              min="1"
              defaultValue={lodging.capacity || 1}
              placeholder="Ej. 10"
            />
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

        {/* Fotos: opcionales, de 0 a 4 (la primera es la portada del enlace) */}
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between gap-2">
            <h2 className="flex items-center gap-2 font-bold text-ink">
              <ImagePlus size={18} className="text-teal-strong" /> Fotos
            </h2>
            <Badge tone={images.length ? "teal" : "neutral"}>
              {images.length} / {MAX_IMAGES}
            </Badge>
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            {images.map((src, index) => (
              <div key={index} className="group relative aspect-[4/3] overflow-hidden rounded-xl border border-line">
                <img src={src} alt={`Imagen ${index + 1}`} className="size-full object-cover transition-transform duration-300 group-hover:scale-105" />
                {index === 0 && (
                  <span className="absolute left-1.5 top-1.5 rounded-full bg-black/60 px-2 py-0.5 text-[10px] font-bold text-white">
                    Portada
                  </span>
                )}
                <button
                  type="button"
                  onClick={() => actions.removeImage(index)}
                  aria-label={`Eliminar imagen ${index + 1}`}
                  className="absolute right-2 top-2 grid size-8 cursor-pointer place-items-center rounded-lg bg-black/60 text-white opacity-100 transition focus-visible:opacity-100 hover:bg-danger lg:opacity-0 lg:group-hover:opacity-100"
                >
                  <Trash2 size={15} />
                </button>
              </div>
            ))}

            {images.length < MAX_IMAGES && (
              <button
                type="button"
                onClick={() => fileRef.current?.click()}
                className="grid aspect-[4/3] cursor-pointer place-items-center gap-2 rounded-xl border border-dashed border-line-strong bg-cloud text-sm font-semibold text-ink-soft transition hover:border-teal hover:text-teal-strong"
              >
                <Plus size={18} /> Agregar
              </button>
            )}
          </div>

          {images.length === 0 && (
            <p className="mt-3 text-xs text-muted">
              Sin fotos también funciona: el enlace del huésped se ve limpio con tus colores. Las fotos son opcionales.
            </p>
          )}

          <input
            ref={fileRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            multiple
            hidden
            onChange={pickImages}
          />

          {imageErrors.length > 0 && (
            <div className="mt-3">
              <Alert>
                <span className="grid gap-1">
                  {imageErrors.map((message) => (
                    <span key={message}>{message}</span>
                  ))}
                </span>
              </Alert>
            </div>
          )}
          {imageNotice && (
            <p className="mt-3 text-xs font-semibold text-success" role="status">
              {imageNotice}
            </p>
          )}

          <p className="mt-3 text-xs text-muted">
            Hasta {MAX_IMAGES} fotos · JPG, PNG o WebP · hasta 2 MB cada una. La primera es la portada del enlace.
          </p>
        </Card>
      </div>

      {/* Vista previa (color de marca plano + portada si existe) */}
      <Card className="overflow-hidden">
        <div
          className="relative grid place-items-center gap-3 overflow-hidden px-6 py-14 text-center"
          style={{ backgroundColor: lodging.brand?.primary || "#0fa3a3", color: heroInk }}
        >
          {cover && <img src={cover} alt="" aria-hidden="true" className="absolute inset-0 size-full object-cover opacity-25" />}
          <div className="relative grid place-items-center gap-3">
            <span className="grid size-16 place-items-center rounded-2xl" style={{ backgroundColor: heroInk, color: lodging.brand?.primary }}>
              <BedDouble size={30} />
            </span>
            <h2 className="text-2xl font-extrabold">{lodging.name}</h2>
            <p className="flex items-center gap-1.5 text-sm opacity-80">
              <MapPin size={14} /> {lodging.location}
            </p>
            <p className="mt-2 text-lg font-bold">{formatMoney(lodging.pricePerNight, lodging.currency)} / noche</p>
          </div>
        </div>
        <div className="flex flex-wrap gap-6 border-t border-line px-6 py-4 text-sm">
          <span className="font-semibold text-ink-soft">{plural(lodging.rooms, "habitación", "habitaciones")}</span>
          <span className="font-semibold text-ink-soft">{plural(lodging.bathrooms, "baño", "baños")}</span>
          <span className="font-semibold text-teal-strong">{plural(images.length, "foto", "fotos")}</span>
        </div>
      </Card>
    </div>
  );
}
