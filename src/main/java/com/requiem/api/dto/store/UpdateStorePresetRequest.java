package com.requiem.api.dto.store;

import com.fasterxml.jackson.databind.JsonNode;

public record UpdateStorePresetRequest(
        String title,
        String description,
        String genre,
        String tags,
        Integer bpm,
        String tonality,
        String timeSignature,
        JsonNode configurationData
) {
}
