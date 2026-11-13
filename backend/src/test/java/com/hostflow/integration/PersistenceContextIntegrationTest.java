package com.hostflow.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Comprueba que el contexto de Spring inicia con el esquema de Hibernate del perfil test.
 * No valida Flyway: application-test.properties desactiva las migraciones.
 */
@SpringBootTest
@ActiveProfiles("test")
class PersistenceContextIntegrationTest {

    @Test
    void contextLoadsWithHibernateManagedTestSchema() {
        assertDoesNotThrow(() -> {
            // La anotación @SpringBootTest ya valida el arranque del contexto.
        });
    }
}
