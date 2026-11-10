package com.hostflow.integration;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Test
    void contextLoads_WithFlywayMigrations() {
        // Si el contexto de Spring carga exitosamente, significa que
        // las migraciones de Flyway (o la creación de esquema de Hibernate en test)
        // se ejecutaron sin errores de sintaxis o conflictos.
        assertDoesNotThrow(() -> {
            // No se necesita hacer nada, la anotación @SpringBootTest ya valida el arranque
        });
    }
}