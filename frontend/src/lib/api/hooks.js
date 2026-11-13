// Hooks de lectura de datos de la aplicación.
//
// Las vistas consumen estos hooks en lugar de importar `useStore` directamente:
// así la frontera de datos queda en `@lib/api` y el día que el origen cambie
// (store de demo, caché del backend, etc.) solo se toca este archivo.
import { useStore } from "@lib/state/useStore";

export const useCurrentUser = () => useStore((s) => s.user);
export const useLodging = () => useStore((s) => s.lodging);
export const useBrand = () => useStore((s) => s.lodging?.brand);
export const useBookings = () => useStore((s) => s.bookings);
export const useBooking = (id) => useStore((s) => s.bookings.find((b) => b.id === id));
export const usePayments = () => useStore((s) => s.payments);
export const useServices = () => useStore((s) => s.services);
export const useBlockedDays = () => useStore((s) => s.blockedDays);
export const useBankDetails = () => useStore((s) => s.payments?.bankDetails);
export const useAdminLodgings = () => useStore((s) => s.admin?.lodgings || []);
