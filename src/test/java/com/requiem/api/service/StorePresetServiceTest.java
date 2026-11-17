package com.requiem.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.requiem.api.dto.session.CreateSessionRequest;
import com.requiem.api.dto.session.PatchSessionRequest;
import com.requiem.api.dto.session.SessionDetailDto;
import com.requiem.api.dto.store.*;
import com.requiem.api.entity.ChatSession;
import com.requiem.api.entity.StorePreset;
import com.requiem.api.entity.User;
import com.requiem.api.repository.ChatSessionRepository;
import com.requiem.api.repository.StorePresetRepository;
import com.requiem.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StorePresetService")
class StorePresetServiceTest {

    @Mock
    StorePresetRepository storePresetRepository;
    @Mock
    ChatSessionRepository chatSessionRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    SessionService sessionService;

    @InjectMocks
    StorePresetService storePresetService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User testUser;
    private StorePreset testPreset;
    private ArrayNode dummyConfig;

    @BeforeEach
    void setUp() {
        dummyConfig = objectMapper.createArrayNode();
        dummyConfig.addObject().put("id", "blk-1").put("name", "Intro");

        testUser = User.builder()
                .id("user-1")
                .name("Mozart")
                .email("mozart@salzburg.com")
                .build();

        testPreset = StorePreset.builder()
                .id("preset-1")
                .title("Bossa Nova Chic")
                .description("Acompanhamento suave de bossa")
                .genre("Bossa Nova")
                .tags("bossa, smooth, guitar")
                .bpm(120)
                .tonality("C")
                .timeSignature("4/4")
                .configurationData(dummyConfig)
                .downloadsCount(5)
                .likesCount(2)
                .user(testUser)
                .authorName(testUser.getName())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getPresets: sorts by recent by default")
    void getPresets_recent() {
        when(storePresetRepository.searchByRecent("bossa", null)).thenReturn(List.of(testPreset));

        List<StorePresetSummaryDto> result = storePresetService.getPresets("bossa", null, "recent");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Bossa Nova Chic");
        verify(storePresetRepository).searchByRecent("bossa", null);
        verify(storePresetRepository, never()).searchByPopular(any(), any());
    }

    @Test
    @DisplayName("getPresets: sorts by popular when requested")
    void getPresets_popular() {
        when(storePresetRepository.searchByPopular(null, "Jazz")).thenReturn(List.of(testPreset));

        List<StorePresetSummaryDto> result = storePresetService.getPresets(null, "Jazz", "popular");

        assertThat(result).hasSize(1);
        verify(storePresetRepository).searchByPopular(null, "Jazz");
    }

    @Test
    @DisplayName("getPresetById: returns preset when found")
    void getPresetById_found() {
        when(storePresetRepository.findById("preset-1")).thenReturn(Optional.of(testPreset));

        StorePresetDetailDto dto = storePresetService.getPresetById("preset-1");

        assertThat(dto.id()).isEqualTo("preset-1");
        assertThat(dto.authorName()).isEqualTo("Mozart");
        assertThat(dto.configurationData()).isEqualTo(dummyConfig);
    }

    @Test
    @DisplayName("getPresetById: throws 404 when not found")
    void getPresetById_notFound() {
        when(storePresetRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storePresetService.getPresetById("non-existent"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Configuração não encontrada na loja");
    }

    @Test
    @DisplayName("createPreset: directly with configurationData")
    void createPreset_direct() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(storePresetRepository.save(any(StorePreset.class))).thenAnswer(i -> {
            StorePreset sp = i.getArgument(0);
            sp.setId("new-id");
            return sp;
        });

        CreateStorePresetRequest req = new CreateStorePresetRequest(
                "Jazz Samba",
                "Descrição legal",
                "Jazz",
                "jazz, samba",
                130,
                "Dm",
                "4/4",
                dummyConfig,
                null
        );

        StorePresetDetailDto result = storePresetService.createPreset("user-1", req);

        assertThat(result.title()).isEqualTo("Jazz Samba");
        assertThat(result.authorName()).isEqualTo("Mozart");
        assertThat(result.configurationData()).isEqualTo(dummyConfig);
        verify(storePresetRepository).save(any(StorePreset.class));
    }

    @Test
    @DisplayName("createPreset: exports from existing session")
    void createPreset_fromSession() {
        ChatSession session = ChatSession.builder()
                .id("sess-1")
                .title("Minha Bossa")
                .compositionData(dummyConfig)
                .user(testUser)
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(chatSessionRepository.findByIdAndUserId("sess-1", "user-1")).thenReturn(Optional.of(session));
        when(storePresetRepository.save(any(StorePreset.class))).thenAnswer(i -> {
            StorePreset sp = i.getArgument(0);
            sp.setId("from-sess-id");
            return sp;
        });

        CreateStorePresetRequest req = new CreateStorePresetRequest(
                null, // title will default to session title
                "Publicado direto da sessão",
                "Bossa",
                "bossa",
                null,
                null,
                null,
                null, // config will default to session compositionData
                "sess-1"
        );

        StorePresetDetailDto result = storePresetService.createPreset("user-1", req);

        assertThat(result.title()).isEqualTo("Minha Bossa");
        assertThat(result.configurationData()).isEqualTo(dummyConfig);
    }

    @Test
    @DisplayName("createPreset: throws 400 when configurationData is missing")
    void createPreset_missingConfig() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));

