import { Navigate } from "react-router"
import RequireAuth from "./components/RequireAuth"
import AuthLayout from "./layouts/AuthLayout"
import Login from "./pages/auth/Login"
import Recover from "./pages/auth/Recover"
import Register from "./pages/auth/Register"
import Home from "./pages/Home"
import SessionStarted from "./pages/SessionStarted"

export const routes = [
  { path: "/", Component: Home },
  {
    path: "/auth",
    Component: AuthLayout,
    children: [
      { path: "login", Component: Login },
      { path: "registro", Component: Register },
      { path: "recuperar", Component: Recover },
    ],
  },
  // Destino temporal tras iniciar sesión (ver SessionStarted.jsx).
  {
    path: "/panel",
    Component: RequireAuth,
    children: [{ index: true, Component: SessionStarted }],
  },
  { path: "*", element: <Navigate to="/auth/login" replace /> },
]
