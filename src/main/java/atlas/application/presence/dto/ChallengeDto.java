package atlas.application.presence.dto;

import java.time.Instant;

public record ChallengeDto(String challengeId, String type, String nonce, Instant expiresAt) {}
