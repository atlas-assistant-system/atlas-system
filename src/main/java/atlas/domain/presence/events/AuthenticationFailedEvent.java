package atlas.domain.presence.events;

import atlas.domain.presence.enums.VerificationOutcome;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record AuthenticationFailedEvent(VerificationOutcome outcome, Instant occurredOn) implements DomainEvent {}
