package atlas.application.presence.ports;

import atlas.domain.presence.enums.VerificationOutcome;
import java.time.Instant;

public record AuthenticationAttempt(Instant occurredOn, VerificationOutcome outcome) {}
