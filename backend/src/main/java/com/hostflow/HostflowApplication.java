package com.hostflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;



/**
 * Clase principal de la aplicación HostFlow.
 *
 * <p>Este punto de entrada inicializa el contexto de Spring Boot
 * y levanta todos los beans configurados en la aplicación.</p>
 *
 * <p>Para ejecutar en diferentes entornos:</p>
 * <ul>
 *   <li>Desarrollo: {@code mvn spring-boot:run -Dspring-boot.run.profiles=dev}</li>
 *   <li>Producción: {@code mvn spring-boot:run -Dspring-boot.run.profiles=prod}</li>
 * </ul>
 *
 * @author HostFlow Team
 * @version 1.0.0
 */
@SpringBootApplication
public class HostflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(HostflowApplication.class, args);
   }

}