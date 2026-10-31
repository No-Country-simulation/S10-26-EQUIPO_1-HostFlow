import { useEffect } from "react";

// Posiciona la página en el top cada vez que cambia la ruta.
// Se suscribe al router de createBrowserRouter (App.jsx): `useLocation` no
// funciona FUERA del <RouterProvider>, y este enfoque tampoco remonta componentes
// ni interfiere con las animaciones de entrada de los layouts.
export default function ScrollToTop({ router }) {
  useEffect(() => {
    let lastPath = router.state.location.pathname;
    const scroll = () => {
      const { pathname } = router.state.location;
      if (pathname === lastPath) return; // solo al cambiar de ruta
      lastPath = pathname;
      window.scrollTo({ top: 0, left: 0, behavior: "auto" });
    };
    window.scrollTo({ top: 0, left: 0, behavior: "auto" }); // ruta inicial
    return router.subscribe(scroll);
  }, [router]);

  return null;
}
