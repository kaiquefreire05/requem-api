package com.requiem.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.requiem.api.config.CorsConfig;
import com.requiem.api.config.SecurityConfig;
import com.requiem.api.dto.auth.AuthResponse;
import com.requiem.api.security.JwtAuthFilter;
import com.requiem.api.security.JwtService;
import com.requiem.api.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, JwtService.class, CorsConfig.class })
@ActiveProfiles("test")
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AuthService authService;

    private static final String USER_ID = "user-123";
    private static final AuthResponse AUTH_RESPONSE = new AuthResponse(
            "jwt-token",
            new AuthResponse.UserDto(USER_ID, "test@example.com", "Test User"));

    // ── POST /auth/register ──────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/register → 201 with token and user")
    void register_201() throws Exception {
        when(authService.register(any())).thenReturn(AUTH_RESPONSE);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", "test@example.com", "name", "Test User", "password", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.id").value(USER_ID));
    }

    @Test
    @DisplayName("POST /auth/register → 400 when email missing")
    void register_400_missingEmail() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("name", "Test User", "password", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /auth/register → 400 when password < 6 chars")
    void register_400_shortPassword() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", "test@example.com", "name", "Test", "password", "abc"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /auth/register → 409 when email already taken")
    void register_409_duplicate() throws Exception {
        when(authService.register(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado"));

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", "taken@example.com", "name", "User", "password", "password123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email já cadastrado"));
    }

    // ── POST /auth/login ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/login → 200 with token and user")
    void login_200() throws Exception {
        when(authService.login(any())).thenReturn(AUTH_RESPONSE);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", "test@example.com", "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /auth/login → 401 with wrong credentials")
    void login_401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", "test@example.com", "password", "wrongpass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais inválidas"));
    }

    @Test
    @DisplayName("POST /auth/login → 400 when fields are blank")
    void login_400_blankFields() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "", "password", ""))))
                .andExpect(status().isBadRequest());
    }

    // ── GET /auth/me ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /auth/me → 200 with user details")
    void me_200() throws Exception {
        var userDetail = new AuthResponse.UserDetailDto(USER_ID, "test@example.com", "Test User", Instant.now());
        when(authService.me(USER_ID)).thenReturn(userDetail);

        mockMvc.perform(get("/auth/me")
                .with(authentication(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(USER_ID))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.createdAt").exists());
    }

    @Test
    @DisplayName("GET /auth/me → 401 without token")
    void me_401_unauthenticated() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
