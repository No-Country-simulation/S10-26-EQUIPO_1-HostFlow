package com.hostflow.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpoint_ShouldReturn403_WhenNoTokenProvided() throws Exception {
        // Sin token → anónimo → no tiene rol OWNER/ADMIN → 403
        mockMvc.perform(get("/owners/accommodation/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_ShouldReturn403_WhenNoTokenProvided() throws Exception {
        // Sin token → anónimo → no tiene rol ADMIN → 403
        mockMvc.perform(get("/admin/dashboard/metrics"))
                .andExpect(status().isForbidden());
    }
}