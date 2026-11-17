package com.requiem.api.service;

import com.requiem.api.dto.session.*;
import com.requiem.api.entity.ChatSession;
import com.requiem.api.entity.Message;
import com.requiem.api.entity.User;
import com.requiem.api.repository.ChatSessionRepository;
import com.requiem.api.repository.MessageRepository;
import com.requiem.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final ChatSessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public List<SessionSummaryDto> getSessions(String userId) {
        List<ChatSession> sessions = sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);

        return sessions.stream().map(s -> {
            // Fetch only the last message
            List<MessageDto> lastMsg = messageRepository
                    .findFirstBySessionIdOrderByCreatedAtDesc(s.getId())
                    .map(m -> List.of(toMessageDto(m)))
                    .orElse(List.of());

            return new SessionSummaryDto(
                    s.getId(),
                    s.getTitle(),
                    s.getReferenceMidiId(),
                    s.getCreatedAt(),
                    s.getUpdatedAt(),
                    s.getCompositionData(),
                    lastMsg
            );
        }).collect(Collectors.toList());
    }

    @Transactional
    public SessionDetailDto createSession(String userId, CreateSessionRequest req) {
        User userRef = userRepository.getReferenceById(userId);

        String title = (req.title() != null && !req.title().isBlank())
                ? req.title()
                : "Nova Composição";

        ChatSession session = ChatSession.builder()
                .title(title)
                .referenceMidiId(req.referenceMidiId())
                .user(userRef)
                .build();

        session = sessionRepository.save(session);

        return new SessionDetailDto(
                session.getId(),
                session.getTitle(),
                session.getReferenceMidiId(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                null,
                List.of()
        );
    }

    public SessionDetailDto getSessionById(String sessionId, String userId) {
        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada"));

        List<MessageDto> messages = messageRepository
                .findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());

        return new SessionDetailDto(
                session.getId(),
                session.getTitle(),
                session.getReferenceMidiId(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                session.getCompositionData(),
                messages
        );
    }

    @Transactional
    public void patchSession(String sessionId, String userId, PatchSessionRequest req) {
        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada"));

        if (req.title() != null) {
            session.setTitle(req.title());
        }
        if (req.compositionData() != null) {
            session.setCompositionData(req.compositionData());
        }

        // Force updatedAt update — @PreUpdate will fire on save
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(String sessionId, String userId) {
        // Mirrors deleteMany behavior — no 404 if not found
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .ifPresent(sessionRepository::delete);
    }

    @Transactional
    public MessageDto addMessage(String sessionId, String userId, AddMessageRequest req) {
        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada"));

        Message message = Message.builder()
                .role(req.role())
                .content(req.content())
                .session(session)
                .build();

        message = messageRepository.save(message);

        // Update session updatedAt
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        return toMessageDto(message);
    }

    @Transactional(readOnly = true)
    public ExportSessionResponse exportSession(String sessionId, String userId) {
        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada"));

        return new ExportSessionResponse(
                session.getId(),
                session.getTitle(),
                Instant.now(),
                session.getCompositionData()
        );
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private MessageDto toMessageDto(Message m) {
        return new MessageDto(
                m.getId(),
                m.getRole(),
                m.getContent(),
                m.getSession().getId(),
                m.getCreatedAt()
        );
    }
}
