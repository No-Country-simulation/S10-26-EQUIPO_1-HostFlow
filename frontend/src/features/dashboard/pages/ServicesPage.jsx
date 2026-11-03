import { useEffect, useState } from "react";
import { ConciergeBell, Pencil, Plus, Trash2 } from "lucide-react";
import { Alert, Badge, Button, Card, EmptyState, Field, Switch } from "@components/ui";
import Modal from "@components/ui/Modal";
import { useStore } from "@lib/state/useStore";
import { useSession } from "@features/auth/context/SessionContext";
import { DEMO_MODE } from "@lib/config/env";
import { cn, formatMoney } from "@utils/format";

const EMPTY_FORM = { name: "", price: "", description: "", pricing: "FIXED", perGuest: false };

const PRICING_OPTIONS = [
  { id: "FIXED", label: "Precio fijo por unidad", hint: "El huésped elige la cantidad (ej. limpieza extra, cuna)." },
  {
    id: "PER_NIGHT",
    label: "Por noche",
    hint: "Se multiplica por las noches de la estadía (ej. desayuno, toallas).",
  },
];

// Etiqueta del precio según cómo se cobra: noche, unidad y/o huésped.
const priceLabel = (pricing, perGuest) => {
  const parts = [];
  if (pricing === "PER_NIGHT") parts.push("por noche");
  else parts.push("por unidad");
  if (perGuest) parts.push("por huésped");
  return `Precio ${parts.join(" y ")}`;
};

// Sufijo corto en el listado: "/ noche", "/ huésped" o "/ noche × huésped".
const priceSuffix = (service) => {
  const perNight = service.pricing === "PER_NIGHT";
  if (perNight && service.perGuest) return "/ noche × huésped";
  if (perNight) return "/ noche";
  if (service.perGuest) return "/ huésped";
  return "";
};

