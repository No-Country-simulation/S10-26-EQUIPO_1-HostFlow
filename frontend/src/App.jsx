import { createBrowserRouter, RouterProvider } from "react-router"
import { routes } from "./routes/index"
import ScrollToTop from "./components/layout/ScrollToTop"

const router = createBrowserRouter(routes)

export default function App() {
  return (
    <>
      {/* Al cambiar de ruta, la página vuelve al inicio. */}
      <ScrollToTop router={router} />
      <RouterProvider router={router} />
    </>
  )
}
