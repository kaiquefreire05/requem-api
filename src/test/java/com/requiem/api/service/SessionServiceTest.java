package com.requiem.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.requiem.api.dto.session.*;
import com.requiem.api.entity.ChatSession;
import com.requiem.api.entity.Message;
import com.requiem.api.entity.User;
import com.requiem.api.repository.ChatSessionRepository;
import com.requiem.api.repository.MessageRepository;
import com.requiem.api.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService")
class SessionServiceTest {

    @Mock ChatSessionRepository sessionRepository;
    @Mock MessageRepository messageRepository;
    @Mock UserRepository userRepository;

    @InjectMocks SessionService sessionService;

    // ── Fixtures ────────────────────────────────────────────────────────────

    private User buildUser(String id) {
        return User.builder().id(id).email("u@e.com").name("User").build();
    }

    private ChatSession buildSession(String id, String userId) {
        User user = buildUser(userId);
        return ChatSession.builder()
                .id(id)
                .title("My Session")
                .user(user)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Message buildMessage(String id, ChatSession session) {
        return Message.builder()
                .id(id)
                .role("user")
                .content("Hello")
                .session(session)
                .createdAt(Instant.now())
                .build();
    }

    // ── getSessions ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSessions: returns list with last message per session")
    void getSessions_success() {
        ChatSession s = buildSession("sess-1", "user-1");
        Message lastMsg = buildMessage("msg-1", s);

        when(sessionRepository.findByUserIdOrderByUpdatedAtDesc("user-1")).thenReturn(List.of(s));
        when(messageRepository.findFirstBySessionIdOrderByCreatedAtDesc("sess-1"))
                .thenReturn(Optional.of(lastMsg));

        List<SessionSummaryDto> result = sessionService.getSessions("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("sess-1");
        assertThat(result.get(0).messages()).hasSize(1);
        assertThat(result.get(0).messages().get(0).content()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("getSessions: session with no messages returns empty list")
    void getSessions_noMessages() {
        ChatSession s = buildSession("sess-1", "user-1");

        when(sessionRepository.findByUserIdOrderByUpdatedAtDesc("user-1")).thenReturn(List.of(s));
        when(messageRepository.findFirstBySessionIdOrderByCreatedAtDesc("sess-1"))
                .thenReturn(Optional.empty());

        List<SessionSummaryDto> result = sessionService.getSessions("user-1");

        assertThat(result.get(0).messages()).isEmpty();
    }

    // ── createSession ───────────────────────────────────────────────────────

    @Test
    @DisplayName("createSession: saves with provided title")
    void createSession_withTitle() {
        User userRef = buildUser("user-1");
        ChatSession saved = buildSession("sess-new", "user-1");
        saved.setTitle("My Title");

        when(userRepository.getReferenceById("user-1")).thenReturn(userRef);
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(saved);

        SessionDetailDto result = sessionService.createSession("user-1", new CreateSessionRequest("My Title"));

        assertThat(result.title()).isEqualTo("My Title");
        assertThat(result.messages()).isEmpty();
    }

    @Test
    @DisplayName("createSession: saves reference MIDI id when provided")
    void createSession_withReferenceMidiId() {
        User userRef = buildUser("user-1");
        ChatSession saved = buildSession("sess-new", "user-1");
        saved.setReferenceMidiId("cant-help-falling-in-love");

        when(userRepository.getReferenceById("user-1")).thenReturn(userRef);
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(saved);

        SessionDetailDto result = sessionService.createSession(
                "user-1",
                new CreateSessionRequest("Ensaio de Terça", "cant-help-falling-in-love"));

        verify(sessionRepository).save(argThat(s ->
                "cant-help-falling-in-love".equals(s.getReferenceMidiId())));
        assertThat(result.referenceMidiId()).isEqualTo("cant-help-falling-in-love");
    }

    @Test
    @DisplayName("createSession: null title uses default 'Nova Composição'")
    void createSession_defaultTitle() {
        User userRef = buildUser("user-1");
        ChatSession saved = buildSession("sess-new", "user-1");
        saved.setTitle("Nova Composição");

        when(userRepository.getReferenceById("user-1")).thenReturn(userRef);
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(saved);

        SessionDetailDto result = sessionService.createSession("user-1", new CreateSessionRequest(null));

        verify(sessionRepository).save(argThat(s -> "Nova Composição".equals(s.getTitle())));
        assertThat(result.title()).isEqualTo("Nova Composição");
    }

    // ── getSessionById ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getSessionById: returns session with all messages")
    void getSessionById_success() {
        ChatSession s = buildSession("sess-1", "user-1");
        Message m1 = buildMessage("msg-1", s);
        Message m2 = buildMessage("msg-2", s);

        when(sessionRepository.findByIdAndUserId("sess-1", "user-1")).thenReturn(Optional.of(s));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc("sess-1")).thenReturn(List.of(m1, m2));

        SessionDetailDto result = sessionService.getSessionById("sess-1", "user-1");

        assertThat(result.id()).isEqualTo("sess-1");
        assertThat(result.messages()).hasSize(2);
    }

    @Test
    @DisplayName("getSessionById: not found or wrong owner throws 404")
    void getSessionById_notFound() {
        when(sessionRepository.findByIdAndUserId("sess-x", "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getSessionById("sess-x", "user-1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── patchSession ────────────────────────────────────────────────────────

    @Test
    @DisplayName("patchSession: updates title when provided")
    void patchSession_titleOnly() {
        ChatSession s = buildSession("sess-1", "user-1");
        when(sessionRepository.findByIdAndUserId("sess-1", "user-1")).thenReturn(Optional.of(s));
        when(sessionRepository.save(any())).thenReturn(s);

        sessionService.patchSession("sess-1", "user-1", new PatchSessionRequest("New Title", null));

        assertThat(s.getTitle()).isEqualTo("New Title");
        verify(sessionRepository).save(s);
    }

    @Test
    @DisplayName("patchSession: updates compositionData when provided")
    void patchSession_compositionOnly() {
        ChatSession s = buildSession("sess-1", "user-1");
        JsonNode data = new ObjectMapper().createArrayNode()
                .add(new ObjectMapper().createObjectNode().put("id", "block-1").put("key", "Am"));

        when(sessionRepository.findByIdAndUserId("sess-1", "user-1")).thenReturn(Optional.of(s));
        when(sessionRepository.save(any())).thenReturn(s);

        sessionService.patchSession("sess-1", "user-1", new PatchSessionRequest(null, data));

        assertThat(s.getCompositionData()).isEqualTo(data);
        assertThat(s.getCompositionData().get(0).get("key").asText()).isEqualTo("Am");
    }

    @Test
    @DisplayName("patchSession: throws 404 if session not found")
    void patchSession_notFound() {
        when(sessionRepository.findByIdAndUserId("x", "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.patchSession("x", "user-1", new PatchSessionRequest(null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── deleteSession ───────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteSession: deletes when session exists")
    void deleteSession_exists() {
        ChatSession s = buildSession("sess-1", "user-1");
        when(sessionRepository.findByIdAndUserId("sess-1", "user-1")).thenReturn(Optional.of(s));

        sessionService.deleteSession("sess-1", "user-1");

        verify(sessionRepository).delete(s);
    }

    @Test
    @DisplayName("deleteSession: no-op (no 404) when session not found — mirrors deleteMany")
    void deleteSession_notFound_noException() {
        when(sessionRepository.findByIdAndUserId("ghost", "user-1")).thenReturn(Optional.empty());

        assertThatCode(() -> sessionService.deleteSession("ghost", "user-1"))
                .doesNotThrowAnyException();

        verify(sessionRepository, never()).delete(any());
    }

    // ── addMessage ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("addMessage: saves message and bumps session updatedAt")
    void addMessage_success() {
        ChatSession s = buildSession("sess-1", "user-1");
        Message saved = buildMessage("msg-new", s);
        saved.setRole("user");
        saved.setContent("Hello world");

        when(sessionRepository.findByIdAndUserId("sess-1", "user-1")).thenReturn(Optional.of(s));
        when(messageRepository.save(any(Message.class))).thenReturn(saved);
        when(sessionRepository.save(any())).thenReturn(s);

        MessageDto result = sessionService.addMessage("sess-1", "user-1",
                new AddMessageRequest("user", "Hello world"));

        assertThat(result.role()).isEqualTo("user");
        assertThat(result.content()).isEqualTo("Hello world");
        assertThat(result.sessionId()).isEqualTo("sess-1");

        verify(messageRepository).save(any(Message.class));
        verify(sessionRepository).save(s); // updatedAt bump
    }

    @Test
    @DisplayName("addMessage: session not found throws 404")
    void addMessage_sessionNotFound() {
        when(sessionRepository.findByIdAndUserId("x", "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.addMessage("x", "user-1",
                new AddMessageRequest("user", "Hi")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── exportSession ───────────────────────────────────────────────────────

    @Test
    @DisplayName("exportSession: exports session configuration data")
    void exportSession_success() {
        ChatSession s = buildSession("sess-1", "user-1");
        ObjectMapper mapper = new ObjectMapper();
        s.setCompositionData(mapper.createArrayNode());

        when(sessionRepository.findByIdAndUserId("sess-1", "user-1")).thenReturn(Optional.of(s));

        ExportSessionResponse result = sessionService.exportSession("sess-1", "user-1");

        assertThat(result.sessionId()).isEqualTo("sess-1");
        assertThat(result.title()).isEqualTo("My Session");
        assertThat(result.configurationData()).isNotNull();
    }

    @Test
    @DisplayName("exportSession: throws 404 when session not found")
    void exportSession_notFound() {
        when(sessionRepository.findByIdAndUserId("sess-x", "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.exportSession("sess-x", "user-1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
