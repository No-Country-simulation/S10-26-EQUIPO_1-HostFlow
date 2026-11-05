package com.hostflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de Swagger / OpenAPI 3 para la documentación de la API de HostFlow.
 *
 * <p>Define la información general de la API, los esquemas de seguridad
 * (JWT Bearer Token), los metadatos de contacto y las URLs base de los servidores.</p>
 *
 * <p><strong>Característica Multi-Entorno:</strong></p>
 * <ul>
 *   <li>Incluye tanto el servidor local como el de producción (Render).</li>
 *   <li>Permite cambiar dinámicamente entre servidores desde el dropdown de Swagger UI.</li>
 *   <li>Evita conflictos de "Failed to fetch" al probar la API desplegada en Render.</li>
 * </ul>
 *
 * <p><strong>Ubicación de la documentación:</strong></p>
 * <ul>
 *   <li>Interfaz gráfica (Swagger UI): {@code /api/v1/swagger-ui/index.html}</li>
 *   <li>Definición OpenAPI (JSON): {@code /api/v1/v3/api-docs}</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.2.0 (Soporte multi-entorno: Local y Render sin conflictos)
 */
@Configuration
public class SwaggerConfig {

    // =========================================================================
    // CONFIGURACIÓN DE SERVIDORES (Inyectada desde application.properties)
    // =========================================================================

    @Value("${app.swagger.local-url:http://localhost:8080/api/v1}")
    private String localUrl;

    @Value("${app.swagger.render-url:https://hostflow-backend-mi26.onrender.com/api/v1}")
    private String renderUrl;

    /**
     * Configura el bean de OpenAPI con la información de la API,
     * las URLs de los servidores (Local y Render) y el esquema de seguridad JWT Bearer.
     *
     * @return la configuración completa de OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()

                // =========================================================================
                // 1. INFORMACIÓN GENERAL DE LA API
                // =========================================================================
                .info(new Info()
                        .title("HostFlow API")
                        .version("1.0.0")
                        .description("""
                                API REST para el sistema de gestión integral de alojamientos turísticos HostFlow.
                                
                                Permite el registro y gestión de propietarios, administración de alojamientos, 
                                reservas, huéspedes y procesamiento de pagos multi-moneda.
                                
                                ## 🔐 Autenticación
                                La API utiliza tokens JWT (JSON Web Token) para la autenticación.
                                1. Use el endpoint `/auth/login` para obtener su token.
                                2. Haga clic en el botón **"Authorize"** (candado) en la parte superior derecha.
                                3. Ingrese el token con el formato: `Bearer <tu_token_aquí>`
                                """)
                        .contact(new Contact()
                                .name("HostFlow Team")
                                .email("support@hostflow.com")
                                .url("https://hostflow.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0"))
                )

                // =========================================================================
                // 2. CONFIGURACIÓN DE SERVIDORES (MULTI-ENTORNO)
                // =========================================================================
                // Se definen explícitamente ambos servidores.
                // Swagger UI mostrará un dropdown en la parte superior para que el usuario
                // elija a qué entorno dirigir las peticiones, evitando el error "Failed to fetch".
                .servers(List.of(
                        new Server()
                                .url(localUrl)
                                .description("Servidor de Desarrollo Local"),
                        new Server()
                                .url(renderUrl)
                                .description("Servidor de Producción (Render)")
                ))

                // =========================================================================
                // 3. ESQUEMA DE SEGURIDAD (COMPONENTE REUTILIZABLE)
                // =========================================================================
                // Define el esquema "Bearer Authentication" como disponible.
                // Cada controlador debe declarar explícitamente:
                // @SecurityRequirement(name = "Bearer Authentication")
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese el token JWT. Ejemplo: Bearer eyJhbGciOiJIUzI1Ni...")
                        )
                );
    }
}