        CreateStorePresetRequest req = new CreateStorePresetRequest(
                "Sem config",
                null, null, null, null, null, null,
                null,
                null
        );

        assertThatThrownBy(() -> storePresetService.createPreset("user-1", req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("configuração serializada do auto acompanhamento é obrigatória");
    }

    @Test
    @DisplayName("updatePreset: updates allowed fields when user is owner")
    void updatePreset_owner() {
        when(storePresetRepository.findByIdAndUserId("preset-1", "user-1")).thenReturn(Optional.of(testPreset));
        when(storePresetRepository.save(any(StorePreset.class))).thenAnswer(i -> i.getArgument(0));

        UpdateStorePresetRequest req = new UpdateStorePresetRequest(
                "Novo Título",
                "Nova Descrição",
                "Pop",
                "pop, modern",
                128,
                "G",
                "4/4",
                null
        );

        StorePresetDetailDto result = storePresetService.updatePreset("preset-1", "user-1", req);

        assertThat(result.title()).isEqualTo("Novo Título");
        assertThat(result.description()).isEqualTo("Nova Descrição");
        assertThat(result.genre()).isEqualTo("Pop");
        assertThat(result.bpm()).isEqualTo(128);
    }

    @Test
    @DisplayName("deletePreset: deletes preset when user is owner")
    void deletePreset_owner() {
        when(storePresetRepository.findByIdAndUserId("preset-1", "user-1")).thenReturn(Optional.of(testPreset));

        storePresetService.deletePreset("preset-1", "user-1");

        verify(storePresetRepository).delete(testPreset);
    }

    @Test
    @DisplayName("usePreset: increments downloads count and returns details")
    void usePreset_noSessionCreation() {
        when(storePresetRepository.findById("preset-1")).thenReturn(Optional.of(testPreset));
        when(storePresetRepository.save(any(StorePreset.class))).thenAnswer(i -> i.getArgument(0));

        UsePresetResponse response = storePresetService.usePreset("preset-1", null, false);

        assertThat(response.preset().downloadsCount()).isEqualTo(6);
        assertThat(response.createdSession()).isNull();
        verify(sessionService, never()).createSession(any(), any());
    }

    @Test
    @DisplayName("usePreset: increments downloads and creates session when requested by authenticated user")
    void usePreset_withSessionCreation() {
        when(storePresetRepository.findById("preset-1")).thenReturn(Optional.of(testPreset));
        when(storePresetRepository.save(any(StorePreset.class))).thenAnswer(i -> i.getArgument(0));

        SessionDetailDto mockCreatedSession = new SessionDetailDto(
                "new-session-id",
                "Bossa Nova Chic",
                null,
                Instant.now(),
                Instant.now(),
                dummyConfig,
                List.of()
        );

        when(sessionService.createSession(eq("user-1"), any(CreateSessionRequest.class)))
                .thenReturn(mockCreatedSession);
        when(sessionService.getSessionById("new-session-id", "user-1"))
                .thenReturn(mockCreatedSession);

        UsePresetResponse response = storePresetService.usePreset("preset-1", "user-1", true);

        assertThat(response.preset().downloadsCount()).isEqualTo(6);
        assertThat(response.createdSession()).isNotNull();
        assertThat(response.createdSession().id()).isEqualTo("new-session-id");
        verify(sessionService).patchSession(eq("new-session-id"), eq("user-1"), any(PatchSessionRequest.class));
    }

    @Test
    @DisplayName("usePreset: throws 401 when createSession requested without authenticated user")
    void usePreset_createSession_unauthenticated_throws401() {
        when(storePresetRepository.findById("preset-1")).thenReturn(Optional.of(testPreset));

        assertThatThrownBy(() -> storePresetService.usePreset("preset-1", null, true))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED));
    }
}
