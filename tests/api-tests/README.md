# Pruebas de API — Postman 

## Objetivo
El objetivo de esta carpeta es centralizar, documentar y mantener todas las pruebas automatizadas y colecciones enfocadas en la **capa de servicios y APIs (Backend)** de la aplicación mediante **Postman**.

Estas pruebas se ejecutan de manera directa contra los servicios REST (HTTP) del sistema, sin depender de la interfaz gráfica (Frontend), con el fin de validar el comportamiento, rendimiento, seguridad, estructuras de respuesta y reglas de negocio del servidor.

---

## Tipos de Pruebas Almacenadas
Dentro de esta carpeta se deben incluir:

* **Pruebas de Contrato y Esquema (JSON Schema Validation):** Verificación de que los endpoints retornen los tipos de datos, llaves y estructuras requeridas.
* **Pruebas de Códigos de Estado (HTTP Status Codes):** Comprobación de respuestas correctas (`200 OK`, `201 Created`) y manejo de errores esperados (`400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`).
* **Pruebas Funcionales de Integración de Servicios:** Encadamiento de peticiones (ejemplo: autenticarse -> obtener token -> crear recurso -> consultar recurso creado -> eliminar recurso).
* **Validación de Reglas de Negocio:** Verificación de restricciones de datos, campos requeridos y procesamiento lógico directo en los controladores.

