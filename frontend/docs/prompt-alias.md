Actúa como un Desarrollador Frontend experto en React y Vite. 
Necesito que me ayudes a configurar los "Path Aliases" (alias de rutas) en nuestro proyecto JavaScript con Vite para preparar la refactorización a una arquitectura orientada a dominios (Feature-Based).

Por favor, realiza los siguientes cambios de forma secuencial:

1. Configuración de Vite:
- Modifica o crea el archivo `vite.config.js` para incluir los siguientes alias usando `path.resolve`:
  - `@` apunta a `./src`
  - `@components` apunta a `./src/components`
  - `@features` apunta a `./src/features`
  - `@hooks` apunta a `./src/hooks`
  - `@lib` apunta a `./src/lib`
  - `@styles` apunta a `./src/styles`
  - `@utils` apunta a `./src/utils`

2. Configuración de VS Code / IDE:
- Crea o actualiza el archivo `jsconfig.json` en la raíz del proyecto para que el editor reconozca los alias y proporcione autocompletado correcto (configurando `compilerOptions.paths` acorde a los alias de Vite).

3. Validación y Ejemplos:
- Muestra brevemente cómo deben quedar los archivos de configuración actualizados para asegurarnos de que no falte ninguna dependencia (como 'path').

Por favor, ejecuta estos cambios paso a paso antes de que empecemos a mover los archivos de carpeta.