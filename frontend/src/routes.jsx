import { Navigate } from "react-router"
import RequireAuth from "./components/RequireAuth"
import AppLayout from "./components/AppLayout"
import AuthLayout from "./layouts/AuthLayout"
import { useSession } from "@features/auth/context/SessionContext"
import LoginPage from "@features/auth/pages/LoginPage"
import CreateAccountPage from "@features/auth/pages/CreateAccountPage"
import PasswordRecoveryPage from "@features/auth/pages/PasswordRecoveryPage"
import HomePage from "@features/home/pages/HomePage"
import HostDashboardPage from "@/features/dashboard/pages/OwnerDashboardPage"
import BookingsPage from "@features/dashboard/pages/BookingsPage"
import LodgingPage from "@features/dashboard/pages/LodgingPage"
import AvailabilityPage from "@features/dashboard/pages/AvailabilityPage"
import PaymentMethodsPage from "@features/dashboard/pages/PaymentMethodsPage"
import LodgingProfilePage from "@features/dashboard/pages/LodgingProfilePage"
import AdminDashboard from "./pages/admin/AdminDashboard"

// El panel de anfitrión no aplica a un administrador: se lo manda a su dashboard.
function PanelIndex() {
  const { isAdmin } = useSession()
  return isAdmin ? <Navigate to="/admin" replace /> : <HostDashboardPage />
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
    Component: RequireAuth,
    children: [
      {
        path: "",
        Component: AppLayout,
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
    element: <RequireAuth adminOnly />,
    children: [
      {
        path: "",
        Component: AppLayout,
        children: [{ index: true, Component: AdminDashboard }],
      },
    ],
  },
  { path: "*", element: <Navigate to="/auth/login" replace /> },
]
