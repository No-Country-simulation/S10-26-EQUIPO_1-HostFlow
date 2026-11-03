import React from "react"
import ReactDOM from "react-dom/client"
import "@fontsource-variable/dm-sans"
import "@fontsource-variable/fraunces"
import App from "./App.jsx"
import { ThemeProvider } from "@lib/theme/ThemeContext"
import { SessionProvider } from "@features/auth/context/SessionContext"
import "@styles/globals.css"

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <ThemeProvider>
      <SessionProvider>
        <App />
      </SessionProvider>
    </ThemeProvider>
  </React.StrictMode>,
)
