package com.hostflow.controller;

import com.hostflow.service.PublicPricingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicPricingController.class)
class PublicPricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicPricingService pricingService;

    @Test
    void getBasePrice_ShouldReturnOk_WhenAccommodationExists() throws Exception {
        // Simplemente verificamos que el endpoint responda 200 OK y no 404/500
        // (La lógica del servicio ya está mockeada)
        mockMvc.perform(get("/public/accommodations/1/base-price"))
                .andExpect(status().isOk());
    }
}