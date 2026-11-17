package com.requiem.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.requiem.api.config.CorsConfig;
import com.requiem.api.config.SecurityConfig;
import com.requiem.api.dto.store.CreateStorePresetRequest;
import com.requiem.api.dto.store.StorePresetDetailDto;
import com.requiem.api.dto.store.StorePresetSummaryDto;
import com.requiem.api.dto.store.UsePresetResponse;
import com.requiem.api.security.JwtAuthFilter;
import com.requiem.api.security.JwtService;
import com.requiem.api.service.StorePresetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StorePresetController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, JwtService.class, CorsConfig.class })
@ActiveProfiles("test")
@DisplayName("StorePresetController")
class StorePresetControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    StorePresetService storePresetService;

    private static final String USER_ID = "user-123";
    private static final String PRESET_ID = "preset-abc";

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
    }

    private StorePresetSummaryDto sampleSummary() {
        return new StorePresetSummaryDto(
                PRESET_ID,
                "Jazz Samba",
                "Acompanhamento suingado",
                "Jazz",
                "jazz, samba",
                120,
                "C",
                "4/4",
                10,
                3,
                USER_ID,
                "Chico",
                Instant.now(),
                Instant.now()
        );
    }

    private StorePresetDetailDto sampleDetail() {
        ArrayNode config = objectMapper.createArrayNode();
        config.addObject().put("id", "blk-1");
        return new StorePresetDetailDto(
                PRESET_ID,
                "Jazz Samba",
                "Acompanhamento suingado",
                "Jazz",
                "jazz, samba",
                120,
                "C",
                "4/4",
                config,
                10,
                3,
                USER_ID,
                "Chico",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("GET /store/presets → 200 OK without auth (public access)")
    void getPresets_publicAccess() throws Exception {
        when(storePresetService.getPresets(any(), any(), any())).thenReturn(List.of(sampleSummary()));

        mockMvc.perform(get("/store/presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presets").isArray())
                .andExpect(jsonPath("$.presets[0].id").value(PRESET_ID))
                .andExpect(jsonPath("$.presets[0].title").value("Jazz Samba"));
    }

    @Test
    @DisplayName("GET /store/presets/{id} → 200 OK without auth (public access)")
    void getPresetById_publicAccess() throws Exception {
        when(storePresetService.getPresetById(PRESET_ID)).thenReturn(sampleDetail());

        mockMvc.perform(get("/store/presets/" + PRESET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preset.id").value(PRESET_ID))
                .andExpect(jsonPath("$.preset.configurationData").isArray());
    }

    @Test
    @DisplayName("POST /store/presets/{id}/use → 200 OK without auth")
    void usePreset_publicAccess() throws Exception {
        when(storePresetService.usePreset(eq(PRESET_ID), any(), eq(false)))
                .thenReturn(new UsePresetResponse(sampleDetail(), null));

        mockMvc.perform(post("/store/presets/" + PRESET_ID + "/use"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preset.id").value(PRESET_ID));
    }

    @Test
    @DisplayName("POST /store/presets → 401 when unauthenticated")
    void createPreset_unauthenticated() throws Exception {
        mockMvc.perform(post("/store/presets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Jazz\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /store/presets → 201 when authenticated")
    void createPreset_authenticated() throws Exception {
        when(storePresetService.createPreset(eq(USER_ID), any(CreateStorePresetRequest.class)))
                .thenReturn(sampleDetail());

        CreateStorePresetRequest req = new CreateStorePresetRequest(
                "Jazz Samba",
                "Desc",
                "Jazz",
                "tags",
                120,
                "C",
                "4/4",
                objectMapper.createArrayNode(),
                null
        );

        mockMvc.perform(post("/store/presets")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.preset.title").value("Jazz Samba"));
    }

    @Test
    @DisplayName("GET /store/presets/my → 401 when unauthenticated")
    void getMyPresets_unauthenticated() throws Exception {
        mockMvc.perform(get("/store/presets/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /store/presets/my → 200 when authenticated")
    void getMyPresets_authenticated() throws Exception {
        when(storePresetService.getMyPresets(USER_ID)).thenReturn(List.of(sampleSummary()));

        mockMvc.perform(get("/store/presets/my").with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presets[0].authorId").value(USER_ID));
    }

    @Test
    @DisplayName("DELETE /store/presets/{id} → 200 when authenticated")
    void deletePreset_authenticated() throws Exception {
        mockMvc.perform(delete("/store/presets/" + PRESET_ID).with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(storePresetService).deletePreset(PRESET_ID, USER_ID);
    }
}
