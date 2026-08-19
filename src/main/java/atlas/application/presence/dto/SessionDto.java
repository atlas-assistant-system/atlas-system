package atlas.application.presence.dto;

import java.time.Instant;

public record SessionDto(
    String id,
    String profileId,
    Instant openedAt,
    Instant lastActivityAt,
    Instant expiresAt,
    String status) {}
