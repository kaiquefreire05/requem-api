package com.requiem.api.dto.store;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record StorePresetDetailDto(
        String id,
        String title,
        String description,
        String genre,
        String tags,
        Integer bpm,
        String tonality,
        String timeSignature,
        JsonNode configurationData,
        Integer downloadsCount,
        Integer likesCount,
        String authorId,
        String authorName,
        Instant createdAt,
        Instant updatedAt
) {
}
