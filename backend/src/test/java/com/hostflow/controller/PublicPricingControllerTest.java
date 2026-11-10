package com.hostflow.controller;

import com.hostflow.service.JwtService;
import com.hostflow.service.PublicPricingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicPricingController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicPricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicPricingService pricingService;

    // Necesarios para que el ApplicationContext pueda arrancar
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void getBasePrice_ShouldReturnOk_WhenAccommodationExists() throws Exception {
        mockMvc.perform(get("/public/accommodations/1/base-price"))
                .andExpect(status().isOk());
    }
}