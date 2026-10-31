package com.hostflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal de la aplicación HostFlow.
 *
 * <p>Este punto de entrada inicializa el contexto de Spring Boot
 * y levanta todos los beans configurados en la aplicación.</p>
 *
 * <p><strong>🆕 {@code @EnableScheduling}:</strong> habilita la ejecución
 * de tareas programadas ({@code @Scheduled}), necesarias para el refresco
 * automático de tasas de cambio desde APIs externas cada 1 hora.</p>
 *
 * <p>Para ejecutar en diferentes entornos:</p>
 * <ul>
 *   <li>Desarrollo: {@code mvn spring-boot:run -Dspring-boot.run.profiles=dev}</li>
 *   <li>Producción: {@code mvn spring-boot:run -Dspring-boot.run.profiles=prod}</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 2.0.0
 */
@SpringBootApplication
@EnableScheduling  //  Habilita tareas programadas (refresco de tasas de cambio)
public class HostflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(HostflowApplication.class, args);
    }
}