package com.requiem.api.dto.session;

import com.fasterxml.jackson.databind.JsonNode;

public record PatchSessionRequest(String title, JsonNode compositionData) {}
