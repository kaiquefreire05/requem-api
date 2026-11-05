package com.requiem.api.dto.session;

public record ExportToStoreRequest(
        String title,
        String description,
        String genre,
        String tags,
        Integer bpm,
        String tonality,
        String timeSignature
) {
}
