package atlas.application.presence.dto;

import java.time.Instant;

public record AuthenticationAttemptDto(Instant occurredOn, String outcome) {}
