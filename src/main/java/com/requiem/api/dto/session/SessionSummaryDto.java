package com.requiem.api.dto.session;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

/**
 * Session summary returned in GET /sessions list.
 * Includes only the last message (not all messages).
 */
public record SessionSummaryDto(
        String id,
        String title,
        String referenceMidiId,
        Instant createdAt,
        Instant updatedAt,
        JsonNode compositionData,
        List<MessageDto> messages   // only the last message
) {
    public SessionSummaryDto(
            String id,
            String title,
            Instant createdAt,
            Instant updatedAt,
            JsonNode compositionData,
            List<MessageDto> messages) {
        this(id, title, null, createdAt, updatedAt, compositionData, messages);
    }
}
