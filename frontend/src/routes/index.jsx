import { Navigate } from "react-router"
import ProtectedRoute from "./guards/ProtectedRoute"
import AuthenticatedAppLayout from "@components/layout/AuthenticatedAppLayout"
import AuthLayout from "@/components/layout/AuthLayout"
import { useSession } from "@features/auth/context/SessionContext"
import LoginPage from "@features/auth/pages/LoginPage"
import CreateAccountPage from "@features/auth/pages/CreateAccountPage"
import PasswordRecoveryPage from "@features/auth/pages/PasswordRecoveryPage"
import HomePage from "@features/home/pages/HomePage"
import OwnerDashboardPage from "@features/dashboard/pages/OwnerDashboardPage"
import BookingsPage from "@features/dashboard/pages/BookingsPage"
import LodgingPage from "@features/dashboard/pages/LodgingPage"
import AvailabilityPage from "@features/dashboard/pages/AvailabilityPage"
import PaymentMethodsPage from "@features/dashboard/pages/PaymentMethodsPage"
import LodgingProfilePage from "@features/dashboard/pages/LodgingProfilePage"
import AdminOverviewPage from "@features/admin/pages/AdminOverviewPage"

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
