package com.requiem.api.dto.session;

import jakarta.validation.constraints.NotBlank;

public record AddMessageRequest(
        @NotBlank String role,
        @NotBlank String content
) {}
