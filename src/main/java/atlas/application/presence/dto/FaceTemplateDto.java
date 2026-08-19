package atlas.application.presence.dto;

import java.time.Instant;

public record FaceTemplateDto(String id, Instant capturedAt) {}
