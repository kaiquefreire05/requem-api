package com.requiem.api.controller;

import com.requiem.api.dto.store.*;
import com.requiem.api.service.StorePresetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store/presets")
@RequiredArgsConstructor
public class StorePresetController {

    private final StorePresetService storePresetService;

    // GET /store/presets?search=jazz&genre=jazz&sortBy=popular
    @GetMapping
    public ResponseEntity<Map<String, List<StorePresetSummaryDto>>> getPresets(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false, defaultValue = "recent") String sortBy) {
        List<StorePresetSummaryDto> presets = storePresetService.getPresets(search, genre, sortBy);
        return ResponseEntity.ok(Map.of("presets", presets));
    }

    // GET /store/presets/my (precisa vir antes de /{id} para não colidir)
    @GetMapping("/my")
    public ResponseEntity<Map<String, List<StorePresetSummaryDto>>> getMyPresets(
            @AuthenticationPrincipal String userId) {
        List<StorePresetSummaryDto> presets = storePresetService.getMyPresets(userId);
        return ResponseEntity.ok(Map.of("presets", presets));
    }

    // GET /store/presets/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, StorePresetDetailDto>> getPreset(@PathVariable String id) {
        StorePresetDetailDto preset = storePresetService.getPresetById(id);
        return ResponseEntity.ok(Map.of("preset", preset));
    }

    // POST /store/presets
    @PostMapping
    public ResponseEntity<Map<String, StorePresetDetailDto>> createPreset(
            @AuthenticationPrincipal String userId,
            @RequestBody CreateStorePresetRequest req) {
        StorePresetDetailDto preset = storePresetService.createPreset(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("preset", preset));
    }

    // PATCH /store/presets/{id}
    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, StorePresetDetailDto>> updatePreset(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @RequestBody UpdateStorePresetRequest req) {
        StorePresetDetailDto preset = storePresetService.updatePreset(id, userId, req);
        return ResponseEntity.ok(Map.of("preset", preset));
    }

    // DELETE /store/presets/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deletePreset(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        storePresetService.deletePreset(id, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // POST /store/presets/{id}/use?createSession=false
    @PostMapping("/{id}/use")
    public ResponseEntity<UsePresetResponse> usePreset(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false, defaultValue = "false") boolean createSession) {
        UsePresetResponse response = storePresetService.usePreset(id, userId, createSession);
        return ResponseEntity.ok(response);
    }
}
