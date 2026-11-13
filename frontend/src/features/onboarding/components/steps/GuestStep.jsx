// Paso 1 del onboarding: datos personales del huésped.
// Los errores se muestran debajo de cada input (`Field` + `error`), igual que en
// el onboarding del panel.
import { Alert, Badge, Field } from "@components/ui";
import { Clock, IdCard, Mail, Phone, ShieldCheck, UserRound } from "lucide-react";
import StepHeading from "../StepHeading";
import StayCard from "../StayCard";

export default function GuestStep({ booking, guest, fieldErrors, error, lodging, nights, setGuestField, blurGuestField }) {
  return (
    <div className="card grid gap-4 overflow-hidden p-5 animate-rise sm:p-6">
      <StepHeading
        icon={UserRound}
        eyebrow="Ficha de llegada"
        title={`¡Hola, ${booking.guestName.split(" ")[0]}!`}
        text={`Completá tus datos para ${lodging.name}. Solo toma un minuto.`}
        aside={
          <Badge tone="teal">
            <Clock size={11} /> 1 minuto
          </Badge>
        }
      />

      {/* Tarjeta de la estadía: fechas, noches y huéspedes con la marca */}
      <StayCard booking={booking} lodging={lodging} nights={nights} />

      {/* Nombre y apellido: dos campos, un solo dato al enviar. */}
      <div className="grid gap-4 sm:grid-cols-2">
        <Field
          label={
            <span className="inline-flex items-center gap-1.5">
              <UserRound size={13} className="text-teal-strong" aria-hidden="true" /> Nombre
            </span>
          }
          name="firstName"
          autoComplete="given-name"
          value={guest.firstName}
          onChange={setGuestField("firstName")}
          onBlur={blurGuestField("firstName")}
          error={fieldErrors.firstName}
          aria-invalid={Boolean(fieldErrors.firstName)}
          placeholder="Ej. María"
          autoFocus
        />
        <Field
          label="Apellido"
          name="lastName"
          autoComplete="family-name"
          value={guest.lastName}
          onChange={setGuestField("lastName")}
          onBlur={blurGuestField("lastName")}
          error={fieldErrors.lastName}
          aria-invalid={Boolean(fieldErrors.lastName)}
          placeholder="Ej. González"
        />
      </div>
      <Field
        label={
          <span className="inline-flex items-center gap-1.5">
            <IdCard size={13} className="text-teal-strong" aria-hidden="true" /> DNI / Pasaporte
          </span>
        }
        name="guestDocument"
        value={guest.guestDocument}
        onChange={setGuestField("guestDocument")}
        onBlur={blurGuestField("guestDocument")}
        error={fieldErrors.guestDocument}
        aria-invalid={Boolean(fieldErrors.guestDocument)}
        placeholder="Ej. 30.123.456 o AB123456"
        hint="Tal como figura en tu documento."
        required
      />
      <Field
        label={
          <span className="inline-flex items-center gap-1.5">
            <Mail size={13} className="text-teal-strong" aria-hidden="true" /> Correo electrónico
          </span>
        }
        name="guestEmail"
        type="email"
        autoComplete="email"
        value={guest.guestEmail}
        onChange={setGuestField("guestEmail")}
        onBlur={blurGuestField("guestEmail")}
        error={fieldErrors.guestEmail}
        aria-invalid={Boolean(fieldErrors.guestEmail)}
      />
      <Field
        label={
          <span className="inline-flex items-center gap-1.5">
            <Phone size={13} className="text-teal-strong" aria-hidden="true" /> Teléfono / WhatsApp
          </span>
        }
        name="guestPhone"
        type="tel"
        inputMode="tel"
        autoComplete="tel"
        value={guest.guestPhone}
        onChange={setGuestField("guestPhone")}
        onBlur={blurGuestField("guestPhone")}
        error={fieldErrors.guestPhone}
        aria-invalid={Boolean(fieldErrors.guestPhone)}
        placeholder="Ej. +54 9 11 5555 0101"
        hint="Con código de área. Lo usamos para coordinar tu llegada."
        required
      />

      {error && <Alert>{error}</Alert>}

      <div className="flex items-start gap-2 rounded-xl border border-line bg-cloud px-3.5 py-3 text-xs text-muted">
        <ShieldCheck size={15} className="mt-0.5 shrink-0 text-teal-strong" aria-hidden="true" />
        <span>Tus datos se comparten solo con {lodging.name} para preparar tu llegada.</span>
      </div>
    </div>
  );
}
