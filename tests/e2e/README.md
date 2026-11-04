# Pruebas End-to-End (E2E) — Cypress

## Objetivo
El objetivo de esta carpeta es almacenar y organizar todas las pruebas de integración de extremo a extremo (**End-to-End / E2E**) automatizadas con Cypress. 

Estas pruebas simulan el flujo completo que realizaría un usuario real navegando por la interfaz de la aplicación (Frontend), interactuando con los componentes, realizando acciones (clics, llenado de formularios, navegación) y validando que las respuestas visuales y la comunicación con la API (Backend) funcionen correctamente en su conjunto.

---

## Tipos de Pruebas Almacenadas
Dentro de esta carpeta se deben incluir:

* **Flujos de usuario completos (User Journeys):** Registro, inicio de sesión (Login/Logout), recuperación de contraseña, procesos de compra, flujos clave del negocio, entre otros.
* **Pruebas de navegación y Vistas (UI Testing):** Verificación de renderizado de componentes críticos, rutas protegidas, redirecciones y modales.
* **Pruebas de integración Frontend - Backend:** Simulación de entrada de datos en la interfaz y verificación de los cambios reflejados tras consumir las APIs reales o simuladas (*mocked*).
* **Validación de escenarios de error en la UI:** Comprobación de mensajes de validación en formularios, alertas ante fallos de servidor y manejo de errores visibles para el usuario.

