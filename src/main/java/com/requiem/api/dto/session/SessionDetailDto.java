package com.requiem.api.dto.session;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

/**
 * Full session detail returned in GET /sessions/:id.
 * Includes all messages ordered by createdAt ASC.
 */
public record SessionDetailDto(
        String id,
        String title,
        String referenceMidiId,
        Instant createdAt,
        Instant updatedAt,
        JsonNode compositionData,
        List<MessageDto> messages
) {
    public SessionDetailDto(
            String id,
            String title,
            Instant createdAt,
            Instant updatedAt,
            JsonNode compositionData,
            List<MessageDto> messages) {
        this(id, title, null, createdAt, updatedAt, compositionData, messages);
    }
}
