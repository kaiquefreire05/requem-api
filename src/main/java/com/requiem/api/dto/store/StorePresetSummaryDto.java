package com.requiem.api.dto.store;

import java.time.Instant;

public record StorePresetSummaryDto(
        String id,
        String title,
        String description,
        String genre,
        String tags,
        Integer bpm,
        String tonality,
        String timeSignature,
        Integer downloadsCount,
        Integer likesCount,
        String authorId,
        String authorName,
        Instant createdAt,
        Instant updatedAt
) {
}
