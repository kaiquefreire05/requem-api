package com.requiem.api.dto.auth;

import java.time.Instant;

public record AuthResponse(String token, UserDto user) {

    public record UserDto(String id, String email, String name) {}

    // Extended version including createdAt for GET /auth/me
    public record UserDetailDto(String id, String email, String name, Instant createdAt) {}
}
