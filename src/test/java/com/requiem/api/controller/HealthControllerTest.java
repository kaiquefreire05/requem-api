package com.requiem.api.controller;

import com.requiem.api.config.CorsConfig;
import com.requiem.api.config.SecurityConfig;
import com.requiem.api.security.JwtAuthFilter;
import com.requiem.api.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class, CorsConfig.class})
@ActiveProfiles("test")
@DisplayName("HealthController")
class HealthControllerTest {

    @Autowired MockMvc mockMvc;

    // JwtService is needed by JwtAuthFilter even though health is public
    @MockBean JwtService jwtService;

    @Test
    @DisplayName("GET /health → 200 with status ok and timestamp")
    void health_200() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /health → accessible without authentication")
    void health_publicEndpoint() throws Exception {
        // No auth header → should still return 200 (public endpoint)
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }
}
