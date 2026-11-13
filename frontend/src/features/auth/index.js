// API pública de la feature `auth`.
// Las pantallas del router y el resto de las features importan desde acá
// (`@features/auth`) en lugar de llegar hasta `pages/` o `context/`.
export { default as LoginPage } from "./pages/LoginPage"
export { default as CreateAccountPage } from "./pages/CreateAccountPage"
export { default as PasswordRecoveryPage } from "./pages/PasswordRecoveryPage"
// Contrato de sesión compartido (protege las rutas privadas de la interfaz).
export { SessionProvider, useSession } from "./context/SessionContext"
