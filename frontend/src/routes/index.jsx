import { Navigate } from "react-router"
import ProtectedRoute from "./guards/ProtectedRoute"
import AuthenticatedAppLayout from "@components/layout/AuthenticatedAppLayout"
import AuthLayout from "@components/layout/AuthLayout"
// Cada feature se consume por su barrel: una única puerta de entrada por feature.
import { useSession, LoginPage, CreateAccountPage, PasswordRecoveryPage } from "@features/auth"
import { HomePage } from "@features/home"
import {
  OwnerDashboardPage,
  BookingsPage,
  LodgingPage,
  AvailabilityPage,
  PaymentMethodsPage,
  LodgingProfilePage,
  ServicesPage,
} from "@features/dashboard"
import { AdminOverviewPage } from "@features/admin"
import { OnboardingPage } from "@features/onboarding"

// El panel de anfitrión no aplica a un administrador: se lo manda a su dashboard.
function PanelIndex() {
  const { isAdmin } = useSession()
  return isAdmin ? <Navigate to="/admin" replace /> : <OwnerDashboardPage />
}

export const routes = [
  { path: "/", Component: HomePage },
  {
    path: "/auth",
    Component: AuthLayout,
    children: [
      { path: "login", Component: LoginPage },
      { path: "registro", Component: CreateAccountPage },
      { path: "recuperar", Component: PasswordRecoveryPage },
    ],
  },
  // Onboarding del huésped (B2C): enlace único por alojamiento y reserva,
  // sin cuenta ni sesión. La identidad visual es la del alojamiento.
  { path: "/llegada/:lodgingId/:bookingId", Component: OnboardingPage },
  { path: "/reservations/confirm", Component: OnboardingPage },
  {
    path: "/panel",
    Component: ProtectedRoute,
    children: [
      {
        path: "",
        Component: AuthenticatedAppLayout,
        children: [
          { index: true, Component: PanelIndex },
          { path: "reservas", Component: BookingsPage },
          { path: "alojamiento", Component: LodgingPage },
          { path: "disponibilidad", Component: AvailabilityPage },
          { path: "pagos", Component: PaymentMethodsPage },
          { path: "servicios", Component: ServicesPage },
          { path: "perfil", Component: LodgingProfilePage },
        ],
      },
    ],
  },
  {
    path: "/admin",
    element: <ProtectedRoute adminOnly />,
    children: [
      {
        path: "",
        Component: AuthenticatedAppLayout,
        children: [{ index: true, Component: AdminOverviewPage }],
      },
    ],
  },
  { path: "*", element: <Navigate to="/auth/login" replace /> },
]
