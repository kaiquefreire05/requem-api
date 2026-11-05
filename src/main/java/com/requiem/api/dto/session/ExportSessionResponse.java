package com.requiem.api.dto.session;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record ExportSessionResponse(
        String sessionId,
        String title,
        Instant exportedAt,
        JsonNode configurationData
) {
}
