package com.requiem.api.dto.session;

import java.time.Instant;

public record MessageDto(
        String id,
        String role,
        String content,
        String sessionId,
        Instant createdAt
) {}
