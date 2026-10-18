package com.requiem.api.dto.session;

public record CreateSessionRequest(String title, String referenceMidiId) {
    public CreateSessionRequest(String title) {
        this(title, null);
    }
}
