package com.requiem.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.requiem.api.config.CorsConfig;
import com.requiem.api.config.SecurityConfig;
import com.requiem.api.dto.session.*;
import com.requiem.api.security.JwtAuthFilter;
import com.requiem.api.security.JwtService;
import com.requiem.api.service.SessionService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, JwtService.class, CorsConfig.class })
@ActiveProfiles("test")
@DisplayName("SessionController")
class SessionControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    SessionService sessionService;
    @MockBean
    com.requiem.api.service.StorePresetService storePresetService;

    private static final String USER_ID = "user-123";
    private static final String SESSION_ID = "sess-abc";

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
    }

    private SessionSummaryDto buildSummary() {
        return new SessionSummaryDto(SESSION_ID, "My Session", Instant.now(), Instant.now(), null, List.of());
    }

    private SessionDetailDto buildDetail() {
        return new SessionDetailDto(SESSION_ID, "My Session", Instant.now(), Instant.now(), null, List.of());
    }

    private MessageDto buildMessage() {
        return new MessageDto("msg-1", "user", "Hello", SESSION_ID, Instant.now());
    }

    // ── GET /sessions ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /sessions → 200 with sessions list")
    void getSessions_200() throws Exception {
        when(sessionService.getSessions(USER_ID)).thenReturn(List.of(buildSummary()));

        mockMvc.perform(get("/sessions").with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions").isArray())
                .andExpect(jsonPath("$.sessions[0].id").value(SESSION_ID));
    }

    @Test
    @DisplayName("GET /sessions → 401 without auth")
    void getSessions_401() throws Exception {
        mockMvc.perform(get("/sessions"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /sessions ───────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /sessions → 201 with session object")
    void createSession_201() throws Exception {
        when(sessionService.createSession(eq(USER_ID), any())).thenReturn(buildDetail());

        mockMvc.perform(post("/sessions")
                .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "My Session"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.session.id").value(SESSION_ID))
                .andExpect(jsonPath("$.session.messages").isArray());
    }

    @Test
    @DisplayName("POST /sessions → accepts reference MIDI id")
    void createSession_acceptsReferenceMidiId() throws Exception {
        SessionDetailDto session = new SessionDetailDto(
                SESSION_ID,
                "Ensaio de Terça",
                "cant-help-falling-in-love",
                Instant.now(),
                Instant.now(),
                null,
                List.of());
        when(sessionService.createSession(eq(USER_ID), any())).thenReturn(session);

        mockMvc.perform(post("/sessions")
                .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Ensaio de Terça","referenceMidiId":"cant-help-falling-in-love"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.session.referenceMidiId")
                        .value("cant-help-falling-in-love"));
    }

    @Test
    @DisplayName("POST /sessions → 201 with no body (default title)")
    void createSession_201_noBody() throws Exception {
        when(sessionService.createSession(eq(USER_ID), any())).thenReturn(buildDetail());

        mockMvc.perform(post("/sessions").with(authentication(auth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.session").exists());
    }

    // ── GET /sessions/:id ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /sessions/:id → 200 with session detail")
    void getSession_200() throws Exception {
        when(sessionService.getSessionById(SESSION_ID, USER_ID)).thenReturn(buildDetail());

        mockMvc.perform(get("/sessions/{id}", SESSION_ID).with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.id").value(SESSION_ID));
    }

    @Test
    @DisplayName("GET /sessions/:id → 404 when not found or wrong owner")
    void getSession_404() throws Exception {
        when(sessionService.getSessionById(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada"));

        mockMvc.perform(get("/sessions/{id}", "bad-id").with(authentication(auth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Sessão não encontrada"));
    }

    // ── PATCH /sessions/:id ──────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /sessions/:id → 200 { success: true }")
    void patchSession_200() throws Exception {
        doNothing().when(sessionService).patchSession(eq(SESSION_ID), eq(USER_ID), any());

        mockMvc.perform(patch("/sessions/{id}", SESSION_ID)
                .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "New Title"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PATCH /sessions/:id → 404 when session not found")
    void patchSession_404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada"))
                .when(sessionService).patchSession(any(), any(), any());

        mockMvc.perform(patch("/sessions/{id}", "bad-id")
                .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /sessions/:id ─────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /sessions/:id → 200 { success: true }")
    void deleteSession_200() throws Exception {
        doNothing().when(sessionService).deleteSession(SESSION_ID, USER_ID);

        mockMvc.perform(delete("/sessions/{id}", SESSION_ID).with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── POST /sessions/:id/messages ──────────────────────────────────────────

    @Test
    @DisplayName("POST /sessions/:id/messages → 201 with message")
    void addMessage_201() throws Exception {
        when(sessionService.addMessage(eq(SESSION_ID), eq(USER_ID), any())).thenReturn(buildMessage());

        mockMvc.perform(post("/sessions/{id}/messages", SESSION_ID)
                .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "user", "content", "Hello"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message.id").value("msg-1"))
                .andExpect(jsonPath("$.message.role").value("user"))
                .andExpect(jsonPath("$.message.content").value("Hello"))
                .andExpect(jsonPath("$.message.sessionId").value(SESSION_ID));
    }

    @Test
    @DisplayName("POST /sessions/:id/messages → 400 when role or content blank")
    void addMessage_400_validation() throws Exception {
        mockMvc.perform(post("/sessions/{id}/messages", SESSION_ID)
                .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "", "content", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /sessions/:id/messages → 404 when session not found")
    void addMessage_404() throws Exception {
        when(sessionService.addMessage(any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada"));

        mockMvc.perform(post("/sessions/{id}/messages", "bad-id")
                .with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "user", "content", "Hi"))))
                .andExpect(status().isNotFound());
    }

    // ── Export endpoints ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /sessions/:id/export → 200 with exported configuration")
    void exportSession_200() throws Exception {
        ExportSessionResponse export = new ExportSessionResponse(
                SESSION_ID,
                "Minha Composição",
                Instant.now(),
                objectMapper.createArrayNode()
        );
        when(sessionService.exportSession(SESSION_ID, USER_ID)).thenReturn(export);

        mockMvc.perform(get("/sessions/{id}/export", SESSION_ID).with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.export.sessionId").value(SESSION_ID))
                .andExpect(jsonPath("$.export.title").value("Minha Composição"));
    }

    @Test
    @DisplayName("POST /sessions/:id/export-to-store → 201 when exported to store")
    void exportSessionToStore_201() throws Exception {
        com.requiem.api.dto.store.StorePresetDetailDto mockPreset = new com.requiem.api.dto.store.StorePresetDetailDto(
                "preset-1",
                "Minha Composição",
                "Desc",
                "Jazz",
                "tags",
                120,
                "C",
                "4/4",
                objectMapper.createArrayNode(),
                0,
                0,
                USER_ID,
                "Mozart",
                Instant.now(),
                Instant.now()
        );
        when(storePresetService.createPreset(eq(USER_ID), any())).thenReturn(mockPreset);

        mockMvc.perform(post("/sessions/{id}/export-to-store", SESSION_ID)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Desc\",\"genre\":\"Jazz\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.preset.id").value("preset-1"))
                .andExpect(jsonPath("$.preset.title").value("Minha Composição"));
    }
}
