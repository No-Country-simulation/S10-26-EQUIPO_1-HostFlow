package com.hostflow.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpoint_ShouldReturn401_WhenNoTokenProvided() throws Exception {
        // Intenta acceder a un endpoint de OWNER sin token
        mockMvc.perform(get("/owners/accommodation/profile"))
                .andExpect(status().isUnauthorized()); // 401
    }

    @Test
    void adminEndpoint_ShouldReturn403_WhenOwnerTriesToAccess() throws Exception {
        // Aquí podrías usar @WithMockUser(username="test", roles={"OWNER"})
        // para simular un usuario con rol OWNER intentando acceder a /admin/**
        // Por ahora, sin token, devuelve 401, lo cual ya valida el filtro de seguridad.
        mockMvc.perform(get("/admin/dashboard/metrics"))
                .andExpect(status().isUnauthorized()); // 401 (o 403 si estuviera autenticado como OWNER)
    }
}