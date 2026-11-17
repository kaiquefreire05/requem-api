package com.requiem.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.requiem.api.dto.session.CreateSessionRequest;
import com.requiem.api.dto.session.SessionDetailDto;
import com.requiem.api.dto.store.*;
import com.requiem.api.entity.ChatSession;
import com.requiem.api.entity.StorePreset;
import com.requiem.api.entity.User;
import com.requiem.api.repository.ChatSessionRepository;
import com.requiem.api.repository.StorePresetRepository;
import com.requiem.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorePresetService {

    private final StorePresetRepository storePresetRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;

    @Transactional(readOnly = true)
    public List<StorePresetSummaryDto> getPresets(String search, String genre, String sortBy) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanGenre = (genre != null && !genre.isBlank()) ? genre.trim() : null;

        List<StorePreset> presets;
        if ("popular".equalsIgnoreCase(sortBy)) {
            presets = storePresetRepository.searchByPopular(cleanSearch, cleanGenre);
        } else {
            presets = storePresetRepository.searchByRecent(cleanSearch, cleanGenre);
        }

        return presets.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StorePresetDetailDto getPresetById(String id) {
        StorePreset preset = storePresetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuração não encontrada na loja"));
        return toDetailDto(preset);
    }

    @Transactional(readOnly = true)
    public List<StorePresetSummaryDto> getMyPresets(String userId) {
        return storePresetRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public StorePresetDetailDto createPreset(String userId, CreateStorePresetRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        String title = req.title();
        JsonNode configurationData = req.configurationData();

        if (req.sessionId() != null && !req.sessionId().isBlank()) {
            ChatSession session = chatSessionRepository.findByIdAndUserId(req.sessionId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sessão não encontrada para exportar"));

            if (title == null || title.isBlank()) {
                title = session.getTitle();
            }
            if (configurationData == null) {
                configurationData = session.getCompositionData();
            }
        }

        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O título da configuração é obrigatório");
        }

        if (configurationData == null || configurationData.isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A configuração serializada do auto acompanhamento é obrigatória");
        }

        StorePreset preset = StorePreset.builder()
                .title(title.trim())
                .description(req.description())
                .genre(req.genre())
                .tags(req.tags())
                .bpm(req.bpm())
                .tonality(req.tonality())
                .timeSignature(req.timeSignature())
                .configurationData(configurationData)
                .downloadsCount(0)
                .likesCount(0)
                .user(user)
                .authorName(user.getName())
                .build();

        preset = storePresetRepository.save(preset);
        return toDetailDto(preset);
    }

    @Transactional
    public StorePresetDetailDto updatePreset(String id, String userId, UpdateStorePresetRequest req) {
        StorePreset preset = storePresetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuração não encontrada ou não pertence ao usuário"));

        if (req.title() != null && !req.title().isBlank()) {
            preset.setTitle(req.title().trim());
        }
        if (req.description() != null) {
            preset.setDescription(req.description());
        }
        if (req.genre() != null) {
            preset.setGenre(req.genre());
        }
        if (req.tags() != null) {
            preset.setTags(req.tags());
        }
        if (req.bpm() != null) {
            preset.setBpm(req.bpm());
        }
        if (req.tonality() != null) {
            preset.setTonality(req.tonality());
        }
        if (req.timeSignature() != null) {
            preset.setTimeSignature(req.timeSignature());
        }
        if (req.configurationData() != null && !req.configurationData().isNull()) {
            preset.setConfigurationData(req.configurationData());
        }

        preset = storePresetRepository.save(preset);
        return toDetailDto(preset);
    }

    @Transactional
    public void deletePreset(String id, String userId) {
        StorePreset preset = storePresetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuração não encontrada ou não pertence ao usuário"));
        storePresetRepository.delete(preset);
    }

    @Transactional
    public UsePresetResponse usePreset(String id, String optionalUserId, boolean createSession) {
        StorePreset preset = storePresetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuração não encontrada na loja"));

        if (createSession && (optionalUserId == null || optionalUserId.isBlank())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "É necessário estar autenticado para criar uma sessão a partir da configuração");
        }

        preset.setDownloadsCount(preset.getDownloadsCount() + 1);
        preset = storePresetRepository.save(preset);

        StorePresetDetailDto detailDto = toDetailDto(preset);
        SessionDetailDto sessionDetailDto = null;

        if (createSession) {
            CreateSessionRequest sessionReq = new CreateSessionRequest(preset.getTitle(), null);
            sessionDetailDto = sessionService.createSession(optionalUserId, sessionReq);
            sessionService.patchSession(
                    sessionDetailDto.id(),
                    optionalUserId,
                    new com.requiem.api.dto.session.PatchSessionRequest(null, preset.getConfigurationData())
            );
            sessionDetailDto = sessionService.getSessionById(sessionDetailDto.id(), optionalUserId);
        }

        return new UsePresetResponse(detailDto, sessionDetailDto);
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    public StorePresetSummaryDto toSummaryDto(StorePreset p) {
        return new StorePresetSummaryDto(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getGenre(),
                p.getTags(),
                p.getBpm(),
                p.getTonality(),
                p.getTimeSignature(),
                p.getDownloadsCount(),
                p.getLikesCount(),
                p.getUser().getId(),
                p.getAuthorName(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    public StorePresetDetailDto toDetailDto(StorePreset p) {
        return new StorePresetDetailDto(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getGenre(),
                p.getTags(),
                p.getBpm(),
                p.getTonality(),
                p.getTimeSignature(),
                p.getConfigurationData(),
                p.getDownloadsCount(),
                p.getLikesCount(),
                p.getUser().getId(),
                p.getAuthorName(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
