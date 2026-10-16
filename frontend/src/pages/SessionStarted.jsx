import { LogOut } from "lucide-react";
import { useNavigate } from "react-router";
import { AuthCard, AuthTitle, Brand, Button, DemoBanner, Eyebrow } from "../components/ui";
import { DEMO_MODE } from "../config/env";
import { api } from "../services";

// PANTALLA TEMPORAL posterior al ingreso. Existe solo para poder comprobar el flujo de
// autenticación de punta a punta (login/registro → sesión → cerrar sesión).
// Login y Registro navegan a "/panel": reemplazá esta pantalla por el panel real
// cuando se incorpore ese módulo.
export default function SessionStarted() {
  const navigate = useNavigate();

  const logout = () => {
    api.logout();
    navigate("/auth/login");
  };

  return (
    <main className="relative grid min-h-screen place-items-center overflow-auto px-[6%] py-16 max-[800px]:px-5.5 max-[800px]:pb-10 max-[800px]:pt-20.5">
      <DemoBanner className="absolute inset-x-0 top-0" />

      <AuthCard>
        <div className="mb-7">
          <Brand />
        </div>

        <Eyebrow>Sesión iniciada</Eyebrow>

        <AuthTitle>Autenticación completada</AuthTitle>

        <p className="mb-6 text-muted">
          {DEMO_MODE
            ? "Estás en modo demostración: no se validó ninguna credencial ni se creó ninguna sesión real."
            : "Tu sesión está activa. Esta pantalla es temporal: el panel todavía no está incluido en esta versión."}
        </p>

        <Button variant="secondary" className="w-full" icon={LogOut} onClick={logout}>
          Cerrar sesión
        </Button>
      </AuthCard>
    </main>
  );
}
