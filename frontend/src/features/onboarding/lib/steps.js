// Pasos del onboarding del huésped: etiqueta e ícono para la barra de progreso.
import { Check, ClipboardList, HeartPulse, Ship, UserRound } from "lucide-react";

export const STEPS = [
  { id: 0, label: "Tus datos", icon: UserRound },
  { id: 1, label: "Alérgenos y preferencias", icon: HeartPulse },
  { id: 2, label: "Servicios", icon: ClipboardList },
  { id: 3, label: "Traslado", icon: Ship },
  { id: 4, label: "Confirmar", icon: Check },
];