export default function ServicesPage() {
  const { actions } = useSession();
  const services = useStore((s) => s.services);
  const lodging = useStore((s) => s.lodging);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editing, setEditing] = useState(null); // servicio en edición
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);

  const flashSaved = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 2200);
  };

  // Sincroniza el listado con el backend. En modo demo el store local ya es la
  // fuente de verdad, así que no hace falta pedir nada.
  useEffect(() => {
    if (DEMO_MODE) return;
    let alive = true;
    Promise.resolve()
      .then(() => actions.getServices?.())
      .catch((err) => {
        if (alive) setError(err?.message || "No se pudieron cargar los servicios desde el backend.");
      });
    return () => {
      alive = false;
    };
  }, [actions]);

  const setField = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }));

  const addService = async (event) => {
    event.preventDefault();
    if (!form.name.trim()) return setError("Ingresá el nombre del servicio.");
    if (Number(form.price) < 0) return setError("Ingresá un precio válido (0 si es gratuito).");
    setError("");
    try {
      await actions.addService({
        name: form.name,
        price: form.price,
        description: form.description,
        pricing: form.pricing,
        perGuest: form.perGuest,
      });
    } catch (err) {
      return setError(err?.message || "No se pudo guardar el servicio.");
    }
    setForm(EMPTY_FORM);
    flashSaved();
  };

  const saveEdit = async (event) => {
    event.preventDefault();
    if (!editing) return;
    if (!form.name.trim()) return setError("Ingresá el nombre del servicio.");
    if (Number(form.price) < 0) return setError("Ingresá un precio válido.");
    setError("");
    try {
      await actions.updateService(editing.id, {
        name: form.name.trim(),
        price: Number(form.price) || 0,
        pricing: form.pricing,
        perGuest: Boolean(form.perGuest),
        description: form.description.trim(),
      });
    } catch (err) {
      return setError(err?.message || "No se pudo guardar el servicio.");
    }
    closeEdit();
    flashSaved();
  };

  const openEdit = (service) => {
    setError("");
    setEditing(service);
    setForm({
      name: service.name,
      price: String(service.price),
      description: service.description || "",
      pricing: service.pricing || "FIXED",
      perGuest: Boolean(service.perGuest),
    });
  };

  const closeEdit = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setError("");
  };

  const remove = async (service) => {
    // window.confirm evita borrados accidentales; es el patrón nativo del proyecto.
    if (!window.confirm(`¿Eliminar el servicio "${service.name}"?`)) return;
    try {
      await actions.removeService(service.id);
    } catch (err) {
      return setError(err?.message || "No se pudo eliminar el servicio.");
    }
    flashSaved();
  };

  const toggle = async (service) => {
    try {
      await actions.toggleService(service.id);
    } catch (err) {
      setError(err?.message || "No se pudo cambiar el estado del servicio.");
    }
  };

  return (
    <div className="grid gap-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[clamp(24px,4vw,32px)] font-extrabold">Servicios adicionales</h1>
          <p className="mt-1 text-sm text-muted">
            Ofrecé extras a tus huéspedes: se suman al pedido desde su enlace de llegada.
          </p>
        </div>
        <Badge tone="lime">{services.filter((s) => s.enabled).length} activo{services.filter((s) => s.enabled).length === 1 ? "" : "s"}</Badge>
      </header>

      {!DEMO_MODE && (
        <Alert tone="info">
          El backend aún no expone el módulo de servicios: los cambios se guardan localmente en este navegador.
        </Alert>
      )}

      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        {/* Listado */}
        <div className="grid content-start gap-4">
          {services.length === 0 ? (
            <EmptyState
              icon={ConciergeBell}
              title="No hay servicios"
              text="Agregá el primero con el formulario de al lado."
            />
          ) : (
            services.map((service) => (
              <Card key={service.id} className={cn("p-5 transition-all", !service.enabled && "opacity-70")}>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-bold text-ink">{service.name}</p>
                      {service.enabled ? (
                        <Badge tone="teal">Activo</Badge>
                      ) : (
                        <Badge tone="neutral">Inactivo</Badge>
                      )}
                      <Badge tone={service.pricing === "PER_NIGHT" ? "pink" : "lime"}>
                        {service.pricing === "PER_NIGHT" ? "Por noche" : "Precio fijo"}
                      </Badge>
                      {service.perGuest && <Badge tone="teal">Por huésped</Badge>}
                    </div>
                    {service.description && <p className="mt-1 text-sm text-muted">{service.description}</p>}
                  </div>
                  <p className="text-lg font-extrabold text-ink">
                    {service.price === 0 ? "Gratis" : formatMoney(service.price, lodging.currency)}
                    {service.price > 0 && priceSuffix(service) && (
                      <span className="ml-1 text-xs font-semibold text-muted">{priceSuffix(service)}</span>
                    )}
                  </p>
                </div>

                <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-line pt-3.5">
                  <Switch
                    checked={service.enabled}
                    onChange={() => toggle(service)}
                    label={service.enabled ? "Visible para huéspedes" : "Oculto"}
                  />
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => openEdit(service)}
                      aria-label={`Editar el servicio ${service.name}`}
                      className="inline-flex cursor-pointer items-center gap-1.5 rounded-lg border border-line-strong px-3.5 py-2 text-xs font-bold text-ink-soft transition hover:border-ink/40 hover:text-ink"
                    >
                      <Pencil size={14} aria-hidden="true" /> Editar
                    </button>
                    <button
                      type="button"
                      onClick={() => remove(service)}
                      aria-label={`Eliminar el servicio ${service.name}`}
                      className="inline-flex cursor-pointer items-center gap-1.5 rounded-lg border border-line-strong px-3.5 py-2 text-xs font-bold text-ink-soft transition hover:border-danger hover:text-danger"
                    >
                      <Trash2 size={14} aria-hidden="true" /> Eliminar
                    </button>
                  </div>
                </div>
              </Card>
            ))
          )}
        </div>

        {/* Alta */}
        <Card className="h-fit p-6">
          <div className="sticky top-[65px] z-20 -mx-6 -mt-6 mb-4 flex items-center gap-2 border-b border-line bg-paper/95 px-6 pb-3 pt-2 backdrop-blur">
            <span className="grid size-10 place-items-center rounded-xl bg-lime-soft text-lime-ink">
              <Plus size={20} />
            </span>
            <div>
              <h2 className="font-bold text-ink">Agregar servicio</h2>
              <p className="text-xs text-muted">Nombre, forma de cobro, precio y una descripción corta.</p>
            </div>
          </div>

          <form onSubmit={addService} className="grid gap-4" noValidate>
            <Field
              label="Nombre del servicio"
              name="name"
              value={form.name}
              onChange={setField("name")}
              placeholder="Ej. Desayuno casero"
            />
            <Field
              as="select"
              label="Forma de cobro"
              name="pricing"
              value={form.pricing}
              onChange={setField("pricing")}
              hint={PRICING_OPTIONS.find((option) => option.id === form.pricing)?.hint}
            >
              {PRICING_OPTIONS.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.label}
                </option>
              ))}
            </Field>
            <div className="rounded-xl border border-line bg-cloud px-3.5 py-3">
              <Switch
                checked={Boolean(form.perGuest)}
                onChange={(next) => setForm((current) => ({ ...current, perGuest: next }))}
                label="Cobrar por huésped"
                description="Se multiplica por la cantidad de huéspedes de la reserva (ej. comidas, toallas)."
              />
            </div>
            <Field
              label={`${priceLabel(form.pricing, form.perGuest)} (${lodging.currency})`}
              name="price"
              type="number"
              min="0"
              step="100"
              value={form.price}
              onChange={setField("price")}
              placeholder="Ej. 3500"
              hint="Usá 0 si el servicio es gratuito."
            />
            <Field
              as="textarea"
              label="Descripción (opcional)"
              name="description"
              value={form.description}
              onChange={setField("description")}
              placeholder="Ej. Café, medialunas y fruta por persona."
            />

            {error && <Alert>{error}</Alert>}

            <Button type="submit" className="mt-1 w-full sm:w-auto sm:justify-self-end">
              Agregar servicio
            </Button>
          </form>
        </Card>
      </div>

      {/* Modal de edición */}
      <Modal open={Boolean(editing)} onClose={closeEdit} title="Editar servicio" subtitle={editing?.name}>
        <form onSubmit={saveEdit} className="grid gap-4" noValidate>
          <Field label="Nombre del servicio" name="name" value={form.name} onChange={setField("name")} autoFocus />
          <Field
            as="select"
            label="Forma de cobro"
            name="pricing"
            value={form.pricing}
            onChange={setField("pricing")}
            hint={PRICING_OPTIONS.find((option) => option.id === form.pricing)?.hint}
          >
            {PRICING_OPTIONS.map((option) => (
              <option key={option.id} value={option.id}>
                {option.label}
              </option>
            ))}
          </Field>
          <div className="rounded-xl border border-line bg-cloud px-3.5 py-3">
            <Switch
              checked={Boolean(form.perGuest)}
              onChange={(next) => setForm((current) => ({ ...current, perGuest: next }))}
              label="Cobrar por huésped"
              description="Se multiplica por la cantidad de huéspedes de la reserva (ej. comidas, toallas)."
            />
          </div>
          <Field
            label={`${priceLabel(form.pricing, form.perGuest)} (${lodging.currency})`}
            name="price"
            type="number"
            min="0"
            step="100"
            value={form.price}
            onChange={setField("price")}
          />
          <Field as="textarea" label="Descripción (opcional)" name="description" value={form.description} onChange={setField("description")} />

          {error && <Alert>{error}</Alert>}

          <div className="flex flex-wrap justify-end gap-2">
            <Button type="button" variant="ghost" onClick={closeEdit}>
              Cancelar
            </Button>
            <Button type="submit">Guardar cambios</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
