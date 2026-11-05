package com.requiem.api.dto.store;

import com.requiem.api.dto.session.SessionDetailDto;

public record UsePresetResponse(
        StorePresetDetailDto preset,
        SessionDetailDto createdSession
) {
}
