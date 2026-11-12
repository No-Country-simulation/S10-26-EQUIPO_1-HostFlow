# 🏡 HostFlow 

## Contexto
El Delta del Tigre constituye un destino turístico en el que los propietarios de pequeños complejos de cabañas y hoteles necesitan gestionar la información de sus huéspedes y coordinar servicios complementarios. Sin embargo, muchos de estos establecimientos realizan el registro de sus clientes mediante herramientas de mensajería como WhatsApp, lo que dificulta la centralización de los datos y la organización de las solicitudes.

Por otra parte, los turistas pueden encontrar dificultades para conseguir transporte privado de calidad hasta sus alojamientos, especialmente servicios premium de lancha remis. Esta situación representa una oportunidad para conectar las necesidades de los huéspedes con los servicios disponibles en el destino.

## Propuesta de Valor
HostFlow propone una aplicación web **B2B2C** que digitaliza el proceso de incorporación de huéspedes y ofrece herramientas básicas de gestión a los propietarios. 

### Funcionalidades Principales

* **Panel Administrativo:** Cada propietario/dueño puede registrar su complejo, personalizar su identidad visual, gestionar una lista simplificada de reservas y configurar servicios adicionales con sus respectivos precios.
* **Ficha de Onboarding Personalizada:** Accesible mediante una ruta específica para cada alojamiento y reserva, los turistas pueden:
  * Completar sus datos personales.
  * Indicar alergias y preferencias.
  * Seleccionar servicios adicionales.
  * Solicitar una cotización de transporte en lancha remis.
* **Gestión de Pedidos y Pagos:** Cuando el huésped selecciona servicios adicionales, la plataforma genera un resumen del pedido y muestra los datos bancarios proporcionados por el alojamiento para realizar el pago mediante transferencia externa. El propietario recibe una notificación por correo electrónico con los detalles del pedido (*la plataforma no procesa pagos electrónicos*).
* **Cotizaciones e Integración:** Cuando el turista solicita una cotización de lancha remis, el sistema registra la solicitud y prepara la información necesaria para su futura integración con un servicio externo mediante Webhook o API.

## Arquitectura del Proyecto

HostFlow se estructura en dos interfaces principales:

* **Frontend B2B:** Panel administrativo destinado a los propietarios para gestionar sus alojamientos, reservas y servicios adicionales.
* **Frontend B2C:** Formulario de onboarding personalizado para que los huéspedes registren sus datos y seleccionen los servicios disponibles.

Ambas interfaces se comunican con un backend mediante una API REST, que centraliza la lógica de negocio y la gestión de datos.

## Stack Tecnológico

| Tecnología | Descripción |
|---|---|
| React | Desarrollo de interfaces de usuario. |
| Tailwind CSS | Diseño y estilos de la aplicación. |
| Spring Boot | Desarrollo del backend y servicios REST. |
| PostgreSQL | Gestión y persistencia de datos. |
| Swagger UI | Documentación y exploración de la API. |
| Postman | Pruebas y validación de endpoints. |
| Cypress | Automatización de pruebas end-to-end. |

## Calidad y Testing

La validación de la aplicación contempla pruebas de los servicios del backend y de los principales flujos de usuario.

* **Postman:** Validación de endpoints, respuestas HTTP y comportamiento de los servicios.
* **Swagger UI:** Consulta y exploración de la documentación de la API.
* **Cypress:** Automatización de pruebas end-to-end para verificar los flujos principales de la aplicación.

## Alcance del MVP

El producto contempla las siguientes consideraciones:

* **Mobile First:** La interfaz prioriza la experiencia desde dispositivos móviles.
* **Gestión simplificada de reservas:** Se utiliza un listado cronológico con fechas de Check-in, Check-out y estado de cada reserva, sin incorporar un motor de reservas complejo.
* **White Label:** La personalización de cada alojamiento se realiza mediante rutas dinámicas, logotipo y colores propios.
* **Pagos externos:** No se integra una pasarela de pagos. Las transferencias bancarias se realizan fuera de la plataforma.
* **Solicitud de transporte:** El sistema registra las solicitudes de cotización de lancha remis y prepara los datos necesarios para su integración mediante Webhook/API.

## Equipo

Este proyecto se ha llevado a cabo mediante trabajo colaborativo implementando la metodología ágil Scrum en el entorno de No Country (S10-26-Equipo-1).

---

> **Nota del Desarrollo:** El producto se ha desarrollado como un **MVP** con prioridad en la experiencia móvil, la facilidad de uso y la centralización de información, limitando la complejidad técnica para cumplir con un plazo máximo de tres semanas y media.
