import { Navigate } from "react-router"
import RequireAuth from "./components/RequireAuth"
import AppLayout from "./components/AppLayout"
import AuthLayout from "./layouts/AuthLayout"
import { useSession } from "@features/auth/context/SessionContext"
import Home from "./pages/Home"
import LoginPage from "@features/auth/pages/LoginPage"
import CreateAccountPage from "@features/auth/pages/CreateAccountPage"
import PasswordRecoveryPage from "@features/auth/pages/PasswordRecoveryPage"
import Dashboard from "./pages/app/Dashboard"
import Bookings from "./pages/app/Bookings"
import Lodging from "./pages/app/Lodging"
import Availability from "./pages/app/Availability"
import Payments from "./pages/app/Payments"
import Profile from "./pages/app/Profile"
import AdminDashboard from "./pages/admin/AdminDashboard"

// El panel de anfitrión no aplica a un administrador: se lo manda a su dashboard.
function PanelIndex() {
  const { isAdmin } = useSession()
  return isAdmin ? <Navigate to="/admin" replace /> : <Dashboard />
}

export const routes = [
  { path: "/", Component: Home },
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
          { path: "reservas", Component: Bookings },
          { path: "alojamiento", Component: Lodging },
          { path: "disponibilidad", Component: Availability },
          { path: "pagos", Component: Payments },
          { path: "perfil", Component: Profile },
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
