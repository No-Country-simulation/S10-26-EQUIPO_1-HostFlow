import { useRef, useState } from "react";
import { Building2, Check, ImagePlus, MapPin, Palette, Phone, Trash2 } from "lucide-react";
import { Alert, Badge, Button, Card, Field } from "@components/ui";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";

const COLOR_PRESETS = [
  { name: "Teal", value: "#0fa3a3" },
  { name: "Lima", value: "#d4f53c" },
  { name: "Rosa", value: "#f4b8c4" },
  { name: "Carbón", value: "#111111" },
  { name: "Azul", value: "#3b82f6" },
  { name: "Violeta", value: "#8b5cf6" },
];

const RADII = [
  { id: "rounded", label: "Redondeado", class: "rounded-2xl" },
  { id: "soft", label: "Suave", class: "rounded-lg" },
  { id: "sharp", label: "Recto", class: "rounded-md" },
  { id: "pill", label: "Píldora", class: "rounded-full" },
];

export default function LodgingProfilePage() {
  const { actions } = useSession();
  const lodging = useStore((s) => s.lodging);
  const [saved, setSaved] = useState(false);
  const fileRef = useRef(null);

  const flashSaved = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 2400);
  };

  const submit = (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    actions.updateLodging({
      name: String(form.get("name") || "").trim(),
      tagline: String(form.get("tagline") || "").trim(),
      location: String(form.get("location") || "").trim(),
      phone: String(form.get("phone") || "").trim(),
    });
    flashSaved();
  };

  const pickLogo = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      actions.setLogo(String(reader.result));
      flashSaved();
    };
    reader.readAsDataURL(file);
    event.target.value = "";
  };

  return (
    <div className="grid gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[clamp(24px,4vw,32px)] font-extrabold">Perfil y personalización</h1>
          <p className="mt-1 text-sm text-muted">Subí tu logo y adaptá la identidad de tu empresa.</p>
        </div>
        {saved && <Alert tone="success">Perfil actualizado.</Alert>}
      </header>

      <div className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
        {/* Logo */}
        <Card className="p-6">
          <div className="mb-4 flex items-center gap-2">
            <span className="grid size-10 place-items-center rounded-xl bg-teal-soft text-teal-strong">
              <ImagePlus size={20} />
            </span>
            <div>
              <h2 className="font-bold text-ink">Logo de la empresa</h2>
              <p className="text-xs text-muted">Se muestra en el panel y en el enlace de reserva.</p>
            </div>
          </div>

          <div className="grid place-items-center gap-4 rounded-2xl border border-dashed border-line-strong bg-cloud px-6 py-10">
            {lodging.logo ? (
              <img src={lodging.logo} alt="Logo" className="max-h-32 max-w-full rounded-xl object-contain" />
            ) : (
              <span className="grid size-24 place-items-center rounded-2xl bg-lime text-2xl font-extrabold text-lime-ink">
                {(lodging.name || "H").slice(0, 2).toUpperCase()}
              </span>
            )}
            <p className="text-xs text-muted">{lodging.name}</p>
          </div>

          <div className="mt-4 flex gap-2">
            <Button type="button" variant="secondary" icon={ImagePlus} className="flex-1" onClick={() => fileRef.current?.click()}>
              {lodging.logo ? "Cambiar logo" : "Subir logo"}
            </Button>
            {lodging.logo && (
              <Button type="button" variant="ghost" icon={Trash2} onClick={() => { actions.setLogo(null); flashSaved(); }}>
                Quitar
              </Button>
            )}
          </div>
          <input ref={fileRef} type="file" accept="image/*" hidden onChange={pickLogo} />
          <p className="mt-3 text-xs text-muted">PNG o SVG, fondo transparente recomendado.</p>
        </Card>

        {/* Datos de la empresa */}
        <Card className="p-6">
          <div className="mb-4 flex items-center gap-2">
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
            <Button type="submit" className="mt-1 w-full sm:w-auto">Guardar datos</Button>
          </form>
        </Card>
      </div>

      {/* Marca */}
      <Card className="p-6">
        <div className="mb-5 flex items-center gap-2">
          <span className="grid size-10 place-items-center rounded-xl bg-pink-soft text-pink-strong">
            <Palette size={20} />
          </span>
          <div>
            <h2 className="font-bold text-ink">Personalización visual</h2>
            <p className="text-xs text-muted">Definí los colores y la forma de los elementos de tu marca.</p>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          <div className="grid gap-5">
            <div>
              <p className="mb-2 text-sm font-bold text-ink">Color principal</p>
              <div className="flex flex-wrap gap-2.5">
                {COLOR_PRESETS.map((preset) => {
                  const active = lodging.brand.primary.toLowerCase() === preset.value.toLowerCase();
                  return (
                    <button
                      key={preset.value}
                      type="button"
                      onClick={() => { actions.updateBrand({ primary: preset.value }); flashSaved(); }}
                      title={preset.name}
                      className="grid size-11 cursor-pointer place-items-center rounded-xl border-2 transition-all hover:scale-105"
                      style={{
                        backgroundColor: preset.value,
                        borderColor: active ? "var(--color-ink)" : "transparent",
                      }}
                    >
                      {active && <Check size={18} className="text-white drop-shadow" />}
                    </button>
                  );
                })}
              </div>
            </div>

            <div>
              <p className="mb-2 text-sm font-bold text-ink">Color de acento</p>
              <div className="flex flex-wrap gap-2.5">
                {COLOR_PRESETS.map((preset) => {
                  const active = lodging.brand.accent.toLowerCase() === preset.value.toLowerCase();
                  return (
                    <button
                      key={preset.value}
                      type="button"
                      onClick={() => { actions.updateBrand({ accent: preset.value }); flashSaved(); }}
                      title={preset.name}
                      className="grid size-11 cursor-pointer place-items-center rounded-xl border-2 transition-all hover:scale-105"
                      style={{ backgroundColor: preset.value, borderColor: active ? "var(--color-teal)" : "transparent" }}
                    >
                      {active && <Check size={18} className="text-ink drop-shadow" />}
                    </button>
                  );
                })}
              </div>
            </div>

            <div>
              <p className="mb-2 text-sm font-bold text-ink">Forma de los elementos</p>
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                {RADII.map((radius) => {
                  const active = lodging.brand.radius === radius.id;
                  return (
                    <button
                      key={radius.id}
                      type="button"
                      onClick={() => { actions.updateBrand({ radius: radius.id }); flashSaved(); }}
                      className={`cursor-pointer border bg-cloud px-2 py-3 text-xs font-bold transition-all ${radius.class} ${
                        active ? "border-teal text-teal-strong" : "border-line text-ink-soft hover:border-line-strong"
                      }`}
                    >
                      {radius.label}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Vista previa */}
          <div
            className="grid content-center gap-4 rounded-2xl p-6 text-white"
            style={{ background: `linear-gradient(135deg, ${lodging.brand.primary}, #0b6b6b)` }}
          >
            <div className="flex items-center gap-3">
              {lodging.logo ? (
                <img src={lodging.logo} alt="Logo" className="size-12 rounded-lg bg-white object-contain p-1" />
              ) : (
                <span
                  className="grid size-12 place-items-center text-sm font-extrabold"
                  style={{ background: lodging.brand.accent, color: "#111", borderRadius: "var(--radius-chip)" }}
                >
                  {(lodging.name || "H").slice(0, 2).toUpperCase()}
                </span>
              )}
              <div>
                <p className="font-extrabold leading-tight">{lodging.name}</p>
                <p className="text-xs text-white/75">{lodging.tagline}</p>
              </div>
            </div>
            <p className="text-sm text-white/85">{lodging.location}</p>
            <div className="flex flex-wrap items-center gap-2 text-xs">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-white/15 px-3 py-1.5">
                <MapPin size={13} /> {lodging.location}
              </span>
              <span className="inline-flex items-center gap-1.5 rounded-full bg-white/15 px-3 py-1.5">
                <Phone size={13} /> {lodging.phone}
              </span>
            </div>
            <span className="inline-flex w-fit items-center gap-2 px-4 py-2.5 text-sm font-bold" style={{ background: lodging.brand.accent, color: "#111" }}>
              Reservar ahora
            </span>
          </div>
        </div>

        <div className="mt-5 flex items-center gap-3">
          <Badge tone="neutral">Tema: automático (claro/oscuro)</Badge>
          <span className="text-xs text-muted">Los colores de marca se mantienen en ambos modos.</span>
        </div>
      </Card>
    </div>
  );
}
