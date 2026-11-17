package com.requiem.api.controller;

import com.requiem.api.dto.session.*;
import com.requiem.api.dto.store.CreateStorePresetRequest;
import com.requiem.api.dto.store.StorePresetDetailDto;
import com.requiem.api.service.SessionService;
import com.requiem.api.service.StorePresetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final StorePresetService storePresetService;

    // GET /sessions → { sessions: [...] }
    @GetMapping
    public ResponseEntity<Map<String, List<SessionSummaryDto>>> getSessions(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(Map.of("sessions", sessionService.getSessions(userId)));
    }

    // POST /sessions → 201 { session: {...} }
    @PostMapping
    public ResponseEntity<Map<String, SessionDetailDto>> createSession(
            @AuthenticationPrincipal String userId,
            @RequestBody(required = false) CreateSessionRequest req) {
        if (req == null) req = new CreateSessionRequest(null);
        SessionDetailDto session = sessionService.createSession(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("session", session));
    }

    // GET /sessions/:id → { session: {...} }
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, SessionDetailDto>> getSession(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(Map.of("session", sessionService.getSessionById(id, userId)));
    }

    // PATCH /sessions/:id → { success: true }
    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> patchSession(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @RequestBody PatchSessionRequest req) {
        sessionService.patchSession(id, userId, req);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // DELETE /sessions/:id → { success: true }
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteSession(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        sessionService.deleteSession(id, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // POST /sessions/:id/messages → 201 { message: {...} }
    @PostMapping("/{id}/messages")
    public ResponseEntity<Map<String, MessageDto>> addMessage(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody AddMessageRequest req) {
        MessageDto message = sessionService.addMessage(id, userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", message));
    }

    // GET /sessions/:id/export → 200 { export: {...} }
    @GetMapping("/{id}/export")
    public ResponseEntity<Map<String, ExportSessionResponse>> exportSession(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        ExportSessionResponse export = sessionService.exportSession(id, userId);
        return ResponseEntity.ok(Map.of("export", export));
    }

    // POST /sessions/:id/export-to-store → 201 { preset: {...} }
    @PostMapping("/{id}/export-to-store")
    public ResponseEntity<Map<String, StorePresetDetailDto>> exportSessionToStore(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @RequestBody(required = false) ExportToStoreRequest req) {
        CreateStorePresetRequest createReq = new CreateStorePresetRequest(
                req != null ? req.title() : null,
                req != null ? req.description() : null,
                req != null ? req.genre() : null,
                req != null ? req.tags() : null,
                req != null ? req.bpm() : null,
                req != null ? req.tonality() : null,
                req != null ? req.timeSignature() : null,
                null,
                id
        );
        StorePresetDetailDto preset = storePresetService.createPreset(userId, createReq);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("preset", preset));
    }
}
